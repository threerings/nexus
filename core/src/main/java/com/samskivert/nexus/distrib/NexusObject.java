//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.distrib;

import com.samskivert.nexus.io.Streamable;

/**
 * The basis for all distributed information sharing in Nexus.
 */
public abstract class NexusObject
    implements Streamable
{
    /**
     * An interface implemented by Nexus object attributes, so that they may be supplied with a
     * reference to their owning object. This takes place when the owning object is registered with
     * the Nexus system, or when it is unstreamed from the network.
     */
    public static interface Attribute
    {
        /** Configures this attribute with its owning object reference and index. */
        void init (NexusObject owner, short index);
    }

    /**
     * Initializes this object with its event sink, which also triggers the initialization of its
     * distributed attributes. This takes place when the object is registered with dispatcher on
     * its originating server, and when it is read off the network on a subscribing client.
     */
    public void init (EventSink sink)
    {
        _sink = sink;
        for (int ii = 0, ll = getAttributeCount(); ii < ll; ii++) {
            getAttribute(ii).init(this, (short)ii);
        }
    }

    /**
     * Returns the distributed attribute at the specified index.
     *
     * @exception IndexOutOfBoundsException if an attribute at illegal index is requested.
     */
    protected DAttribute getAttribute (int index)
    {
        throw new IndexOutOfBoundsException("Invalid attribute index " + index);
    }

    /**
     * Returns the number of attributes owned by this object. Values from 0 to {@link
     * #getAttributeCount}-1 may be legally passed to {@link #getAttribute}.
     */
    protected int getAttributeCount ()
    {
        return 0;
    }

    /**
     * Requests that the supplied event be posted to this object.
     */
    protected void postEvent (NexusEvent event)
    {
        event.setTargetId(_id);
        _sink.postEvent(this, event);
    }

    /** The unique identifier for this object. This value is not available until the object has
     * been registered with the Nexus Manager. The id is unique with respect to the peer on which
     * this object was created and registered. */
    protected int _id;

    /** Handles the dispatch of events on this object. */
    protected EventSink _sink;
}
