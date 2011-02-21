//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.distrib;

import java.io.IOException;

import com.samskivert.nexus.io.Streamable;

/**
 * A value attribute for a Nexus object. Contains a single value, which may be updated.
 */
public class DValue<T extends Streamable>
    implements Streamable
{
    /**
     * Returns the current value.
     */
    public T get ()
    {
        return _value;
    }

    /**
     * Updates the current value. Emits a {@link ValueChangedEvent} to communicate the value change
     * to listeners.
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
    public void readObject (Input in) throws IOException
    {
        @SuppressWarnings("unchecked") T value = (T)in.readStreamable();
        _value = value;
    }

    // from interface Streamable
    public void writeObject (Output out) throws IOException
    {
        out.writeStreamable(_value);
    }

    protected T _value;
}
