//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

/**
 * An RPC request invoked in the context of a Nexus entity. The sender is blocked until the request
 * is processed, and the return value can be delivered to the sender.
 */
public abstract class Request<E,R>
{
    /**
     * Called with the requested entity on the server and thread appropriate for said entity.
     */
    public abstract R invoke (E entity);
}
