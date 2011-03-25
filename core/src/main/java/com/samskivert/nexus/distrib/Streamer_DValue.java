//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.distrib;

import com.samskivert.nexus.io.Streamable;
import com.samskivert.nexus.io.Streamer;

/**
 * Handles the streaming of {@link DValue} internal classes.
 */
public class Streamer_DValue
{
    /** Handles the streaming of {@link DValue.ChangedEvent} instances. */
    public static class ChangedEvent<T> implements Streamer<DValue.ChangedEvent<T>> {
        public void writeObject (Streamable.Output out, DValue.ChangedEvent<T> obj) {
            out.writeInt(obj._targetId);
            out.writeShort(obj._index);
            out.writeValue(obj._value);
        }
        public DValue.ChangedEvent<T> readObject (Streamable.Input in) {
            return new DValue.ChangedEvent<T>(in.readInt(), in.readShort(), in.<T>readValue());
        }
    }
}
