//
// $Id$

package com.samskivert.nexus.distrib;

/**
 * An action invoked in the context of a Nexus entity. The sender does not block awaiting a response.
 */
public interface Action<E>
{
    /**
     * Called with the requested entity on the thread appropriate for said entity.
     */
    void invoke (E entity);
}
