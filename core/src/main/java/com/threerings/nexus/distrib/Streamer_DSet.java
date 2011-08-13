//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import com.threerings.nexus.io.Streamable;
import com.threerings.nexus.io.Streamer;

/**
 * Contains streamers for {@link DSet} inner classes.
 */
public class Streamer_DSet
{
    /** Handles streaming of {@link DSet.AddEvent} instances. */
    public static class AddEvent<E> implements Streamer<DSet.AddEvent<E>> {
        public Class<?> getObjectClass () {
            return DSet.AddEvent.class;
        }
        public void writeObject (Streamable.Output out, DSet.AddEvent<E> obj) {
            out.writeInt(obj.targetId);
            out.writeShort(obj.index);
            out.writeValue(obj._elem);
        }
        public DSet.AddEvent<E> readObject (Streamable.Input in) {
            return new DSet.AddEvent<E>(in.readInt(), in.readShort(), in.<E>readValue());
        }
    }

    /** Handles streaming of {@link DSet.RemoveEvent} instances. */
    public static class RemoveEvent<E> implements Streamer<DSet.RemoveEvent<E>> {
        public Class<?> getObjectClass () {
            return DSet.RemoveEvent.class;
        }
        public void writeObject (Streamable.Output out, DSet.RemoveEvent<E> obj) {
            out.writeInt(obj.targetId);
            out.writeShort(obj.index);
            out.writeValue(obj._elem);
        }
        public DSet.RemoveEvent<E> readObject (Streamable.Input in) {
            return new DSet.RemoveEvent<E>(in.readInt(), in.readShort(), in.<E>readValue());
        }
    }
}
