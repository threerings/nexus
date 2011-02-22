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
public class DValue<T extends Streamable> extends AbstractValue<T>
{
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
        _owner.postEvent(new StreamableChangedEvent<T>().init(_index, value, ovalue));
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

    protected void applyChanged (StreamableChangedEvent<T> event)
    {
        _value = event.getValue();
        notifyListeners(event);
    }

    /** The current value. */
    protected T _value;

    /** Notifies listeners of a change of a {@link Streamable} value. */
    protected static class StreamableChangedEvent<T extends Streamable> extends ChangedEvent<T>
    {
        @Override public T getValue () {
            return _newValue;
        }

        @Override public T getOldValue () {
            return _oldValue;
        }

        @Override public void applyTo (NexusObject target) {
            @SuppressWarnings("unchecked") DValue<T> attr = (DValue<T>)target.getAttribute(_index);
            attr.applyChanged(this);
        }

        @Override public void readObject (Input in) {
            _newValue = in.<T>readStreamable();
            _oldValue = in.<T>readStreamable();
        }

        @Override public void writeObject (Output out) {
            out.writeStreamable(_newValue);
            out.writeStreamable(_oldValue);
        }

        /** Used in lieu of a constructor. */
        protected StreamableChangedEvent<T> init (short index, T newValue, T oldValue) {
            _index = index;
            _newValue = newValue;
            _oldValue = oldValue;
            return this;
        }

        protected T _newValue, _oldValue;
    }
}
