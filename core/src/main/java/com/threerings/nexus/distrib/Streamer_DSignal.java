//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import com.threerings.nexus.io.Streamable;
import com.threerings.nexus.io.Streamer;

/**
 * Handles the streaming of {@link DSignal} and/or nested classes.
 */
public class Streamer_DSignal
{
    /**
     * Handles the streaming of {@link DSignal.EmitEvent} instances.
     */
    public static class EmitEvent<T>
        implements Streamer<DSignal.EmitEvent<T>>
    {
        @Override
        public Class<?> getObjectClass () {
            return DSignal.EmitEvent.class;
        }

        @Override
        public void writeObject (Streamable.Output out, DSignal.EmitEvent<T> obj) {
            writeObjectImpl(out, obj);
        }

        @Override
        public DSignal.EmitEvent<T> readObject (Streamable.Input in) {
            return new DSignal.EmitEvent<T>(
                in.readInt(),
                in.readShort(),
                in.<T>readValue()
            );
        }

        public static <T> void writeObjectImpl (Streamable.Output out, DSignal.EmitEvent<T> obj) {
            Streamer_DAttribute.Event.writeObjectImpl(out, obj);
            out.writeValue(obj._event);
        }
    }

    // no streamer for non-Streamable enclosing class: DSignal
}
