//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.server;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;

import com.google.common.collect.Maps;

import com.samskivert.nexus.distrib.Action;
import com.samskivert.nexus.distrib.Address;
import com.samskivert.nexus.distrib.DService;
import com.samskivert.nexus.distrib.DistribUtil;
import com.samskivert.nexus.distrib.EventSink;
import com.samskivert.nexus.distrib.Keyed;
import com.samskivert.nexus.distrib.NexusEvent;
import com.samskivert.nexus.distrib.NexusException;
import com.samskivert.nexus.distrib.NexusObject;
import com.samskivert.nexus.distrib.Request;
import com.samskivert.nexus.distrib.Singleton;
import com.samskivert.nexus.util.Callback;

import static com.samskivert.nexus.util.Log.log;

/**
 * Maintains metadata for all registered entities including their contexts. Contexts are action
 * queues that are guaranteed to be processed in a single-threaded manner.
 */
public class ObjectManager
    implements EventSink
{
    /** An interface implemented by sessions, which must forward object events to clients. */
    public interface Subscriber
    {
        /** Notifies the subscriber of an event which must be forwarded. */
        void forwardEvent (NexusEvent event);
    }

    public ObjectManager (NexusConfig config, Executor exec)
    {
        _config = config;
        _exec = exec;
    }

    /**
     * Registers the supplied object in its own context.
     */
    public void register (NexusObject object)
    {
        register(object, new EntityContext(_exec));
    }

    /**
     * Registers the supplied child object in the context of its parent.
     */
    public void registerChild (Singleton parent, NexusObject child)
    {
        Binding<Singleton> pbind = requireSingleton(
            parent.getClass(), "Can't bind child to unregistered singleton parent");
        register(child, pbind.context);
    }

    /**
     * Registers the supplied child object in the context of its parent.
     */
    public void registerChild (Keyed parent, NexusObject child)
    {
        Binding<Keyed> pbind = requireKeyed(
            parent.getClass(), parent.getKey(), "Can't bind child to unregistered keyed parent");
        register(child, pbind.context);
    }

    /**
     * Registers the supplied singleton entity in its own context.
     *
     * @throws NexusException if an entity is already mapped for this singleton type.
     */
    public void registerSingleton (Singleton entity)
    {
        Binding<Singleton> bind = new Binding<Singleton>(entity, new EntityContext(_exec));
        Class<?> sclass = entity.getClass();
        if (_singletons.putIfAbsent(sclass, bind) != null) {
            throw new NexusException(
                "Singleton entity already registered for " + sclass.getName());
        }

        // if the entity is also a nexus object, register it as such
        if (entity instanceof NexusObject) {
            register((NexusObject)entity, bind.context);
        }
    }

    /**
     * Registers the supplied keyed entity in its own context.
     *
     * @throws NexusException if an entity is already mapped with the entity's key.
     */
    public void registerKeyed (Keyed entity)
    {
        // TODO: ensure that the key class is non-null and a legal streamable type
        Class<?> kclass = entity.getClass();
        ConcurrentMap<Comparable<?>,Binding<Keyed>> emap = _keyeds.get(kclass);
        if (emap == null) {
            ConcurrentMap<Comparable<?>,Binding<Keyed>> cmap =
                _keyeds.putIfAbsent(kclass, emap = Maps.newConcurrentMap());
            // if someone beat us to the punch, we need to use their map, not ours
            if (cmap != null) {
                emap = cmap;
            }
        }

        Binding<Keyed> bind = new Binding<Keyed>(entity, new EntityContext(_exec));
        Binding<Keyed> exist = emap.putIfAbsent(entity.getKey(), bind);
        if (exist != null) {
            throw new NexusException(
                "Keyed entity already registered for " + kclass.getName() + ":" + entity.getKey());
        }

        // if the entity is also a nexus object, assign it an id
        if (entity instanceof NexusObject) {
            register((NexusObject)entity, bind.context);
        }

        // TODO: report to the PeerManager that we host this keyed entity
    }

    /**
     * Returns true if we host the specified keyed entity, false if not.
     */
    public boolean hostsKeyed (Class<?> eclass, Comparable<?> key)
    {
        ConcurrentMap<Comparable<?>,Binding<Keyed>> emap = _keyeds.get(eclass);
        return (emap == null) ? false : emap.containsKey(key);
    }

    /**
     * Clears an anonymous or child object registration.
     */
    public void clear (NexusObject object)
    {
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
                    _subscribers.remove(id);
                }
            }
        });
    }

    /**
     * Clears a singleton entity registration. If the entity is also a {@link NexusObject} its
     * object registration is also cleared.
     */
    public void clearSingleton (Singleton entity)
    {
        if (_singletons.remove(entity.getClass()) == null) {
            log.warning("Requested to clear unknown singleton", "class", entity.getClass());
        }
        if (entity instanceof NexusObject) {
            clear((NexusObject)entity);
        }
    }

    /**
     * Clears a keyed entity registration. If the entity is also a {@link NexusObject} its object
     * registration is also cleared.
     */
    public void clearKeyed (Keyed entity)
    {
        ConcurrentMap<Comparable<?>,Binding<Keyed>> emap = _keyeds.get(entity.getClass());
        if (emap.remove(entity.getKey()) == null) {
            log.warning("Requested to clear unknown keyed entity",
                        "class", entity.getClass(), "key", entity.getKey());
        }
        if (entity instanceof NexusObject) {
            clear((NexusObject)entity);
        }
    }

    /**
     * Invokes the supplied action on the specified singleton entity.
     *
     * @throws NexusException if no singleton instance is registered for the specified type.
     */
    public <T extends Singleton> void invoke (Class<T> eclass, Action<T> action)
    {
        invoke(requireSingleton(eclass, "No singleton registered for"), action);
    }

    /**
     * Invokes the supplied action on the specified keyed entity. The entity must be local to this
     * server or an exception will be raised.
     */
    public <T extends Keyed> void invoke (Class<T> eclass, Comparable<?> key, Action<T> action)
    {
        invoke(requireKeyed(eclass, key, "No singleton registered for"), action);
    }

    /**
     * Invokes the supplied request on the specified singleton entity. The caller will block until
     * the request is processed.
     *
     * @throws NexusException wrapping any exception thrown by the request.
     */
    public <T extends Singleton,R> R invoke (Class<T> eclass, Request<T,R> request)
    {
        return invoke(requireSingleton(eclass, "No singleton registered for"), request);
    }

    /**
     * Invokes the supplied request on the specified keyed entity. The entity must be local to this
     * server or an exception will be raised. The caller will block until the request is processed.
     */
    public <T extends Keyed,R> R invoke (Class<T> eclass, Comparable<?> key, Request<T,R> request)
    {
        return invoke(requireKeyed(eclass, key, "No keyed entity registered for"), request);
    }

    /**
     * Invokes an action on the supplied target object, if it exists.
     */
    public void invoke (int id, Action<NexusObject> action)
    {
        invoke(requireObject(id, "No object registered with id "), action);
    }

    /**
     * Requests that the supplied subscriber be added to the object with the specified address.
     *
     * @return the the object to which a subscriber was added.
     * @throws NexusException if the requested object does not exist, or if the instance registered
     * at the address is not a NexusObject, or if the request fails due to access control.
     */
    public <T extends NexusObject> T addSubscriber (Address<T> addr, Subscriber sub)
    {
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

        if (!(bind.entity instanceof NexusObject)) {
            throw new NexusException(bind.entity.getClass().getName() + " is not a NexusObject");
        }

        @SuppressWarnings("unchecked") T target = (T)bind.entity;
        // TODO: access control
        getSubscriberSet(target.getId()).add(sub);
        return target;
    }

    /**
     * Requests that the supplied subscriber be removed from the object with the specified id.
     */
    public void clearSubscriber (int id, Subscriber sub)
    {
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

    // from interface EventSink
    public void postEvent (final NexusEvent event)
    {
        final Set<Subscriber> subs;
        synchronized (_subscribers) {
            subs = _subscribers.get(event.targetId);
        }
        invoke(event.targetId, new Action<NexusObject>() {
            public void invoke (NexusObject object) {
                // TODO: access control
                // apply the event to the object (notifying listeners)
                event.applyTo(object);
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

    // from interface EventSink
    public void postCall (int objId, final short attrIdx, final short methId, final Object[] args)
    {
        invoke(objId, new Action<NexusObject>() {
            public void invoke (NexusObject object) {
                DistribUtil.dispatchCall(object, attrIdx, methId, args);
            }
            @Override public String toString () {
                return "isvc:" + attrIdx + ":" + methId + ":" + args.length;
            }
        });
    }

    // from interface EventSink
    public String getHost ()
    {
        return _config.publicHostname;
    }

    // from interface EventSink
    public void postEvent (NexusObject source, final NexusEvent event)
    {
        // TODO: fancy aggregation using thread-local accumulators
        postEvent(event);
    }

    protected void register (NexusObject object, EntityContext ctx)
    {
        int id = getNextObjectId();
        DistribUtil.init(object, id, this);
        _objects.put(id, new Binding<NexusObject>(object, ctx));
    }

    protected Binding<Singleton> requireSingleton (Class<?> eclass, String errmsg)
    {
        Binding<Singleton> bind = _singletons.get(eclass);
        if (bind == null) {
            throw new NexusException(errmsg + " " + eclass.getName());
        }
        return bind;
    }

    protected Binding<Keyed> requireKeyed (Class<?> eclass, Comparable<?> key, String errmsg)
    {
        ConcurrentMap<Comparable<?>,Binding<Keyed>> emap = _keyeds.get(eclass);
        if (emap == null) {
            throw new NexusException(errmsg + " " + eclass + ":" + key);
        }
        Binding<Keyed> bind = emap.get(key);
        if (bind == null) {
            throw new NexusException(errmsg + " " + eclass + ":" + key);
        }
        return bind;
    }

    protected Binding<NexusObject> requireObject (int id, String errmsg)
    {
        Binding<NexusObject> bind = _objects.get(id);
        if (bind == null) {
            throw new NexusException(errmsg + id);
        }
        return bind;
    }

    protected <T> void invoke (Binding<?> bind, final Action<T> action)
    {
        @SuppressWarnings("unchecked") final T entity = (T)bind.entity;
        bind.context.postOp(new Runnable() {
            public void run () {
                action.invoke(entity);
            }
        });
    }

    protected <T,R> R invoke (Binding<?> bind, final Request<T,R> request)
    {
        @SuppressWarnings("unchecked") final T entity = (T)bind.entity;

        // post the request execution as a future task
        FutureTask<R> task = new FutureTask<R>(new Callable<R>() {
            public R call () {
                return request.invoke(entity);
            }
        });
        bind.context.postOp(task);

        // block, awaiting the completion of the request, and return its response
        try {
            // TODO: should we use the same timeout we use for remote requests?
            return task.get();
        } catch (ExecutionException ee) {
            throw new NexusException("Request failure " + request, ee.getCause());
        } catch (InterruptedException ie) {
            throw new NexusException("Interrupted while waiting for request " + request);
        }
    }

    protected Set<Subscriber> getSubscriberSet (int targetId)
    {
        synchronized (_subscribers) {
            Set<Subscriber> subs = _subscribers.get(targetId);
            if (subs == null) {
                subs = new ConcurrentSkipListSet<Subscriber>(SUBSCRIBER_COMP);
                _subscribers.put(targetId, subs);
            }
            return subs;
        }
    }

    protected final synchronized int getNextObjectId ()
    {
        // look for the next unused oid; if we had two billion objects, this would loop infinitely,
        // but the world will come to an end long before we have two billion objects
        do {
            _nextId = (_nextId == Integer.MAX_VALUE) ? 1 : (_nextId + 1);
        } while (_objects.containsKey(_nextId));
        return _nextId;
    }

    /** Maintains bindings of entities to contexts. */
    protected static class Binding<T> {
        public final T entity;
        public final EntityContext context;
        public Binding (T entity, EntityContext context) {
            this.entity = entity;
            this.context = context;
        }
    }

    /** Contains our server configuration. */
    protected NexusConfig _config;

    /** The executor we use to execute actions and requests. */
    protected final Executor _exec;

    /** Used to assign ids to newly registered objects. See {@link #getNextObjectId}. */
    protected int _nextId = 0;

    /** A mapping of all nexus objects hosted on this server. */
    protected final ConcurrentMap<Integer,Binding<NexusObject>> _objects = Maps.newConcurrentMap();

    /** A mapping of all singleton entities hosted on this server. */
    protected final ConcurrentMap<Class<?>,Binding<Singleton>> _singletons = Maps.newConcurrentMap();

    /** A mapping of all keyed entities hosted on this server. */
    protected final ConcurrentMap<Class<?>,ConcurrentMap<Comparable<?>,Binding<Keyed>>> _keyeds =
        Maps.newConcurrentMap();

    /** A mapping from object id to subscriber set. The outer mapping will only be modified under
     * synchronization, but the inner-set allows concurrent modifications. */
    protected final Map<Integer,Set<Subscriber>> _subscribers = Maps.newHashMap();

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
