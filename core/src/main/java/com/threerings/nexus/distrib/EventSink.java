//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

/**
 * Handles the dispatch of events for a {@link NexusObject}.
 */
public interface EventSink
{
    /**
     * Returns the name of the host that is associated with this event sink. This is used by Nexus
     * objects to construct their address.
     */
    String getHost ();

    /**
     * Posts an event originating from the specified object, to be dispatched to local and
     * distributed listeners.
     */
    void postEvent (NexusObject source, NexusEvent event);

    /**
     * Posts a service request originating from the specified object. It will be distributed
     * upstream.
     */
    void postCall (NexusObject source, short attrIndex, short methodId, Object[] args);
}
