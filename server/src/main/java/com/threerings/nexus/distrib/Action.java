//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

/**
 * An action invoked in the context of a Nexus entity. The sender does not block awaiting a
 * response.
 */
public interface Action<E>
{
    /**
     * Called with the requested entity on the thread appropriate for said entity.
     */
    void invoke (E entity);
}
