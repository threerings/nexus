//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.server;

import java.util.Timer;
import java.util.TimerTask;

import com.google.common.base.Preconditions;

import com.threerings.nexus.distrib.Action;
import com.threerings.nexus.distrib.Keyed;
import com.threerings.nexus.distrib.Nexus;
import com.threerings.nexus.distrib.Singleton;

/**
 * Handles the invocation of actions on entities after a delay or periodically. This uses a Java
 * {@link Timer} thread under the hood, and thus must be {@link #shutdown} when no longer needed.
 */
public class NexusTimer
{
    /** A handle used to control deferred actions. See {@link #invokeAfter}. */
    public interface Deferred {
        /** Cancels this deferred action. This is mainly intended for stopping repeating actions.
         * <p> The caller should not assume that a canceled action will not subsequently be
         * executed at least once. It is possible that the cancellation occurs simultaneously with
         * the action being queued for execution on its entity, at which point the action cannot be
         * stopped. Thus any deferred action should contain code that confirm that its
         * preconditions still hold. </p>
         * @throws IllegalStateException if this action has already been canceled. */
        void cancel ();

        /** Causes this deferred action to repeat every {@code period} millis after its first
         * invocation, until canceled. This must be called immediately after {@link #invokeAfter}.
         * @return a reference to this instance for convenient chaining.
         * @throws IllegalStateException if this action has been canceled. */
        Deferred repeatEvery (long period);
    }

    /**
     * Creates a timer that will work with the supplied Nexus.
     */
    public NexusTimer (Nexus nexus) {
        _nexus = nexus;
    }

    /**
     * Shuts down this server and cleans up any resources it is using.
     */
    public void shutdown () {
        _timer.cancel();
    }

    /**
     * Executes an action in the context of the specified singleton entity (either object or
     * non-object entity). The action is executed after the specified delay, unless canceled prior.
     *
     * @throws EntityNotFoundException if no singleton instance is registered for {@code eclass}
     */
    public <T extends Singleton> Deferred invokeAfter (final Class<T> eclass, long delay,
                                                       final Action<? super T> action) {
        return schedule(new Runnable() {
            public void run () {
                _nexus.invoke(eclass, action);
            }
        }, delay);
    }

    /**
     * Executes an action in the context (server+thread) of the specified keyed (object or
     * non-object) entity. The action is executed after the specified delay, unless canceled prior.
     * The action may be streamed to another server node if the context for the specified keyed
     * entity is hosted outside the local server node.
     */
    public <T extends Keyed> Deferred invokeAfter (final Class<T> eclass, final Comparable<?> key,
                                                   long delay, final Action<? super T> action) {
        return schedule(new Runnable() {
            public void run () {
                _nexus.invoke(eclass, key, action);
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
            @Override public Deferred repeatEvery (long period) {
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

    /** The nexus on which we invoke actions. */
    protected final Nexus _nexus;

    /** The daemon timer used to schedule all intervals. */
    protected final Timer _timer = new Timer("Nexus Deferred Action Timer");
}
