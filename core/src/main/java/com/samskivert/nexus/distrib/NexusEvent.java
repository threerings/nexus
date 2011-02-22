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
    /**
     * Applies this event to the supplied target object.
     */
    public abstract void applyTo (NexusObject object);

    /**
     * Returns the id of the Nexus object on which this event was generated.
     */
    public int getTargetId ()
    {
        return _targetId;
    }

    // from interface Streamable
    public void readObject (Input in)
    {
        _targetId = in.readInt();
    }

    // from interface Streamable
    public void writeObject (Output out)
    {
        out.writeInt(_targetId);
    }

    /**
     * Configures this event with its target object id. This is called when the event is posted to
     * the object and before it is forwarded to the object's event sink.
     */
    protected void setTargetId (int targetId)
    {
        _targetId = targetId;
    }

    protected int _targetId;
}
