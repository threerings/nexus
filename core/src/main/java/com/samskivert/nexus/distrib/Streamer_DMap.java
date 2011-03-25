//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.distrib;

import com.samskivert.nexus.io.Streamable;
import com.samskivert.nexus.io.Streamer;

/**
 * Contains streamers for {@link DMap} inner classes.
 */
public class Streamer_DMap
{
    /** Handles streaming of {@link DMap.PutEvent} instances. */
    public static class PutEvent<K,V> implements Streamer<DMap.PutEvent<K,V>> {
        public void writeObject (Streamable.Output out, DMap.PutEvent<K,V> obj) {
            out.writeInt(obj._targetId);
            out.writeShort(obj._index);
            out.writeValue(obj._key);
            out.writeValue(obj._value);
        }

        public DMap.PutEvent<K,V> readObject (Streamable.Input in) {
            return new DMap.PutEvent<K,V>(in.readInt(), in.readShort(),
                                          in.<K>readValue(), in.<V>readValue());
        }
    }

    /** Handles streaming of {@link DMap.RemovedEvent} instances. */
    public static class RemovedEvent<K,V> implements Streamer<DMap.RemovedEvent<K,V>> {
        public void writeObject (Streamable.Output out, DMap.RemovedEvent<K,V> obj) {
            out.writeInt(obj._targetId);
            out.writeShort(obj._index);
            out.writeValue(obj._key);
        }

        public DMap.RemovedEvent<K,V> readObject (Streamable.Input in) {
            return new DMap.RemovedEvent<K,V>(in.readInt(), in.readShort(), in.<K>readValue());
        }
    }
}
