//
// $Id$

package com.samskivert.nexus.io;

import com.samskivert.nexus.distrib.NexusService;

/**
 * Provides {@link Streamable#Input} and {@link Streamable#Output} using precomputed class
 * mappings, and writes data to and from UTF8 strings. Note that this protocol is assymetric and
 * the corresponding server reader and writer are in the nexus-server project (to allow the server
 * code to make use of the full range of JDK libraries).
 */
public class GWTIO
{
    /** An automatically generated class that knows about all {@link Streamable} and
     * {@link NexusService} classes that will be used by a client. */
    public interface Serializer
    {
        /** Returns the class assigned the supplied code. */
        Class<?> getClass (short code);

        /** Returns the streamer for the class assigned the supplied code. */
        Streamer<?> getStreamer (short code);

        /** Returns the service factory for the class assigned the supplied code. */
        ServiceFactory<?> getServiceFactory (short code);

        /** Returns the code assigned to the supplied class. */
        short getCode (Class<?> clazz);

        /** Returns the code assigned to the supplied service class. */
        short getServiceCode (Class<? extends NexusService> clazz);

        /** Writes the class code for the supplied value, and returns the streamer for same. */
        <T> Streamer<T> writeStreamer (Streamable.Output out, T value);
    }

    /**
     * Returns a {@link Streamable#Input} that obtains its underlying data from the supplied string
     * payload.
     */
    public static Streamable.Input newInput (Serializer szer, String data)
    {
        return new ClientInput(szer, data);
    }

    /**
     * Returns a {@link Streamable#Output} that obtains its underlying data from the supplied output
     * stream.
     */
    public static Streamable.Output newOutput (Serializer szer, StringBuffer output)
    {
        return new ClientOutput(szer, output);
    }

    private GWTIO () {} // no constructsky
}
