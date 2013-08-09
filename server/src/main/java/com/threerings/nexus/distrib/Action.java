//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import static com.threerings.nexus.util.Log.log;

/**
 * An action invoked in the context of a Nexus entity. The sender does not block awaiting a
 * response. TODO: this needs to become an interface with a default method when the time comes to
 * support Java 8 lambdas.
 */
// TODO: @FunctionalInterface
public abstract class Action<E>
{
    /** A variant of {@link Action} that can be used when you know that the action will not cross
     * server boundaries. This allows things to be referenced from the enclosing scope <em>when
     * that is known to be thread safe</em>. Remember that actions likely execute in a different
     * thread context than the one from which they were initiated, so one must be careful not to
     * use things from an enclosing scope that will cause race conditions. */
    public static abstract class Local<E> extends Action<E> {
    }

    /**
     * Called with the requested entity on the thread appropriate for said entity.
     */
    public abstract void invoke (E entity);

    /**
     * Called if the action could not be processed due to the target entity being not found. Note:
     * this only occurs for keyed entities, not singleton entities. Note also that this method will
     * be called in no execution context, which means that one must either perform only thread-safe
     * operations in the body of this method, or one must dispatch a new action to process the
     * failure. The default implementation simply logs a warning.
     *
     * @param nexus a reference to the nexus for convenient dispatch of a new action.
     * @param eclass the class of the entity that could not be found.
     * @param key the key of the entity that could not be found.
     */
    public void onDropped (Nexus nexus, Class<?> eclass, Comparable<?> key) {
        log.warning("Dropping action on unknown entity", "eclass", eclass.getName(), "key", key,
                    "action", this);
    }
}
