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
    public interface DListener {
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
    protected static DListener[] addListener (DListener[] listeners, DListener listener)
    {
        // scan backwards from the end of the array finding the left-most null slot
        int idx = listeners.length;
        while (idx > 0 && listeners[idx-1] == null) idx--;

        // if we found no null slots, expand the array
        if (idx == listeners.length) {
            DListener[] nlist = new DListener[listeners.length+LISTENER_EXPAND_DELTA];
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
    protected static void removeListener (DListener[] listeners, DListener listener)
    {
        for (int ii = 0, ll = listeners.length; ii < ll; ii++) {
            if (listeners[ii] == listener) {
                System.arraycopy(listeners, ii+1, listeners, ii, listeners.length-ii-1);
                listeners[listeners.length-1] = null;
                return;
            }
        }
    }

    /**
     * Returns the event value, unless it is (reference) equal to the sentinel value, in which case
     * it returns the local value. This is used to support the reporting of "previous" values for
     * value, set and map values, thusly:
     *
     * <p> On the authoritative server (the only place where object changes may be made), a value
     * change results in immediate application of the updated value. The previous value is stored
     * in a transient field in the ChangedEvent, and when the event is later processed, the
     * listeners will be notified using the previous value as stored in the event (i.e. chooseValue
     * returns eventValue).
     *
     * <p> On all nodes (clients or other servers) that are subscribed to the object in question,
     * the ChangedEvent will have come from over the network. In that case, the transient
     * "previous" value in the ChangedEvent will be equal to the sentinel value. When the event is
     * applied, the previous value will be extracted from the attribute in question prior to
     * applying the new value from the event. Because this copy of the attribute was *not* updated
     * immediately (as it was on the authoritative server), the local value will be the correct
     * previous value and can be used as such. In that case, chooseValue returns localValue.
     */
    protected static <T> T chooseValue (T localValue, T eventValue)
    {
        return eventValue == SENTINEL_VALUE ? localValue : eventValue;
    }

    /**
     * Returns a sentinel value for use by events in tracking unset values. See {@link
     * #chooseValue}.
     */
    protected static <T> T sentinelValue ()
    {
        @SuppressWarnings("unchecked") T value = (T)SENTINEL_VALUE;
        return value;
    }

    /** A base class for all events associated with an attribute. */
    protected static abstract class Event extends NexusEvent
    {
        protected Event (short index) {
            _index = index;
        }
        protected final short _index;
    }

    /** The object that owns this attribute. */
    protected transient NexusObject _owner;

    /** The index of this attribute in its containing object. */
    protected transient short _index;

    /** Used by our subclasses as a sentinel. */
    protected static final DListener[] NO_LISTENERS = new DListener[0];

    /** The number of new slots we add when expanding a listener array. */
    protected static final int LISTENER_EXPAND_DELTA = 3;

    /** Used by {@link #sentinelValue}. */
    protected static final Object SENTINEL_VALUE = new Object();
}
