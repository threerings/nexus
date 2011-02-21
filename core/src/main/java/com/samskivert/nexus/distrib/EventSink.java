//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.distrib;

/**
 * Handles the dispatch of events for a {@link NexusObject}.
 */
public interface EventSink
{
    /**
     * Posts an event originating from the specified object, to be dispatched to local and
     * distributed listeners.
     */
    void postEvent (NexusObject source, NexusEvent event);
}
