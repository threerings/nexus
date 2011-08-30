//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import com.threerings.nexus.io.Streamable;
import com.threerings.nexus.io.Streamer;

/**
 * Handles the streaming of {@link DSet} and/or nested classes.
 */
public class Streamer_DSet
{
    /**
     * Handles the streaming of {@link DSet.AddEvent} instances.
     */
    public static class AddEvent<T>
        implements Streamer<DSet.AddEvent<T>>
    {
        @Override
        public Class<?> getObjectClass () {
            return DSet.AddEvent.class;
        }

        @Override
        public void writeObject (Streamable.Output out, DSet.AddEvent<T> obj) {
            writeObjectImpl(out, obj);
        }

        @Override
        public DSet.AddEvent<T> readObject (Streamable.Input in) {
            return new DSet.AddEvent<T>(
                in.readInt(),
                in.readShort(),
                in.<T>readValue()
            );
        }

        public static <T> void writeObjectImpl (Streamable.Output out, DSet.AddEvent<T> obj) {
            Streamer_DAttribute.Event.writeObjectImpl(out, obj);
            out.writeValue(obj._elem);
        }
    }

    /**
     * Handles the streaming of {@link DSet.RemoveEvent} instances.
     */
    public static class RemoveEvent<T>
        implements Streamer<DSet.RemoveEvent<T>>
    {
        @Override
        public Class<?> getObjectClass () {
            return DSet.RemoveEvent.class;
        }

        @Override
        public void writeObject (Streamable.Output out, DSet.RemoveEvent<T> obj) {
            writeObjectImpl(out, obj);
        }

        @Override
        public DSet.RemoveEvent<T> readObject (Streamable.Input in) {
            return new DSet.RemoveEvent<T>(
                in.readInt(),
                in.readShort(),
                in.<T>readValue()
            );
        }

        public static <T> void writeObjectImpl (Streamable.Output out, DSet.RemoveEvent<T> obj) {
            Streamer_DAttribute.Event.writeObjectImpl(out, obj);
            out.writeValue(obj._elem);
        }
    }

    // no streamer for non-Streamable enclosing class: DSet
}
