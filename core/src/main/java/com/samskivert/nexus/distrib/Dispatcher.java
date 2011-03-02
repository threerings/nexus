//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.distrib;

/**
 * Handles the dispatch of events.
 */
public interface Dispatcher
{
    /**
     * Locates the object that is the target of the supplied evenet, and the appropriate executor
     * for said object, and dispatches the event on the object.
     */
    void dispatchEvent (NexusEvent event);
}
