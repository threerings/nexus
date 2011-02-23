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
public abstract class DValue<T> extends DAttribute
{
    /** An event emitted when a value changes. */
    public static abstract class ChangedEvent<T> extends DAttribute.Event
    {
        /** Returns the new value of the attribute. */
        public T getValue () {
            return _newValue;
        }

        /** Returns the value of the attribute prior to the change. */
        public T getOldValue () {
            return _oldValue;
        }

        @Override public void applyTo (NexusObject target) {
            @SuppressWarnings("unchecked") DValue<T> attr = (DValue<T>)target.getAttribute(_index);
            attr.applyChanged(this);
        }

        /** Used in lieu of a constructor. */
        protected ChangedEvent<T> init (short index, T newValue, T oldValue) {
            _index = index;
            _newValue = newValue;
            _oldValue = oldValue;
            return this;
        }

        protected T _newValue, _oldValue;
    }

    /** An interface for publishing change events to listeners. */
    public static interface ChangedListener<T> extends Listener
    {
        /** Notifies listener of a value change. */
        void valueChanged (ChangedEvent<T> event);
    }

    /**
     * Creates a value attribute with the supplied initial value.
     */
    public static DValue<Integer> create (int value)
    {
        return new DValue<Integer>(value) {
            @Override public void readObject (Input in) {
                _value = in.readInt();
            }
            @Override public void writeObject (Output out) {
                out.writeInt(_value);
            }
            @Override protected ChangedEvent<Integer> createChangedEvent () {
                return new ChangedEvent<Integer>() {
                    @Override public void readObject (Input in) {
                        super.readObject(in);
                        _newValue = in.readInt();
                        _oldValue = in.readInt();
                    }
                    @Override public void writeObject (Output out) {
                        super.writeObject(out);
                        out.writeInt(_newValue);
                        out.writeInt(_oldValue);
                    }
                };
            }
        };
    }

    /**
     * Creates a value attribute with the supplied initial value.
     */
    public static <T extends Streamable> DValue<T> create (T value)
    {
        return new DValue<T>(value) {
            @Override public void readObject (Input in) {
                _value = in.<T>readStreamable();
            }
            @Override public void writeObject (Output out) {
                out.writeStreamable(_value);
            }
            @Override protected ChangedEvent<T> createChangedEvent () {
                return new ChangedEvent<T>() {
                    @Override public void readObject (Input in) {
                        super.readObject(in);
                        _newValue = in.<T>readStreamable();
                        _oldValue = in.<T>readStreamable();
                    }
                    @Override public void writeObject (Output out) {
                        super.writeObject(out);
                        out.writeStreamable(_newValue);
                        out.writeStreamable(_oldValue);
                    }
                };
            }
        };
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
        _owner.postEvent(createChangedEvent().init(_index, value, ovalue));
        return ovalue;
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

    protected DValue (T value)
    {
        _value = value;
    }

    protected abstract ChangedEvent<T> createChangedEvent ();

    protected void applyChanged (ChangedEvent<T> event)
    {
        _value = event.getValue();
        for (int ii = 0, ll = _changedListeners.length; ii < ll; ii++) {
            @SuppressWarnings("unchecked") ChangedListener<T> listener =
                (ChangedListener<T>)_changedListeners[ii];
            listener.valueChanged(event);
        }
    }

    /** Our registered change listeners. */
    protected Listener[] _changedListeners = NO_LISTENERS;

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

    }
}
