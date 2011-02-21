//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.distrib;

/**
 * The basis for all distributed information sharing in Nexus.
 */
public class NexusObject
{
    /** The unique identifier for this object. This value is not available until the object has
     * been registered with the Nexus Manager. The id is unique with respect to the peer on which
     * this object was created and registered. */
    protected int _id;

    /** Handles the dispatch of events on this object. */
    protected transient EventSink _sink;
}
