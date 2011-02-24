//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.distrib;

import com.samskivert.nexus.io.Streamable;

/**
 * Handles the streaming of {@link DValue} instances and its internal events.
 */
public class Streamer_DValue
{
    public <T> void writeObject (Streamable.Output out, DValue<T> obj)
    {
        out.writeValue(obj._value);
    }

    public <T> DValue<T> readObject (Streamable.Input in)
    {
        return new DValue<T>(in.<T>readValue());
    }

    public static class ChangedEvent {
        public <T> void writeObject (Streamable.Output out, DValue.ChangedEvent<T> obj)
        {
            out.writeShort(obj._index);
            out.writeValue(obj._value);
        }

        public <T> DValue.ChangedEvent<T> readObject (Streamable.Input in)
        {
            return new DValue.ChangedEvent<T>(in.readShort(), in.<T>readValue());
        }
    }
}
