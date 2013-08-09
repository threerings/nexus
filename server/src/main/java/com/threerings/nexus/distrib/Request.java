//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

/**
 * An RPC request invoked in the context of a Nexus entity. The sender is blocked until the request
 * is processed, and the return value can be delivered to the sender.
 */
// TODO: @FunctionalInterface
public interface Request<E,R>
{
    /** A variant of {@link Request} that can be used when you know that the action will not cross
     * server boundaries. This allows things to be referenced from the enclosing scope <em>when
     * that is known to be thread safe</em>. Remember that requests likely execute in a different
     * thread context than the one from which they were initiated, so one must be careful not to
     * use things from an enclosing scope that will cause race conditions. */
    static abstract class Local<E,R> implements Request<E,R> {
    }

    /**
     * Called with the requested entity on the server and thread appropriate for said entity.
     */
    R invoke (E entity);
}
