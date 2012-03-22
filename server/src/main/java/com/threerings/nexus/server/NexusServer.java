//
// $Id$
//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.server;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;

import com.google.common.base.Preconditions;

import react.Slot;

import com.threerings.nexus.distrib.Action;
import com.threerings.nexus.distrib.Keyed;
import com.threerings.nexus.distrib.Nexus;
import com.threerings.nexus.distrib.NexusObject;
import com.threerings.nexus.distrib.Request;
import com.threerings.nexus.distrib.Singleton;

/**
 * Implements the Nexus services and coordinates communication between nodes.
 */
public class NexusServer implements Nexus
{
    /**
     * Creates a server with the supplied configuration.
     *
     * @param exec the executor to use for dispatching events and actions. An executor that uses a
     * pool of threads is appropriate.
     */
    public NexusServer (NexusConfig config, ExecutorService exec) {
        _config = config;
        _omgr = new ObjectManager(config, exec);
        _smgr = new SessionManager(_omgr);
    }

    /**
     * Returns the session manager used by this server.
     */
    public SessionManager getSessionManager () {
        return _smgr;
    }

    /**
     * Shuts down this server and cleans up any resources it is using.
     */
    public void shutdown () {
        _timer.cancel();
    }

    // from interface Nexus
    public void register (NexusObject object) {
        _omgr.register(object);
    }

    // from interface Nexus
    public void register (NexusObject child, Singleton parent) {
        _omgr.register(child, parent);
    }

    // from interface Nexus
    public void register (NexusObject child, Keyed parent) {
        _omgr.register(child, parent);
    }

    // from interface Nexus
    public void registerSingleton (Singleton entity) {
        _omgr.registerSingleton(entity);
    }

    // from interface Nexus
    public void registerSingleton (Singleton child, Singleton parent) {
        _omgr.registerSingleton(child, parent);
    }

    // from interface Nexus
    public void registerKeyed (Keyed entity) {
        _omgr.registerKeyed(entity);
    }

    // from interface Nexus
    public void registerKeyed (Keyed child, Keyed parent) {
        _omgr.registerKeyed(child, parent);
    }

    // from interface Nexus
    public void clear (NexusObject object) {
        _omgr.clear(object);
    }

    // from interface Nexus
    public void clearSingleton (Singleton entity) {
        _omgr.clearSingleton(entity);
    }

    // from interface Nexus
    public void clearKeyed (Keyed entity) {
        _omgr.clearKeyed(entity);
    }

    // from interface Nexus
    public <E, T extends Singleton> Slot<E> routed (T entity, final Slot<E> slot) {
        // javac isn't smart enough to know that all the T's are the same here
        @SuppressWarnings("unchecked") final Class<T> eclass = (Class<T>)entity.getClass();
        return new Slot<E>() {
            @Override public void onEmit (final E event) {
                invoke(eclass, new Action<T>() {
                    public void invoke (T entity) {
                        slot.onEmit(event);
                    }
                });
            }
        };
    }

    // from interface Nexus
    public <E, T extends Keyed> Slot<E> routed (T entity, final Slot<E> slot) {
        // javac isn't smart enough to know that all the T's are the same here
        @SuppressWarnings("unchecked") final Class<T> eclass = (Class<T>)entity.getClass();
        final Comparable<?> key = entity.getKey();
        return new Slot<E>() {
            @Override public void onEmit (final E event) {
                invoke(eclass, key, new Action<T>() {
                    public void invoke (T entity) {
                        slot.onEmit(event);
                    }
                });
            }
        };
    }

    // from interface Nexus
    public <T extends Singleton> void invoke (Class<T> eclass, Action<T> action) {
        _omgr.invoke(eclass, action);
    }

    // from interface Nexus
    public <T extends Keyed> void invoke (Class<T> kclass, Comparable<?> key, Action<T> action) {
        // TODO: determine whether the entity is local or remote
        _omgr.invoke(kclass, key, action);
    }

    // from interface Nexus
    public <T extends Singleton,R> R invoke (Class<T> eclass, Request<T,R> request) {
        return _omgr.invoke(eclass, request);
    }

    // from interface Nexus
    public <T extends Keyed,R> R invoke (Class<T> kclass, Comparable<?> key, Request<T,R> request) {
        // TODO: determine whether the entity is local or remote
        return _omgr.invoke(kclass, key, request);
    }

    // from interface Nexus
    public <T extends Singleton> Deferred invokeAfter (
        final Class<T> eclass, long delay, final Action<T> action) {
        return schedule(new Runnable() {
            public void run () {
                invoke(eclass, action);
            }
        }, delay);
    }

    // from interface Nexus
    public <T extends Keyed> Deferred invokeAfter (
        final Class<T> eclass, final Comparable<?> key, long delay, final Action<T> action) {
        return schedule(new Runnable() {
            public void run () {
                invoke(eclass, key, action);
            }
        }, delay);
    }

    protected Deferred schedule (final Runnable action, final long delay) {
        return new Deferred() {
            public TimerTask task = createTask();
            /*ctor*/ {
                _timer.schedule(task, delay);
            }
            @Override public void cancel () {
                Preconditions.checkState(task != null, "Deferred action already canceled.");
                task.cancel();
                task = null;
            }
            @Override public Nexus.Deferred repeatEvery (long period) {
                Preconditions.checkState(task != null, "Deferred action has been canceled.");
                task.cancel();
                task = createTask();
                _timer.schedule(task, delay, period);
                return this;
            }
            private TimerTask createTask () {
                return new TimerTask() {
                    @Override public void run () {
                        action.run();
                    }
                };
            }
        };
    }

    protected final NexusConfig _config;
    protected final ObjectManager _omgr;
    protected final SessionManager _smgr;

    /** The daemon timer used to schedule all intervals. */
    protected final Timer _timer = new Timer("Nexus Deferred Action Timer");
}
