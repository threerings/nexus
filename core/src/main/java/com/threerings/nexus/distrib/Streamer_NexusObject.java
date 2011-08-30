//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import com.threerings.nexus.io.Streamable;
import com.threerings.nexus.io.Streamer;

/**
 * Handles the streaming of {@link NexusObject} and/or nested classes.
 */
public class Streamer_NexusObject
{
    /**
     * Handles the streaming of {@link NexusObject.DummyKeyed} instances.
     */
    public static class DummyKeyed
        implements Streamer<NexusObject.DummyKeyed>
    {
        @Override
        public Class<?> getObjectClass () {
            return NexusObject.DummyKeyed.class;
        }

        @Override
        public void writeObject (Streamable.Output out, NexusObject.DummyKeyed obj) {
            writeObjectImpl(out, obj);
        }

        @Override
        public NexusObject.DummyKeyed readObject (Streamable.Input in) {
            return new NexusObject.DummyKeyed(
            );
        }

        public static  void writeObjectImpl (Streamable.Output out, NexusObject.DummyKeyed obj) {
        }
    }

    /**
     * Handles the streaming of {@link NexusObject.DummySingle} instances.
     */
    public static class DummySingle
        implements Streamer<NexusObject.DummySingle>
    {
        @Override
        public Class<?> getObjectClass () {
            return NexusObject.DummySingle.class;
        }

        @Override
        public void writeObject (Streamable.Output out, NexusObject.DummySingle obj) {
            writeObjectImpl(out, obj);
        }

        @Override
        public NexusObject.DummySingle readObject (Streamable.Input in) {
            return new NexusObject.DummySingle(
            );
        }

        public static  void writeObjectImpl (Streamable.Output out, NexusObject.DummySingle obj) {
        }
    }

    public static  void writeObjectImpl (Streamable.Output out, NexusObject obj) {
    }
}
