//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import com.threerings.nexus.io.Streamable;
import com.threerings.nexus.io.Streamer;

/**
 * Handles the streaming of {@link Address} and/or nested classes.
 */
public class Streamer_Address<T extends NexusObject>
{
    /**
     * Handles the streaming of {@link Address.OfKeyed} instances.
     */
    public static class OfKeyed<T extends NexusObject & Keyed>
        implements Streamer<Address.OfKeyed<T>>
    {
        @Override
        public Class<?> getObjectClass () {
            return Address.OfKeyed.class;
        }

        @Override
        public void writeObject (Streamable.Output out, Address.OfKeyed<T> obj) {
            writeObjectImpl(out, obj);
        }

        @Override
        public Address.OfKeyed<T> readObject (Streamable.Input in) {
            return new Address.OfKeyed<T>(
                in.readString(),
                in.<T>readClass(),
                in.<Comparable<?>>readValue()
            );
        }

        public static <T extends NexusObject & Keyed> void writeObjectImpl (Streamable.Output out, Address.OfKeyed<T> obj) {
            Streamer_Address.OfTyped.writeObjectImpl(out, obj);
            out.writeValue(obj.key);
        }
    }

    /**
     * Handles the streaming of {@link Address.OfSingleton} instances.
     */
    public static class OfSingleton<T extends NexusObject & Singleton>
        implements Streamer<Address.OfSingleton<T>>
    {
        @Override
        public Class<?> getObjectClass () {
            return Address.OfSingleton.class;
        }

        @Override
        public void writeObject (Streamable.Output out, Address.OfSingleton<T> obj) {
            writeObjectImpl(out, obj);
        }

        @Override
        public Address.OfSingleton<T> readObject (Streamable.Input in) {
            return new Address.OfSingleton<T>(
                in.readString(),
                in.<T>readClass()
            );
        }

        public static <T extends NexusObject & Singleton> void writeObjectImpl (Streamable.Output out, Address.OfSingleton<T> obj) {
            Streamer_Address.OfTyped.writeObjectImpl(out, obj);
        }
    }

    /**
     * Handles the streaming of {@link Address.OfAnonymous} instances.
     */
    public static class OfAnonymous
        implements Streamer<Address.OfAnonymous>
    {
        @Override
        public Class<?> getObjectClass () {
            return Address.OfAnonymous.class;
        }

        @Override
        public void writeObject (Streamable.Output out, Address.OfAnonymous obj) {
            writeObjectImpl(out, obj);
        }

        @Override
        public Address.OfAnonymous readObject (Streamable.Input in) {
            return new Address.OfAnonymous(
                in.readString(),
                in.readInt()
            );
        }

        public static  void writeObjectImpl (Streamable.Output out, Address.OfAnonymous obj) {
            Streamer_Address.writeObjectImpl(out, obj);
            out.writeInt(obj.id);
        }
    }

    /**
     * Handles the streaming of {@link Address.OfTyped} instances.
     */
    public static class OfTyped<T extends NexusObject>
        implements Streamer<Address.OfTyped<T>>
    {
        @Override
        public Class<?> getObjectClass () {
            return Address.OfTyped.class;
        }

        @Override
        public void writeObject (Streamable.Output out, Address.OfTyped<T> obj) {
            writeObjectImpl(out, obj);
        }

        @Override
        public Address.OfTyped<T> readObject (Streamable.Input in) {
            return new Address.OfTyped<T>(
                in.readString(),
                in.<T>readClass()
            );
        }

        public static <T extends NexusObject> void writeObjectImpl (Streamable.Output out, Address.OfTyped<T> obj) {
            Streamer_Address.writeObjectImpl(out, obj);
            out.writeClass(obj.clazz);
        }
    }

    public static <T extends NexusObject> void writeObjectImpl (Streamable.Output out, Address<T> obj) {
        out.writeString(obj.host);
    }
}
