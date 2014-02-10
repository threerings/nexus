//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.server;

import java.util.Collections;
import java.util.Comparator;
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
        /** Called (on `object`'s thread context) once the subscription has been added. */
        void onSubscribed (NexusObject object);
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
    public <N extends NexusObject> Nexus.Context<N> register (N object) {
        return register(object, new EntityContext(_exec));
    }

    /**
     * Registers the supplied object in the context of the specified parent.
     */
    public void register (NexusObject child, Nexus.Context<?> parent) {
        register(child, ((Binding<?>)parent).context);
    }

    /**
     * Registers the supplied singleton entity in its own context.
     */
    public <S extends Singleton> Nexus.Context<S> register (Class<? super S> sclass, S entity) {
        return register(sclass, entity, new EntityContext(_exec));
    }

    /**
     * Registers the supplied singleton in the context of its parent.
     */
    public <S extends Singleton> void register (Class<? super S> sclass, S child,
                                                Nexus.Context<?> parent) {
        register(sclass, child, ((Binding<?>)parent).context);
    }

    /**
     * Registers the supplied keyed entity in its own context.
     *
     * @throws NexusException if an entity is already mapped with the entity's key.
     */
    public <K extends Keyed> Nexus.Context<K> registerKeyed (Class<? super K> kclass, K entity) {
        return register(kclass, entity.getKey(), entity, new EntityContext(_exec));
    }

    /**
     * Registers the supplied keyed entity in the context of its parent.
     */
    public <K extends Keyed> void registerKeyed (Class<? super K> kclass, K child,
                                                 Nexus.Context<?> parent) {
        register(kclass, child.getKey(), child, ((Binding<?>)parent).context);
    }

    /**
     * Registers a factory for keyed entities of type {@code kclass}.
     */
    public <E extends Keyed> void registerKeyedFactory (Class<? super E> kclass,
                                                        KeyedFactory<E> kf) {
        KeyedFactory<?> ofact = _kfacts.putIfAbsent(kclass, kf);
        if (ofact != null) {
            log.warning("Rejected request to overwrite keyed entity factory",
                        "kclass", kclass, "ofact", ofact, "nfact", kf);
        }
    }

    /**
     * Registers a global map and its associated context.
     */
    public <K,V> RMap<K,V> registerMap (String id) {
        GlobalMap<K,V> map = new GlobalMap<K,V>(id, this);
        // oddly, we have to tell Java that "forgetting" K and V here is safe, and we have to do
        // some serious backbending to accomplish it to boot; damned half-assed existentials
        Binding<?> bind = simpleBinding(map, new EntityContext(_exec));
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
    public <K extends Keyed> boolean hostsKeyed (Class<K> kclass, Comparable<?> key) {
        return getKeyedMap(kclass).containsKey(key);
    }

    /**
     * Returns the set of keys for all instances of {@code kclass} hosted by this manager.
     */
    public <K extends Keyed> Set<Comparable<?>> hostedKeys (Class<K> kclass) {
        return Collections.unmodifiableSet(getKeyedMap(kclass).keySet());
    }

    /**
     * Returns an unused identifier for the specified keyed entity class.
     */
    public <K extends Keyed> int nextId (int serverId, int maxServers, Class<K> kclass) {
        IdGen gen = _kidgens.get(kclass);
        if (gen == null) {
            // do the "add to concurrent map" shuffle
            IdGen collide = _kidgens.putIfAbsent(kclass, gen = new IdGen());
            if (collide != null) gen = collide;
        }

        ConcurrentMap<Comparable<?>,Binding<K>> kmap = getKeyedMap(kclass);
        int candidate;
        do candidate = gen.nextId(serverId, maxServers);
        while (kmap.containsKey(candidate));
        return candidate;
    }

    /**
     * Returns the number of instances of the specified keyed entity hosted on this server.
     */
    public <K extends Keyed> int census (Class<K> kclass) {
        return getKeyedMap(kclass).size();
    }

    /**
     * Clears an anonymous or child object registration.
     */
    public void clear (NexusObject object) {
        final int id = object.getId();
        Binding<?> bind = _objects.get(id);
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
    public <S extends Singleton> void clear (Class<? super S> sclass, S entity) {
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
    public <K extends Keyed> void clearKeyed (Class<? super K> kclass, K entity) {
        ConcurrentMap<Comparable<?>,Binding<K>> emap = getKeyedMap(kclass);
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
        assert(require(sclass, "No singleton registered for").context ==
               EntityContext.current.get());
    }

    /**
     * See {@link Nexus#assertContext(Class,Comparable)}.
     */
    public <T extends Keyed> void assertContext (Class<T> kclass, Comparable<?> key) {
        assert(require(kclass, key, "No keyed entity registered for").context ==
               EntityContext.current.get());
    }

    /**
     * Invokes the supplied action on the specified singleton entity.
     */
    public <T extends Singleton> void invoke (Class<T> sclass, Action<? super T> action) {
        require(sclass, "No singleton registered for").invoke(action);
    }

    /**
     * Invokes the supplied action on the specified keyed entity. The entity must be local to this
     * server or an exception will be raised.
     */
    public <T extends Keyed> void invoke (Class<T> kclass, Comparable<?> key,
                                          Action<? super T> action) {
        try {
            require(kclass, key, "No keyed entity registered for").invoke(action);
        } catch (EntityNotFoundException enfe) {
            action.onDropped(_nexus, kclass, key);
        }
    }

    /**
     * Invokes the supplied request on the specified singleton entity.
     */
    public <S extends Singleton,R> Future<R> invoke (Class<S> sclass, Request<? super S,R> request) {
        return require(sclass, "No singleton registered for").requestF(request);
    }

    /**
     * Invokes the supplied request on the specified keyed entity. The entity must be local to this
     * server or an exception will be raised.
     */
    public <K extends Keyed,R> Future<R> invoke (Class<K> kclass, Comparable<?> key,
                                                 Request<? super K,R> request) {
        return require(kclass, key, "No keyed entity registered for").requestF(request);
    }

    /**
     * Invokes an action on the supplied target object.
     */
    public void invoke (int id, Action<NexusObject> action) {
        requireObject(id, "No object registered with id ").invoke(action);
    }

    /**
     * Requests that the supplied subscriber be added to the object with the specified address.
     *
     * @return the the object to which a subscriber was added.
     * @throws NexusException if the requested object does not exist, or if the instance registered
     * at the address is not a NexusObject, or if the request fails due to access control.
     */
    public <T extends NexusObject> void addSubscriber (Address<T> addr, final Subscriber sub) {
        Binding<?> bind;
        if (addr instanceof Address.OfKeyed) {
            Address.OfKeyed<?> kaddr = (Address.OfKeyed<?>)addr;
            @SuppressWarnings("unchecked") Class<Keyed> kclass = (Class<Keyed>)kaddr.clazz;
            bind = require(kclass, kaddr.key, "No keyed entity registered for ");
        } else if (addr instanceof Address.OfSingleton) {
            Address.OfSingleton<?> saddr = (Address.OfSingleton<?>)addr;
            @SuppressWarnings("unchecked") Class<Singleton> sclass = (Class<Singleton>)saddr.clazz;
            bind = require(sclass, "No singleton registered for ");
        } else if (addr instanceof Address.OfAnonymous) {
            Address.OfAnonymous aaddr = (Address.OfAnonymous)addr;
            bind = requireObject(aaddr.id, "No object registered with id ");
        } else {
            throw new IllegalArgumentException("Unknown address type " + addr);
        }

        @SuppressWarnings("unchecked") T target = (T)bind.entity();
        // TODO: access control (the calling session is bound during this call, so we need to
        // access control here or find some other way of passing the session along if we intend to
        // move over to the object's thread)
        invoke(target.getId(), new Action.Local<NexusObject>() {
            @Override public void invoke (NexusObject object) {
                addSubscriber(object, sub);
                sub.onSubscribed(object);
            }
        });
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
    public boolean clearSubscriber (int id, Subscriber sub) {
        Set<Subscriber> subs;
        synchronized (_subscribers) {
            subs = _subscribers.get(id);
        }
        if (subs == null) return false; // the object was probably already destroyed
        if (subs.remove(sub)) return true;
        log.warning("Requested to remove unknown subscriber", "id", id, "sub", sub);
        return false;
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
        invoke(event.targetId, new Action.Local<NexusObject>() {
            @Override public void invoke (NexusObject object) {
                SessionLocal.setCurrent(source);
                try {
                    // TODO: access control
                    // apply the event to the object (notifying listeners)
                    event.applyTo(object);
                } finally {
                    SessionLocal.clearCurrent();
                }

                // forward the event to any subscribers (we do this on the object's thread to
                // interact nicely with the subscription process, which also takes place on the
                // object's thread; by ensuring that both things are done on the object's thread,
                // we avoid opening a window in which events could be sent to a client that was in
                // the process of subscribing to an object but had not yet received its response)
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
        invoke(objId, new Action.Local<NexusObject>() {
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

    protected <S extends Singleton> Binding<S> register (Class<? super S> sclass, S entity,
                                                         EntityContext ctx) {
        Binding<S> bind = simpleBinding(entity, ctx);
        if (_singletons.putIfAbsent(sclass, bind) != null) {
            throw new NexusException("Singleton entity already registered for " + sclass.getName());
        }
        // if the entity is also a nexus object, register it as such
        if (entity instanceof NexusObject) {
            register((NexusObject)entity, ctx);
        }
        return bind;
    }

    protected <K extends Keyed> Binding<K> register (Class<? super K> kclass, Comparable<?> key,
                                                     K entity, EntityContext ctx) {
        // TODO: ensure that the key class is non-null and a legal streamable type
        ConcurrentMap<Comparable<?>,Binding<K>> emap = getKeyedMap(kclass);
        Binding<K> bind = simpleBinding(entity, ctx);
        Binding<K> exist = emap.putIfAbsent(key, bind);
        if (exist != null) throw new NexusException(
            "Keyed entity already registered for " + kclass.getName() + ":" + key);

        // finish up separately (shared bits also happen for auto-created keyed entities)
        finishRegisterKeyed(entity, ctx);
        return bind;
    }

    protected void finishRegisterKeyed (Keyed entity, EntityContext ctx) {
        // if the entity is also a nexus object, assign it an id
        if (entity instanceof NexusObject) {
            register((NexusObject)entity, ctx);
        }

        // TODO: report to the PeerManager that we host this keyed entity
    }

    protected <N extends NexusObject> Binding<N> register (N object, EntityContext ctx) {
        int id = getNextObjectId();
        DistribUtil.init(object, id, this);
        Binding<N> bind = simpleBinding(object, ctx);
        _objects.put(id, bind);
        return bind;
    }

    protected <E extends Singleton> Binding<E> require (Class<? super E> sclass, String errmsg) {
        @SuppressWarnings("unchecked") Binding<E> bind = (Binding<E>)_singletons.get(sclass);
        if (bind == null) {
            throw new NexusException(errmsg + " " + sclass.getName());
        }
        return bind;
    }

    protected <K extends Keyed> Binding<K> require (Class<? super K> kclass, final Comparable<?> key,
                                                    String errmsg) {
        final ConcurrentMap<Comparable<?>,Binding<K>> emap = getKeyedMap(kclass);
        Binding<K> bind = emap.get(key);
        if (bind != null) return bind;

        // if we have no factory for this entity, then it doesn't exist
        @SuppressWarnings("unchecked") final KeyedFactory<K> fact =
            (KeyedFactory<K>)_kfacts.get(kclass);
        if (fact == null) throw new EntityNotFoundException(errmsg, kclass, key);

        // otherwise we'll auto-create the entity
        final EntityContext ctx = new EntityContext(_exec);
        bind = deferredBinding(new Thunk<K>() {
            public K execute () {
                try {
                    K entity = fact.create(_nexus, key);
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
        Binding<K> exist = emap.putIfAbsent(key, bind);
        // if someone beat us to the punch, return their binding, not ours
        return (exist == null) ? bind : exist;
    }

    protected Binding<NexusObject> requireObject (int id, String errmsg) {
        @SuppressWarnings("unchecked") Binding<NexusObject> bind =
            (Binding<NexusObject>)_objects.get(id);
        if (bind == null) {
            throw new NexusException(errmsg + id);
        }
        return bind;
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

    protected <E extends Keyed> ConcurrentMap<Comparable<?>,Binding<E>> getKeyedMap (
        Class<? super E> kclass) {
        ConcurrentMap<Comparable<?>,Binding<?>> emap = _keyeds.get(kclass);
        if (emap == null) {
            ConcurrentMap<Comparable<?>,Binding<?>> cmap =
                _keyeds.putIfAbsent(kclass, emap = Maps.newConcurrentMap());
            // if someone beat us to the punch, we need to use their map, not ours
            if (cmap != null) {
                emap = cmap;
            }
        }
        Object noreally = emap;
        @SuppressWarnings("unchecked") ConcurrentMap<Comparable<?>,Binding<E>> casted =
            (ConcurrentMap<Comparable<?>,Binding<E>>)noreally;
        return casted;
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

    protected <E> Binding<E> simpleBinding (final E entity, EntityContext ctx) {
        return new Binding<E>(ctx) {
            public E entity () { return entity; }
        };
    }

    protected <E> Binding<E> deferredBinding (final Thunk<E> thunk, EntityContext ctx) {
        return new Binding<E>(ctx) {
            public E entity () {
                if (_entity == null) _entity = thunk.execute();
                return _entity;
            }
            // we don't have to worry about thread safety here because we know that entity() is
            // only ever called on this binding's execution context
            protected E _entity;
        };
    }

    /** Used to auto-create entities. */
    protected interface Thunk<T> { T execute (); }

    /** Maintains bindings of entities to contexts. */
    protected abstract class Binding<E> implements Nexus.Context<E> {
        @Override // from interface Nexus.Context
        public void invoke (final Action<? super E> action) {
            if (_safetyChecks && !(action instanceof Action.Local<?>)) defangAction(action);
            context.postOp(new Runnable() {
                public void run () {
                    action.invoke(entity());
                }
            });
        }

        @Override // from interface Nexus.Context
        public <R> Future<R> requestF (final Request<? super E,R> request) {
            if (_safetyChecks && !(request instanceof Request.Local<?,?>)) defangAction(request);
            // post the request execution as a future task
            FutureTask<R> task = new FutureTask<R>(new Callable<R>() {
                public R call () {
                    return request.invoke(entity());
                }
            });
            context.postOp(task);
            return task;
        }

        @Override // from interface Nexus.Context
        public <R> R request (Request<? super E,R> request) {
            return null;
        }

        public final EntityContext context;
        public abstract E entity ();

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
    protected final ConcurrentMap<Integer,Binding<?>> _objects = Maps.newConcurrentMap();

    /** A mapping of all singleton entities hosted on this server. */
    protected final ConcurrentMap<Class<?>,Binding<?>> _singletons = Maps.newConcurrentMap();

    /** A mapping of all keyed entities hosted on this server. */
    protected final ConcurrentMap<Class<?>,ConcurrentMap<Comparable<?>,Binding<?>>> _keyeds =
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
