//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.distrib;

import com.samskivert.nexus.io.Streamable;

/**
 * The base type for all Nexus object attributes.
 */
public abstract class DAttribute
    implements Streamable, NexusObject.Attribute
{
    /** A marker interface from which all attribute listeners must extend. */
    public interface Listener {
    }

    // from interface NexusObject.Attribute
    public void init (NexusObject owner, short index)
    {
        _owner = owner;
        _index = index;
    }

    /**
     * Adds the supplied listener to the supplied listener list.
     */
    protected Listener[] addListener (Listener[] listeners, Listener listener)
    {
        int idx = listeners.length;
        while (listeners[idx-1] == null) idx--;
        if (idx == listeners.length) {
            Listener[] nlist = new Listener[listeners.length+1];
            System.arraycopy(listeners, 0, nlist, 0, listeners.length);
            nlist[listeners.length] = listener;
            return nlist;
        }

        listeners[idx] = listener;
        return listeners;
    }

    /**
     * Removes the supplied listener from the supplied listener list (nulling out its reference).
     * The remaining listeners are shifted left to close the gap. Note that the listener is located
     * using reference equality.
     */
    protected void removeListener (Listener[] listeners, Listener listener)
    {
        for (int ii = 0, ll = listeners.length; ii < ll; ii++) {
            if (listeners[ii] == listener) {
                listeners[ii] = null;
                System.arraycopy(listeners, ii, listeners, ii+1, listeners.length-ii-1);
                return;
            }
        }
    }

    /** The object that owns this attribute. */
    protected NexusObject _owner;

    /** The index of this attribute in its containing object. */
    protected short _index;

    /** A base class for all events associated with an attribute. */
    protected static abstract class Event extends NexusEvent
    {
        @Override public void readObject (Input in) {
            super.readObject(in);
            _index = in.readShort();
        }

        @Override public void writeObject (Output out) {
            super.writeObject(out);
            out.writeShort(_index);
        }

        protected short _index;
    }

    /** Used by our subclasses as a sentinel. */
    protected static final Listener[] NO_LISTENERS = new Listener[0];
}
