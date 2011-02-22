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
public class DIntValue extends DAttribute
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
        // TODO: generate ValueChangedEvent
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

    protected int _value;
}
