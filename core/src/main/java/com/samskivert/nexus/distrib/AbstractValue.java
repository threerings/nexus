//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.distrib;

/**
 * Contains code shared by {@link DValue}, {@link DIntValue}, etc.
 */
public abstract class AbstractValue<T> extends DAttribute
{
    /** An event emitted when a value changes. */
    public static abstract class ChangedEvent<T> extends DAttribute.Event
    {
        /** Returns the new value of the attribute. */
        public abstract T getValue ();

        /** Returns the value of the attribute prior to the change. */
        public abstract T getOldValue ();
    }

    /** An interface for publishing change events to listeners. */
    public static interface ChangedListener<T> extends Listener
    {
        /** Notifies listener of a value change. */
        void valueChanged (ChangedEvent<T> event);
    }

    /**
     * Adds a listener for {@link ChangedEvent}s.
     */
    public void addChangedListener (ChangedListener<T> listener)
    {
        _changedListeners = addListener(_changedListeners, listener);
    }

    /**
     * Removes a listener for {@link ChangedEvent}s.
     */
    public void removeChangedListener (ChangedListener<T> listener)
    {
        removeListener(_changedListeners, listener);
    }

    protected void notifyListeners (ChangedEvent<T> event)
    {
        for (int ii = 0, ll = _changedListeners.length; ii < ll; ii++) {
            @SuppressWarnings("unchecked") ChangedListener<T> listener =
                (ChangedListener<T>)_changedListeners[ii];
            listener.valueChanged(event);
        }
    }

    /** Our registered change listeners. */
    protected Listener[] _changedListeners = NO_LISTENERS;
}
