//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.distrib;

import com.samskivert.nexus.io.Streamable;

/**
 * Contains information describing a change to a {@link NexusObject}.
 */
public abstract class NexusEvent
    implements Streamable
{
    /** The id of the Nexus object on which this event was generated. */
    public final int targetId;

    /**
     * Applies this event to the supplied target object.
     */
    public abstract void applyTo (NexusObject object);

    @Override
    public String toString ()
    {
        String cname = getClass().getName();
        cname = cname.substring(cname.lastIndexOf(".")+1);
        StringBuilder buf = new StringBuilder(cname).append("[");
        toString(buf);
        return buf.append("]").toString();
    }

    /**
     * Creates an event targeted to the specified object.
     */
    protected NexusEvent (int targetId)
    {
        this.targetId = targetId;
    }

    protected void toString (StringBuilder buf)
    {
        buf.append("target=").append(targetId);
    }
}
