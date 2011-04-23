//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import com.threerings.nexus.io.Streamable;

import static com.threerings.nexus.util.Log.log;

/**
 * A value attribute for a Nexus object. Contains a single value, which may be updated.
 */
public class DValue<T> extends DAttribute
{
    /** An interface for publishing change events to listeners. */
    public static interface Listener<T> extends DListener
    {
        /** Notifies listener of a value update. */
        void valueChanged (T value, T oldValue);
    }

    /**
     * Creates a new attribute with the specified initial value.
     */
    public static <T> DValue<T> create (T value)
    {
        return new DValue<T>(value);
    }

    /**
     * Returns the current value.
     */
    public T get ()
    {
        return _value;
    }

    /**
     * Updates the current value; notifies {@link Listener}s that the value has changed.
     *
     * @return the value prior to update.
     */
    public T update (T value)
    {
        T ovalue = _value;
        _value = value;
        postChanged(value, ovalue);
        return ovalue;
    }

    /**
     * Adds a listener for value changes.
     */
    public void addListener (Listener<T> listener)
    {
        _listeners = addListener(_listeners, listener);
    }

    /**
     * Removes a listener for value changes.
     */
    public void removeListener (Listener<T> listener)
    {
        removeListener(_listeners, listener);
    }

    @Override // from DAttribute
    public void readContents (Streamable.Input in)
    {
        _value = in.<T>readValue();
    }

    @Override // from DAttribute
    public void writeContents (Streamable.Output out)
    {
        out.writeValue(_value);
    }

    protected DValue (T value)
    {
        _value = value;
    }

    protected void postChanged (T value, T ovalue)
    {
        ChangedEvent<T> event = new ChangedEvent<T>(_owner.getId(), _index, value);
        event.oldValue = ovalue;
        _owner.postEvent(event);
    }

    protected void applyChanged (T value, T oldValue)
    {
        T ovalue = DAttribute.chooseValue(_value, oldValue);
        _value = value;
        for (int ii = 0, ll = _listeners.length; ii < ll; ii++) {
            @SuppressWarnings("unchecked") Listener<T> listener = (Listener<T>)_listeners[ii];
            if (listener != null) {
                try {
                    listener.valueChanged(_value, ovalue);
                } catch (Throwable t) {
                    log.warning("Listener choked in valueChanged", "value", value, "ovalue", oldValue,
                                "listener", listener, t);
                }
            }
        }
    }

    /** An event emitted when a value changes. */
    protected static class ChangedEvent<T> extends DAttribute.Event
    {
        public T oldValue = DAttribute.<T>sentinelValue();

        public ChangedEvent (int targetId, short index, T value) {
            super(targetId, index);
            _value = value;
        }

        @Override public void applyTo (NexusObject target) {
            @SuppressWarnings("unchecked") DValue<T> attr =
                (DValue<T>)target.getAttribute(this.index);
            attr.applyChanged(_value, oldValue);
        }

        @Override protected void toString (StringBuilder buf) {
            super.toString(buf);
            buf.append(", value=").append(_value);
        }

        protected final T _value;
    }

    /** The current value. */
    protected T _value;

    /** Our registered listeners. */
    protected DListener[] _listeners = NO_LISTENERS;
}
