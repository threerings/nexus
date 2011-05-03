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
    /** Handles the streaming of {@link DValue.ChangedEvent} instances. */
    public static class ChangedEvent<T> implements Streamer<DValue.ChangedEvent<T>> {
        public Class<?> getObjectClass () {
            return DValue.ChangedEvent.class;
        }
        public void writeObject (Streamable.Output out, DValue.ChangedEvent<T> obj) {
            out.writeInt(obj.targetId);
            out.writeShort(obj.index);
            out.writeValue(obj._value);
        }
        public DValue.ChangedEvent<T> readObject (Streamable.Input in) {
            return new DValue.ChangedEvent<T>(in.readInt(), in.readShort(), in.<T>readValue());
        }
    }
}
