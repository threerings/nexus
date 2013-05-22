//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.server;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import com.google.common.collect.Maps;

import react.RMap;
import react.RPromise;
import react.Slot;
import react.Try;

import com.threerings.nexus.distrib.Action;
import com.threerings.nexus.distrib.Address;
import com.threerings.nexus.distrib.DistribUtil;
import com.threerings.nexus.distrib.EntityNotFoundException;
import com.threerings.nexus.distrib.EventSink;
import com.threerings.nexus.distrib.Keyed;
import com.threerings.nexus.distrib.KeyedFactory;
import com.threerings.nexus.distrib.Nexus;
import com.threerings.nexus.distrib.NexusEvent;
import com.threerings.nexus.distrib.NexusException;
import com.threerings.nexus.distrib.NexusObject;
import com.threerings.nexus.distrib.Request;
import com.threerings.nexus.distrib.Singleton;
import static com.threerings.nexus.util.Log.log;

/**
 * Maintains metadata for all registered entities including their contexts. Contexts are action
 * queues that are guaranteed to be processed in a single-threaded manner.
 */
public class ObjectManager
    implements EventSink
{
    /** An interface implemented by sessions, which must forward object events to clients. */
    public interface Subscriber {
        /** Notifies the subscriber of an event which must be forwarded. */
        void forwardEvent (NexusEvent event);
        /** Notifies the subscriber that the specified object was cleared/removed. */
        void onCleared (int id);
    }

    public ObjectManager (NexusConfig config, Nexus nexus, Executor exec) {
        _config = config;
        _nexus = nexus;
        _exec = exec;
    }

    /**
     * Registers the supplied object in its own context.
     */
    public void register (NexusObject object) {
        register(object, new EntityContext(_exec));
    }

    /**
     * Registers the supplied object in the context of its parent.
     */
    public void register (NexusObject child, Singleton parent) {
        Binding<Singleton> pbind = requireSingleton(
            getSingletonClass(parent.getClass()),
            "Can't bind child to unregistered singleton parent");
        register(child, pbind.context);
    }

    /**
     * Registers the supplied object in the context of its parent.
     */
    public void register (NexusObject child, Keyed parent) {
        Binding<Keyed> pbind = requireKeyed(
            getKeyedClass(parent.getClass()), parent.getKey(),
            "Can't bind child to unregistered keyed parent");
        register(child, pbind.context);
    }

    /**
     * Registers the supplied singleton entity in its own context.
     */
    public void registerSingleton (Singleton entity) {
        register(entity, new EntityContext(_exec));
    }

    /**
     * Registers the supplied singleton in the context of its parent.
     */
    public void registerSingleton (Singleton child, Singleton parent) {
        Binding<Singleton> pbind = requireSingleton(
            getSingletonClass(parent.getClass()),
            "Can't bind child to unregistered singleton parent");
        register(child, pbind.context);
    }

    /**
     * Registers the supplied keyed entity in its own context.
     *
     * @throws NexusException if an entity is already mapped with the entity's key.
     */
    public void registerKeyed (Keyed entity) {
        register(entity, new EntityContext(_exec));
    }

    /**
     * Registers the supplied keyed entity in the context of its parent.
     */
    public void registerKeyed (Keyed child, Keyed parent) {
        Binding<Keyed> pbind = requireKeyed(
            getKeyedClass(parent.getClass()), parent.getKey(),
            "Can't bind child to unregistered keyed parent");
        register(child, pbind.context);
    }

    /**
     * Registers a factory for keyed entities of type {@code kclass}.
     */
    public <T extends Keyed> void registerKeyedFactory (Class<T> kclass, KeyedFactory<T> factory) {
        KeyedFactory<?> ofact = _kfacts.putIfAbsent(kclass, factory);
        if (ofact != null) {
            log.warning("Rejected request to overwrite keyed entity factory",
                        "kclass", kclass, "ofact", ofact, "nfact", factory);
        }
    }

    /**
     * Registers a global map and its associated context.
     */
    public <K,V> RMap<K,V> registerMap (String id) {
        GlobalMap<K,V> map = new GlobalMap<K,V>(id, this);
        // oddly, we have to tell Java that "forgetting" K and V here is safe, and we have to do
        // some serious backbending to accomplish it to boot; damned half-assed existentials
        Binding<?> bind = Binding.simple(map, new EntityContext(_exec));
        @SuppressWarnings("unchecked") Binding<GlobalMap<?,?>> casted =
            (Binding<GlobalMap<?,?>>)bind;
        if (_maps.putIfAbsent(id, casted) != null) {
            throw new NexusException("Map already registered for id: " + id);
        }
        return map;
    }

    /**
     * Returns true if we host the specified keyed entity, false if not.
     */
    public boolean hostsKeyed (Class<?> kclass, Comparable<?> key) {
        return getKeyedMap(kclass).containsKey(key);
    }

    /**
     * Returns an unused identifier for the specified keyed entity class.
     */
    public int nextId (int serverId, int maxServers, Class<?> kclass) {
        IdGen gen = _kidgens.get(kclass);
        if (gen == null) {
            // do the "add to concurrent map" shuffle
            IdGen collide = _kidgens.putIfAbsent(kclass, gen = new IdGen());
            if (collide != null) gen = collide;
        }

        ConcurrentMap<Comparable<?>,Binding<Keyed>> kmap = getKeyedMap(kclass);
        int candidate;
        do candidate = gen.nextId(serverId, maxServers);
        while (kmap.containsKey(candidate));
        return candidate;
    }

    /**
     * Returns the number of instances of the specified keyed entity hosted on this server.
     */
    public int census (Class<?> kclass) {
        return getKeyedMap(kclass).size();
    }

    /**
     * Clears an anonymous or child object registration.
     */
    public void clear (NexusObject object) {
        final int id = object.getId();
        Binding<NexusObject> bind = _objects.get(id);
        if (bind == null) {
            log.warning("Requested to clear unknown object", "id", id);
        }

        // prevent the object from dispatching any more events
        DistribUtil.clear(object);

        // queue up an action on this object's context that will remove this object's binding; this
        // will allow any pending events for this object to be processed before the binding is
        // cleared
        bind.context.postOp(new Runnable() {
            public void run () {
                _objects.remove(id);
                synchronized (_subscribers) {
                    // remove the subscriber set and notify them that the object was cleared
                    Set<Subscriber> subs = _subscribers.remove(id);
                    if (subs != null) {
                        for (Subscriber sub : subs) sub.onCleared(id);
                    }
                }
            }
        });
    }

    /**
     * Clears a singleton entity registration. If the entity is also a {@link NexusObject} its
     * object registration is also cleared.
     */
    public void clearSingleton (Singleton entity) {
        Class<?> sclass = getSingletonClass(entity.getClass());
        if (_singletons.remove(sclass) == null) {
            log.warning("Requested to clear unknown singleton", "class", sclass);
        }
        if (entity instanceof NexusObject) {
            clear((NexusObject)entity);
        }
    }

    /**
     * Clears a keyed entity registration. If the entity is also a {@link NexusObject} its object
     * registration is also cleared.
     */
    public void clearKeyed (Keyed entity) {
        Class<?> kclass = getKeyedClass(entity.getClass());
        ConcurrentMap<Comparable<?>,Binding<Keyed>> emap = getKeyedMap(kclass);
        if (emap.remove(entity.getKey()) == null) {
            log.warning("Requested to clear unknown keyed entity",
                        "class", kclass, "key", entity.getKey());
        }
        if (entity instanceof NexusObject) {
            clear((NexusObject)entity);
        }
    }

    /**
     * See {@link Nexus#assertContext(Class)}.
     */
    public <T extends Singleton> void assertContext (Class<T> sclass) {
        assert(requireSingleton(sclass, "No singleton registered for").context ==
               EntityContext.current.get());
    }

    /**
     * See {@link Nexus#assertContext(Class,Comparable)}.
     */
    public <T extends Keyed> void assertContext (Class<T> kclass, Comparable<?> key) {
        assert(requireKeyed(kclass, key, "No keyed entity registered for").context ==
               EntityContext.current.get());
    }

    /**
     * Invokes the supplied action on the specified singleton entity.
     */
    public <T extends Singleton> void invoke (Class<T> sclass, Action<? super T> action) {
        invoke(requireSingleton(sclass, "No singleton registered for"), action);
    }

    /**
     * Invokes the supplied action on the specified keyed entity. The entity must be local to this
     * server or an exception will be raised.
     */
    public <T extends Keyed> void invoke (Class<T> kclass, Comparable<?> key,
                                          Action<? super T> action) {
        try {
            invoke(requireKeyed(kclass, key, "No keyed entity registered for"), action);
        } catch (EntityNotFoundException enfe) {
            action.onDropped(_nexus, kclass, key);
        }
    }

    /**
     * Invokes the supplied request on the specified singleton entity.
     */
    public <T extends Singleton,R> Future<R> invoke (Class<T> sclass, Request<? super T,R> request) {
        return invoke(requireSingleton(sclass, "No singleton registered for"), request);
    }

    /**
     * Invokes the supplied request on the specified keyed entity. The entity must be local to this
     * server or an exception will be raised.
     */
    public <T extends Keyed,R> Future<R> invoke (Class<T> kclass, Comparable<?> key,
                                                 Request<? super T,R> request) {
        return invoke(requireKeyed(kclass, key, "No keyed entity registered for"), request);
    }

    /**
     * Invokes an action on the supplied target object.
     */
    public void invoke (int id, Action<NexusObject> action) {
        invoke(requireObject(id, "No object registered with id "), action);
    }

    /**
     * Requests that the supplied subscriber be added to the object with the specified address.
     *
     * @return the the object to which a subscriber was added.
     * @throws NexusException if the requested object does not exist, or if the instance registered
     * at the address is not a NexusObject, or if the request fails due to access control.
     */
    public <T extends NexusObject> T addSubscriber (Address<T> addr, Subscriber sub) {
        Binding<?> bind;
        if (addr instanceof Address.OfKeyed) {
            Address.OfKeyed<?> kaddr = (Address.OfKeyed<?>)addr;
            bind = requireKeyed(kaddr.clazz, kaddr.key, "No keyed entity registered for ");
        } else if (addr instanceof Address.OfSingleton) {
            Address.OfSingleton<?> saddr = (Address.OfSingleton<?>)addr;
            bind = requireSingleton(saddr.clazz, "No singleton registered for ");
        } else if (addr instanceof Address.OfAnonymous) {
            Address.OfAnonymous aaddr = (Address.OfAnonymous)addr;
            bind = requireObject(aaddr.id, "No object registered with id ");
        } else {
            throw new IllegalArgumentException("Unknown address type " + addr);
        }

        @SuppressWarnings("unchecked") T target = (T)bind.entity();
        // TODO: access control
        addSubscriber(target, sub);
        return target;
    }

    /**
     * Adds a subscriber to an existing object.
     */
    public void addSubscriber (NexusObject target, Subscriber sub) {
        int id = target.getId();
        if (id == 0) throw new NexusException("Cannot subscribe to unregistered object " + target);
        getSubscriberSet(id).add(sub);
    }

    /**
     * Requests that the supplied subscriber be removed from the object with the specified id.
     */
    public void clearSubscriber (int id, Subscriber sub) {
        Set<Subscriber> subs;
        synchronized (_subscribers) {
            subs = _subscribers.get(id);
        }
        if (subs != null) {
            if (!subs.remove(sub)) {
                log.warning("Requested to remove unknown subscriber", "id", id, "sub", sub);
            }
        } // otherwise, the object was probably already destroyed
    }

    /**
     * Dispatches the supplied event to the appropriate object, on the appropriate thread.
     * @param source the session from which the event originated, or null.
     */
    public void dispatchEvent (final NexusEvent event, final Session source) {
        final Set<Subscriber> subs;
        synchronized (_subscribers) {
            subs = _subscribers.get(event.targetId);
        }
        invoke(event.targetId, new Action<NexusObject>() {
            @Override public void invoke (NexusObject object) {
                SessionLocal.setCurrent(source);
                try {
                    // TODO: access control
                    // apply the event to the object (notifying listeners)
                    event.applyTo(object);
                } finally {
                    SessionLocal.clearCurrent();
                }

                // forward the event to any subscribers
                if (subs != null) {
                    for (Subscriber sub : subs) {
                        sub.forwardEvent(event);
                    }
                }
            }
            @Override public String toString() {
                return event.toString();
            }
        });
    }

    /**
     * Dispatches the supplied service call on the appropriate object, on the appropriate thread.
     * @param source the session from which the call originated, or null.
     */
    public <R> void dispatchCall (int objId, final short attrIdx, final short methId,
                                  final Object[] args, final Session source,
                                  final Slot<Try<R>> callback) {
        invoke(objId, new Action<NexusObject>() {
            @Override public void invoke (NexusObject object) {
                SessionLocal.setCurrent(source);
                try {
                    DistribUtil.dispatchCall(object, attrIdx, methId, args, callback);

                } finally {
                    SessionLocal.clearCurrent();
                }
            }
            @Override public String toString () {
                return "isvc:" + attrIdx + ":" + methId + ":" + args.length;
            }
        });
    }

    // from interface EventSink
    public void postEvent (NexusEvent event) {
        // events that originate on the server are dispatched directly
        dispatchEvent(event, null);
    }

    // from interface EventSink
    public <R> void postCall (NexusObject source, short attrIdx, short methId, Object[] args,
                              RPromise<R> result) {
        // calls that originate on the server are dispatched directly
        dispatchCall(source.getId(), attrIdx, methId, args, null, result.completer());
    }

    // from interface EventSink
    public String getHost () {
        return _config.publicHostname;
    }

    // from interface EventSink
    public void postEvent (NexusObject source, final NexusEvent event) {
        // TODO: fancy aggregation using thread-local accumulators
        postEvent(event);
    }

    protected void register (Singleton entity, EntityContext ctx) {
        Binding<Singleton> bind = Binding.simple(entity, ctx);
        Class<?> sclass = getSingletonClass(entity.getClass());
        if (_singletons.putIfAbsent(sclass, bind) != null) {
            throw new NexusException(
                "Singleton entity already registered for " + sclass.getName());
        }

        // if the entity is also a nexus object, register it as such
        if (entity instanceof NexusObject) {
            register((NexusObject)entity, ctx);
        }
    }

    protected void register (Keyed entity, EntityContext ctx) {
        // TODO: ensure that the key class is non-null and a legal streamable type
        Class<?> kclass = getKeyedClass(entity.getClass());
        ConcurrentMap<Comparable<?>,Binding<Keyed>> emap = getKeyedMap(kclass);
        Binding<Keyed> bind = Binding.simple(entity, ctx);
        Binding<Keyed> exist = emap.putIfAbsent(entity.getKey(), bind);
        if (exist != null) {
            throw new NexusException(
                "Keyed entity already registered for " + kclass.getName() + ":" + entity.getKey());
        }

        // finish up separately (shared bits also happen for auto-created keyed entities)
        finishRegisterKeyed(entity, ctx);
    }

    protected void finishRegisterKeyed (Keyed entity, EntityContext ctx) {
        // if the entity is also a nexus object, assign it an id
        if (entity instanceof NexusObject) {
            register((NexusObject)entity, ctx);
        }

        // TODO: report to the PeerManager that we host this keyed entity
    }

    protected void register (NexusObject object, EntityContext ctx) {
        int id = getNextObjectId();
        DistribUtil.init(object, id, this);
        _objects.put(id, Binding.simple(object, ctx));
    }

    protected Binding<Singleton> requireSingleton (Class<?> sclass, String errmsg) {
        Binding<Singleton> bind = _singletons.get(sclass);
        if (bind == null) {
            throw new NexusException(errmsg + " " + sclass.getName());
        }
        return bind;
    }

    protected Binding<Keyed> requireKeyed (Class<?> kclass, final Comparable<?> key, String errmsg) {
        final ConcurrentMap<Comparable<?>,Binding<Keyed>> emap = getKeyedMap(kclass);
        Binding<Keyed> bind = emap.get(key);
        if (bind != null) return bind;

        // if we have no factory for this entity, then it doesn't exist
        final KeyedFactory<?> fact = _kfacts.get(kclass);
        if (fact == null) throw new EntityNotFoundException(errmsg, kclass, key);

        // otherwise we'll auto-create the entity
        final EntityContext ctx = new EntityContext(_exec);
        bind = Binding.deferred(new Thunk<Keyed>() {
            public Keyed execute () {
                try {
                    Keyed entity = fact.create(_nexus, key);
                    finishRegisterKeyed(entity, ctx);
                    return entity;
                    // if entity auto-creation fails, we need to clear our binding
                } catch (RuntimeException re) {
                    emap.remove(key);
                    throw re;
                } catch (Error err) {
                    emap.remove(key);
                    throw err;
                }
            }
        }, ctx);
        Binding<Keyed> exist = emap.putIfAbsent(key, bind);
        // if someone beat us to the punch, return their binding, not ours
        return (exist == null) ? bind : exist;
    }

    protected Binding<NexusObject> requireObject (int id, String errmsg) {
        Binding<NexusObject> bind = _objects.get(id);
        if (bind == null) {
            throw new NexusException(errmsg + id);
        }
        return bind;
    }

    protected <T> void invoke (final Binding<?> bind, final Action<T> action) {
        if (_safetyChecks) defangAction(action);

        bind.context.postOp(new Runnable() {
            public void run () {
                @SuppressWarnings("unchecked") final T entity = (T)bind.entity();
                action.invoke(entity);
            }
        });
    }

    protected <T,R> Future<R> invoke (final Binding<?> bind, final Request<T,R> request) {
        if (_safetyChecks) defangAction(request);

        // post the request execution as a future task
        FutureTask<R> task = new FutureTask<R>(new Callable<R>() {
            public R call () {
                @SuppressWarnings("unchecked") final T entity = (T)bind.entity();
                return request.invoke(entity);
            }
        });
        bind.context.postOp(task);
        return task;
    }

    protected void defangAction (Object action) {
        Class<?> aclass = action.getClass();
        for (java.lang.reflect.Field field: aclass.getDeclaredFields()) {
            String name = field.getName();
            if (name.startsWith("this$") || // Java-style captured this pointers
                name.endsWith("$outer")) {  // Scala-style captured this pointers
                try {
                    field.setAccessible(true);
                    field.set(action, null);
                } catch (IllegalAccessException iae) {
                    throw new NexusException("Error defanging outer-this pointer " + field, iae);
                }
            }
        }
    }

    protected <K,V> void emitMapPut (String id, K key, V value, V oldValue) {
        // TODO: send this update off to the peer manager to be sent to the other servers in the
        // next map broadcast
        applyMapPut(id, key, value, oldValue);
    }

    protected <K,V> void emitMapRemove (String id, K key, V oldValue) {
        // TODO: send this update off to the peer manager to be sent to the other servers in the
        // next map broadcast
        applyMapRemove(id, key, oldValue);
    }

    protected <K,V> void applyMapPut (String id, final K key, final V value, final V oldValue) {
        final Binding<GlobalMap<?,?>> bind = _maps.get(id);
        if (bind == null) {
            log.warning("Got put for unknown map", "id", id, "key", key, "value", value,
                        "ovalue", oldValue);
            return;
        }
        bind.context.postOp(new Runnable() {
            public void run () {
                @SuppressWarnings("unchecked") final GlobalMap<K,V> map =
                    (GlobalMap<K,V>)bind.entity();
                map.applyPut(key, value, oldValue);
            }
        });
    }

    protected <K,V> void applyMapRemove (String id, final K key, final V oldValue) {
        final Binding<GlobalMap<?,?>> bind = _maps.get(id);
        if (bind == null) {
            log.warning("Got remove for unknown map", "id", id, "key", key, "ovalue", oldValue);
            return;
        }
        bind.context.postOp(new Runnable() {
            public void run () {
                @SuppressWarnings("unchecked") final GlobalMap<K,V> map =
                    (GlobalMap<K,V>)bind.entity();
                map.applyRemove(key, oldValue);
            }
        });
    }

    protected ConcurrentMap<Comparable<?>,Binding<Keyed>> getKeyedMap (Class<?> kclass) {
        ConcurrentMap<Comparable<?>,Binding<Keyed>> emap = _keyeds.get(kclass);
        if (emap == null) {
            ConcurrentMap<Comparable<?>,Binding<Keyed>> cmap =
                _keyeds.putIfAbsent(kclass, emap = Maps.newConcurrentMap());
            // if someone beat us to the punch, we need to use their map, not ours
            if (cmap != null) {
                emap = cmap;
            }
        }
        return emap;
    }

    protected Set<Subscriber> getSubscriberSet (int targetId) {
        synchronized (_subscribers) {
            Set<Subscriber> subs = _subscribers.get(targetId);
            if (subs == null) {
                subs = new ConcurrentSkipListSet<Subscriber>(SUBSCRIBER_COMP);
                _subscribers.put(targetId, subs);
            }
            return subs;
        }
    }

    protected final synchronized int getNextObjectId () {
        // look for the next unused oid; if we had two billion objects, this would loop infinitely,
        // but the world will come to an end long before we have two billion objects
        do _nextId = (_nextId == Integer.MAX_VALUE) ? 1 : (_nextId + 1);
        while (_objects.containsKey(_nextId));
        return _nextId;
    }

    protected static Class<?> getKeyedClass (Class<?> kclass) {
        if (kclass == Object.class) throw new AssertionError(
            "Keyed instance lacks implementation of Keyed interface!?");
        for (Class<?> iface : kclass.getInterfaces()) {
            if (iface.equals(Keyed.class)) return kclass;
        }
        return getKeyedClass(kclass.getSuperclass());
    }

    protected static Class<?> getSingletonClass (Class<?> sclass) {
        if (sclass == Object.class) throw new AssertionError(
            "Singleton instance lacks implementation of Singleton interface!?");
        for (Class<?> iface : sclass.getInterfaces()) {
            if (iface.equals(Singleton.class)) return sclass;
        }
        return getSingletonClass(sclass.getSuperclass());
    }

    /** Used to auto-create entities. */
    protected interface Thunk<T> { T execute (); }

    /** Maintains bindings of entities to contexts. */
    protected static abstract class Binding<T> {
        public static <T> Binding<T> simple (final T entity, EntityContext ctx) {
            return new Binding<T>(ctx) {
                public T entity () { return entity; }
            };
        }

        public static <T> Binding<T> deferred (final Thunk<T> thunk, EntityContext ctx) {
            return new Binding<T>(ctx) {
                public T entity () {
                    if (_entity == null) _entity = thunk.execute();
                    return _entity;
                }
                // we don't have to worry about thread safety here because we know that entity() is
                // only ever called on this binding's execution context
                protected T _entity;
            };
        }

        public final EntityContext context;
        public abstract T entity ();

        private Binding (EntityContext ctx) {
            this.context = ctx;
        }
    }

    protected static class IdGen {
        public synchronized int nextId (int offset, int skip) {
            _nextId += skip;
            int id = _nextId + offset;
            if (id > 0) return id;
            // if we overflowed, wrap back around to zero
            _nextId = 0;
            return _nextId + offset;
        }
        protected int _nextId;
    }

    /** Contains our server configuration. */
    protected NexusConfig _config;

    /** The Nexus in which we're operating. */
    protected Nexus _nexus;

    /** The executor we use to execute actions and requests. */
    protected final Executor _exec;

    /** Used to assign ids to newly registered objects. See {@link #getNextObjectId}. */
    protected int _nextId = 0;

    /** A mapping of all nexus objects hosted on this server. */
    protected final ConcurrentMap<Integer,Binding<NexusObject>> _objects = Maps.newConcurrentMap();

    /** A mapping of all singleton entities hosted on this server. */
    protected final ConcurrentMap<Class<?>,Binding<Singleton>> _singletons =
        Maps.newConcurrentMap();

    /** A mapping of all keyed entities hosted on this server. */
    protected final ConcurrentMap<Class<?>,ConcurrentMap<Comparable<?>,Binding<Keyed>>> _keyeds =
        Maps.newConcurrentMap();

    /** A mapping of factories for keyed entities. */
    protected final ConcurrentMap<Class<?>,KeyedFactory<?>> _kfacts = Maps.newConcurrentMap();

    /** A mapping of id generators for keyed entities. */
    protected final ConcurrentMap<Class<?>,IdGen> _kidgens = Maps.newConcurrentMap();

    /** A mapping of distributed maps known to this server. */
    protected final ConcurrentMap<String,Binding<GlobalMap<?,?>>> _maps = Maps.newConcurrentMap();

    /** A mapping from object id to subscriber set. The outer mapping will only be modified under
     * synchronization, but the inner-set allows concurrent modifications. */
    protected final Map<Integer,Set<Subscriber>> _subscribers = Maps.newHashMap();

    /** Indicates whether runtime safety checks should be made. They are expensive and thus should
     * only be enabled during development. */
    protected final boolean _safetyChecks = Boolean.getBoolean("nexus.safety_checks");

    /** A comparator on subscribers which orders by hashcode. */
    protected static final Comparator<Subscriber> SUBSCRIBER_COMP = new Comparator<Subscriber>() {
        public int compare (Subscriber s1, Subscriber s2) {
            int h1 = s1.hashCode(), h2 = s2.hashCode();
            if (h1 < h2) return -1;
            else if (h1 > h2) return 1;
            else return 0;
        }
    };
}
