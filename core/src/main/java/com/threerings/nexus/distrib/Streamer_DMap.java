//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import com.threerings.nexus.io.Streamable;
import com.threerings.nexus.io.Streamer;

/**
 * Handles the streaming of {@link DMap} and/or nested classes.
 */
public class Streamer_DMap
{
    /**
     * Handles the streaming of {@link DMap.PutEvent} instances.
     */
    public static class PutEvent<K,V>
        implements Streamer<DMap.PutEvent<K,V>>
    {
        @Override
        public Class<?> getObjectClass () {
            return DMap.PutEvent.class;
        }

        @Override
        public void writeObject (Streamable.Output out, DMap.PutEvent<K,V> obj) {
            writeObjectImpl(out, obj);
        }

        @Override
        public DMap.PutEvent<K,V> readObject (Streamable.Input in) {
            return new DMap.PutEvent<K,V>(
                in.readInt(),
                in.readShort(),
                in.<K>readValue(),
                in.<V>readValue()
            );
        }

        public static <K,V> void writeObjectImpl (Streamable.Output out, DMap.PutEvent<K,V> obj) {
            Streamer_DAttribute.Event.writeObjectImpl(out, obj);
            out.writeValue(obj._key);
            out.writeValue(obj._value);
        }
    }

    /**
     * Handles the streaming of {@link DMap.RemoveEvent} instances.
     */
    public static class RemoveEvent<K,V>
        implements Streamer<DMap.RemoveEvent<K,V>>
    {
        @Override
        public Class<?> getObjectClass () {
            return DMap.RemoveEvent.class;
        }

        @Override
        public void writeObject (Streamable.Output out, DMap.RemoveEvent<K,V> obj) {
            writeObjectImpl(out, obj);
        }

        @Override
        public DMap.RemoveEvent<K,V> readObject (Streamable.Input in) {
            return new DMap.RemoveEvent<K,V>(
                in.readInt(),
                in.readShort(),
                in.<K>readValue()
            );
        }

        public static <K,V> void writeObjectImpl (Streamable.Output out, DMap.RemoveEvent<K,V> obj) {
            Streamer_DAttribute.Event.writeObjectImpl(out, obj);
            out.writeValue(obj._key);
        }
    }

    // no streamer for non-Streamable enclosing class: DMap
}
