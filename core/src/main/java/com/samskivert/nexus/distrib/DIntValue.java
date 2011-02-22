//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.distrib;

import com.samskivert.nexus.io.Streamable;

/**
 * An int value attribute for a Nexus object. Contains a single int value, which may be updated.
 */
public class DIntValue extends AbstractValue<Integer>
{
    /**
     * Creates a value attribute with the supplied initial integer value.
     */
    public static DIntValue create (int value)
    {
        return new DIntValue(value);
    }

    /**
     * Returns the current value.
     */
    public int get ()
    {
        return _value;
    }

    /**
     * Updates the current value. Emits a {@link ValueChangedEvent} to communicate the value change
     * to listeners.
     *
     * @return the value prior to update.
     */
    public int update (int value)
    {
        int ovalue = _value;
        _value = value;
        _owner.postEvent(new IntChangedEvent().init(_index, value, ovalue));
        return ovalue;
    }

    // from interface Streamable
    public void readObject (Input in)
    {
        _value = in.readInt();
    }

    // from interface Streamable
    public void writeObject (Output out)
    {
        out.writeInt(_value);
    }

    protected DIntValue (int value)
    {
        _value = value;
    }

    protected void applyChanged (IntChangedEvent event)
    {
        _value = event._newValue;
        notifyListeners(event);
    }

    protected int _value;

    /** Notifies listeners of a change of an int value. */
    protected static class IntChangedEvent extends DValue.ChangedEvent<Integer>
    {
        @Override public Integer getValue () {
            return _newValue;
        }

        @Override public Integer getOldValue () {
            return _oldValue;
        }

        @Override public void applyTo (NexusObject target) {
            ((DIntValue)target.getAttribute(_index)).applyChanged(this);
        }

        @Override public void readObject (Input in) {
            _newValue = in.readInt();
            _oldValue = in.readInt();
        }

        @Override public void writeObject (Output out) {
            out.writeInt(_newValue);
            out.writeInt(_oldValue);
        }

        /** Used in lieu of a constructor. */
        protected IntChangedEvent init (short index, int newValue, int oldValue) {
            _index = index;
            _newValue = newValue;
            _oldValue = oldValue;
            return this;
        }

        protected int _newValue, _oldValue;
    }
}
