//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.distrib;

import java.io.IOException;

import com.samskivert.nexus.io.Streamable;

/**
 * An int value attribute for a Nexus object. Contains a single int value, which may be updated.
 */
public class DIntValue
    implements Streamable
{
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
    public void readObject (Input in) throws IOException
    {
        _value = in.readInt();
    }

    // from interface Streamable
    public void writeObject (Output out) throws IOException
    {
        out.writeInt(_value);
    }

    protected int _value;
}
