//
// $Id$

package com.samskivert.nexus.server;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;

import com.google.common.collect.Maps;

import com.samskivert.nexus.distrib.Action;
import com.samskivert.nexus.distrib.EventSink;
import com.samskivert.nexus.distrib.Keyed;
import com.samskivert.nexus.distrib.NexusEvent;
import com.samskivert.nexus.distrib.NexusException;
import com.samskivert.nexus.distrib.NexusObject;
import com.samskivert.nexus.distrib.Request;
import com.samskivert.nexus.distrib.Singleton;

import static com.samskivert.nexus.util.Log.log;

/**
 * Maintains metadata for all registered entities including their contexts. Contexts are action
 * queues that are guaranteed to be processed in a single-threaded manner.
 */
public class ObjectManager
    implements EventSink
{
    public ObjectManager (NexusConfig config, Executor exec)
    {
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
        if (_objects.remove(object.getId()) == null) {
            log.warning("Requested to clear unknown object", "id", object.getId());
        }
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
        return invoke(requireKeyed(eclass, key, "No singleton registered for"), request);
    }

    // from interface EventSink
    public void postEvent (NexusObject source, NexusEvent event)
    {
        // TODO
    }

    protected void register (NexusObject object, EntityContext ctx)
    {
        int id = getNextObjectId();
        object.init(id, this);
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
}
