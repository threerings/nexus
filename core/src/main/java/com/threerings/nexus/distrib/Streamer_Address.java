//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import com.threerings.nexus.io.Streamable;
import com.threerings.nexus.io.Streamer;

/**
 * Handles the streaming of {@link Address} instances.
 */
public class Streamer_Address
{
    public static class OfKeyed<T extends NexusObject & Keyed> implements Streamer<Address.OfKeyed<T>>
    {
        public Class<?> getObjectClass () {
            return Address.OfKeyed.class;
        }
        public void writeObject (Streamable.Output out, Address.OfKeyed<T> obj) {
            out.writeString(obj.host);
            out.writeClass(obj.clazz);
            out.writeValue(obj.key);
        }
        public Address.OfKeyed<T> readObject (Streamable.Input in) {
            return new Address.OfKeyed<T>(in.readString(), in.<T>readClass(),
                                          in.<Comparable<?>>readValue());
        }
    }

    public static class OfSingleton<T extends NexusObject & Singleton>
        implements Streamer<Address.OfSingleton<T>>
    {
        public Class<?> getObjectClass () {
            return Address.OfSingleton.class;
        }
        public void writeObject (Streamable.Output out, Address.OfSingleton<T> obj) {
            out.writeString(obj.host);
            out.writeClass(obj.clazz);
        }
        public Address.OfSingleton<T> readObject (Streamable.Input in) {
            return new Address.OfSingleton<T>(in.readString(), in.<T>readClass());
        }
    }

    public static class OfAnonymous implements Streamer<Address.OfAnonymous>
    {
        public Class<?> getObjectClass () {
            return Address.OfAnonymous.class;
        }
        public void writeObject (Streamable.Output out, Address.OfAnonymous obj) {
            out.writeString(obj.host);
            out.writeInt(obj.id);
        }
        public Address.OfAnonymous readObject (Streamable.Input in) {
            return new Address.OfAnonymous(in.readString(), in.readInt());
        }
    }
}
