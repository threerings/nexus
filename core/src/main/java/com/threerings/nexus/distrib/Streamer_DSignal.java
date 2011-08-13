//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import com.threerings.nexus.io.Streamable;
import com.threerings.nexus.io.Streamer;

/**
 * Contains streamers for {@link DSignal} inner classes.
 */
public class Streamer_DSignal
{
    /** Handles streaming of {@link DSignal.EmitEvent} instances. */
    public static class EmitEvent<T> implements Streamer<DSignal.EmitEvent<T>> {
        public Class<?> getObjectClass () {
            return DSignal.EmitEvent.class;
        }
        public void writeObject (Streamable.Output out, DSignal.EmitEvent<T> obj) {
            out.writeInt(obj.targetId);
            out.writeShort(obj.index);
            out.writeValue(obj._event);
        }
        public DSignal.EmitEvent<T> readObject (Streamable.Input in) {
            return new DSignal.EmitEvent<T>(in.readInt(), in.readShort(), in.<T>readValue());
        }
    }
}
