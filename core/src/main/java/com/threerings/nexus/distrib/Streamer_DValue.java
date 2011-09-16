//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import com.threerings.nexus.io.Streamable;
import com.threerings.nexus.io.Streamer;

/**
 * Handles the streaming of {@link DValue} and/or nested classes.
 */
public class Streamer_DValue
{
    /**
     * Handles the streaming of {@link DValue.ChangeEvent} instances.
     */
    public static class ChangeEvent<T> implements Streamer<DValue.ChangeEvent<T>> {
        @Override
        public Class<?> getObjectClass () {
            return DValue.ChangeEvent.class;
        }

        @Override
        public void writeObject (Streamable.Output out, DValue.ChangeEvent<T> obj) {
            writeObjectImpl(out, obj);
        }

        @Override
        public DValue.ChangeEvent<T> readObject (Streamable.Input in) {
            return new DValue.ChangeEvent<T>(
                in.readInt(),
                in.readShort(),
                in.<T>readValue()
            );
        }

        public static <T> void writeObjectImpl (Streamable.Output out, DValue.ChangeEvent<T> obj) {
            Streamer_DAttribute.Event.writeObjectImpl(out, obj);
            out.writeValue(obj._value);
        }
    }

    // no streamer for non-Streamable enclosing class: DValue
}
