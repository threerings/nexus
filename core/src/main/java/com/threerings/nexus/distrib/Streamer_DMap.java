//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import com.threerings.nexus.io.Streamable;
import com.threerings.nexus.io.Streamer;

/**
 * Contains streamers for {@link DMap} inner classes.
 */
public class Streamer_DMap
{
    /** Handles streaming of {@link DMap.PutEvent} instances. */
    public static class PutEvent<K,V> implements Streamer<DMap.PutEvent<K,V>> {
        public Class<?> getObjectClass () {
            return DMap.PutEvent.class;
        }
        public void writeObject (Streamable.Output out, DMap.PutEvent<K,V> obj) {
            out.writeInt(obj.targetId);
            out.writeShort(obj.index);
            out.writeValue(obj._key);
            out.writeValue(obj._value);
        }
        public DMap.PutEvent<K,V> readObject (Streamable.Input in) {
            return new DMap.PutEvent<K,V>(in.readInt(), in.readShort(),
                                          in.<K>readValue(), in.<V>readValue());
        }
    }

    /** Handles streaming of {@link DMap.RemoveEvent} instances. */
    public static class RemoveEvent<K,V> implements Streamer<DMap.RemoveEvent<K,V>> {
        public Class<?> getObjectClass () {
            return DMap.RemoveEvent.class;
        }
        public void writeObject (Streamable.Output out, DMap.RemoveEvent<K,V> obj) {
            out.writeInt(obj.targetId);
            out.writeShort(obj.index);
            out.writeValue(obj._key);
        }
        public DMap.RemoveEvent<K,V> readObject (Streamable.Input in) {
            return new DMap.RemoveEvent<K,V>(in.readInt(), in.readShort(), in.<K>readValue());
        }
    }
}
