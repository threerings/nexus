//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.distrib;

import com.samskivert.nexus.io.Streamable;

/**
 * A value attribute for a Nexus object. Contains a single value, which may be updated.
 */
public class DValue<T extends Streamable> extends DAttribute
{
    /** An event emitted when a value changes. */
    public static abstract class ChangedEvent<T> extends NexusEvent
    {
        /** Returns the new value of the attribute. */
        public abstract T getValue ();

        /** Returns the value of the attribute prior to the change. */
        public abstract T getOldValue ();
    }

    /**
     * Creates a value attribute with the supplied initial value.
     */
    public static <T extends Streamable> DValue<T> create (T value)
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
     * Updates the current value. Emits a {@link ChangedEvent} to communicate the value change to
     * listeners.
     *
     * @return the value prior to update.
     */
    public T update (T value)
    {
        T ovalue = _value;
        _value = value;
        // TODO: generate ValueChangedEvent
        return ovalue;
    }

    // from interface Streamable
    public void readObject (Input in)
    {
        _value = in.<T>readStreamable();
    }

    // from interface Streamable
    public void writeObject (Output out)
    {
        out.writeStreamable(_value);
    }

    protected DValue (T value)
    {
        _value = value;
    }

    /** The current value. */
    protected T _value;

    /** A change event that operates on {@link Streamable} values. */
    protected static class StreamableChangedEvent<T extends Streamable> extends ChangedEvent<T>
    {
        public StreamableChangedEvent (T newValue, T oldValue) {
            _newValue = newValue;
            _oldValue = oldValue;
        }

        @Override // from ChangedEvent<T>
        public T getValue () {
            return _newValue;
        }

        @Override // from ChangedEvent<T>
        public T getOldValue () {
            return _oldValue;
        }

        // from interface Streamable
        public void readObject (Input in) {
            _newValue = in.<T>readStreamable();
            _oldValue = in.<T>readStreamable();
        }

        // from interface Streamable
        public void writeObject (Output out) {
            out.writeStreamable(_newValue);
            out.writeStreamable(_oldValue);
        }

        protected T _newValue, _oldValue;
    }
}
