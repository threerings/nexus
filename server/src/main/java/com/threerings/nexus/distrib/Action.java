//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import static com.threerings.nexus.util.Log.log;

/**
 * An action invoked in the context of a Nexus entity. The sender does not block awaiting a
 * response.
 */
public abstract class Action<E>
{
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