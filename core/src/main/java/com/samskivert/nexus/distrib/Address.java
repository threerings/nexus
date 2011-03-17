//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.distrib;

import com.samskivert.nexus.io.Streamable;

/**
 * Identifies a distributed object somewhere in the Nexus network. The object may be keyed, or a
 * singleton, as dictated by subclasses.
 */
public abstract class Address<T extends NexusObject> implements Streamable
{
    /** An address of a keyed object. */
    public static class OfKeyed<T extends NexusObject & Keyed> extends Address<T>
    {
        /** The key identifying our target object. */
        public final Comparable<?> key;

        public OfKeyed (String host, Class<T> clazz, Comparable<?> key) {
            super(host, clazz);
            this.key = key;
        }
    }

    /** An address of a singleton object. */
    public static class OfSingleton<T extends NexusObject & Singleton> extends Address<T>
    {
        public OfSingleton (String host, Class<T> clazz) {
            super(host, clazz);
        }
    }

    /**
     * Creates an address for a singleton instance on the specified host.
     */
    public static <T extends NexusObject & Singleton> Address<T> make (String host, Class<T> clazz)
    {
        return new OfSingleton<T>(host, clazz);
    }

    /**
     * Creates an address for a keyed instance on the specified host.
     */
    public static <T extends NexusObject & Keyed> Address<T> make (
        String host, Class<T> clazz, Comparable<?> key)
    {
        return new OfKeyed<T>(host, clazz, key);
    }

    /** The hostname of the server on which this object resides. */
    public final String host;

    /** The type of this object. */
    public final Class<T> clazz;

    protected Address (String host, Class<T> clazz)
    {
        this.host = host;
        this.clazz = clazz;
    }
}
