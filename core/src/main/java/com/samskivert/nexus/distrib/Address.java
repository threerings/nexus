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
    public static class OfKeyed<T extends NexusObject & Keyed> extends OfTyped<T>
    {
        /** The key identifying our target object. */
        public final Comparable<?> key;

        public OfKeyed (String host, Class<T> clazz, Comparable<?> key) {
            super(host, clazz);
            this.key = key;
        }

        @Override public String toString () {
            return super.toString() + ":" + key;
        }

        @Override public int hashCode () {
            return key.hashCode() ^ super.hashCode();
        }

        @Override public boolean equals (Object other) {
            if (other.getClass() != getClass()) {
                return false;
            }
            OfKeyed<?> oaddr = (OfKeyed<?>)other;
            return oaddr.key.equals(key) && super.equals(oaddr);
        }
    }

    /** An address of a singleton object. */
    public static class OfSingleton<T extends NexusObject & Singleton> extends OfTyped<T>
    {
        public OfSingleton (String host, Class<T> clazz) {
            super(host, clazz);
        }
    }

    /** An address of an anonymous object. */
    public static class OfAnonymous extends Address<NexusObject>
    {
        /** The id of the target object. */
        public final int id;

        public OfAnonymous (String host, int id) {
            super(host);
            this.id = id;
        }

        @Override public String toString () {
            return super.toString() + ":" + id;
        }

        @Override public int hashCode () {
            return id ^ super.hashCode();
        }

        @Override public boolean equals (Object other) {
            if (other.getClass() != getClass()) {
                return false;
            }
            OfAnonymous oaddr = (OfAnonymous)other;
            return (oaddr.id == id) && super.equals(oaddr);
        }
    }

    /**
     * Creates an address for a keyed instance on the specified host.
     */
    public static <T extends NexusObject & Keyed> Address<T> create (
        String host, Class<T> clazz, Comparable<?> key)
    {
        return new OfKeyed<T>(host, clazz, key);
    }

    /**
     * Creates an address for a singleton instance on the specified host.
     */
    public static <T extends NexusObject & Singleton> Address<T> create (String host, Class<T> clazz)
    {
        return new OfSingleton<T>(host, clazz);
    }

    /**
     * Creates an address for an anonymous object on the specified host.
     */
    public static Address<NexusObject> create (String host, int id)
    {
        return new OfAnonymous(host, id);
    }

    /** The hostname of the server on which this object resides. */
    public final String host;

    @Override public String toString ()
    {
        return host;
    }

    @Override public int hashCode ()
    {
        return host.hashCode();
    }

    @Override public boolean equals (Object other)
    {
        if (other.getClass() != getClass()) {
            return false;
        }
        Address<?> oaddr = (Address<?>)other;
        return oaddr.host.equals(host);
    }

    protected Address (String host)
    {
        this.host = host;
    }

    /** An address of an object with a type. */
    protected static class OfTyped<T extends NexusObject> extends Address<T>
    {
        /** The type of this object. */
        public final Class<T> clazz;

        public OfTyped (String host, Class<T> clazz) {
            super(host);
            this.clazz = clazz;
        }

        @Override public String toString () {
            return super.toString() + ":" + clazz.getName();
        }

        @Override public int hashCode () {
            return clazz.hashCode() ^ super.hashCode();
        }

        @Override public boolean equals (Object other) {
            if (other.getClass() != getClass()) {
                return false;
            }
            OfTyped<?> oaddr = (OfTyped<?>)other;
            return oaddr.clazz.equals(clazz) && super.equals(oaddr);
        }
    }
}
