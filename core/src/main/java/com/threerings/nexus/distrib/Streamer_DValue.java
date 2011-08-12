//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import com.threerings.nexus.io.Streamable;
import com.threerings.nexus.io.Streamer;

/**
 * Handles the streaming of {@link DValue} internal classes.
 */
public class Streamer_DValue
{
    /** Handles the streaming of {@link DValue.ChangeEvent} instances. */
    public static class ChangeEvent<T> implements Streamer<DValue.ChangeEvent<T>> {
        public Class<?> getObjectClass () {
            return DValue.ChangeEvent.class;
        }
        public void writeObject (Streamable.Output out, DValue.ChangeEvent<T> obj) {
            out.writeInt(obj.targetId);
            out.writeShort(obj.index);
            out.writeValue(obj._value);
        }
        public DValue.ChangeEvent<T> readObject (Streamable.Input in) {
            return new DValue.ChangeEvent<T>(in.readInt(), in.readShort(), in.<T>readValue());
        }
    }
}
