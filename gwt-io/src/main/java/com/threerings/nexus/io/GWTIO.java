//
// Nexus GWTIO - I/O and network services for Nexus built on GWT and WebSockets
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.io;

/**
 * Provides {@link Streamable#Input} and {@link Streamable#Output} using precomputed class
 * mappings, and writes data to and from UTF8 strings. Note that this protocol is assymetric and
 * the corresponding server reader and writer are in the gwt-server project (to allow the server
 * code to make use of the full range of JDK libraries).
 */
public class GWTIO
{
    /**
     * Returns a {@link Streamable#Input} that obtains its underlying data from the supplied string
     * payload.
     */
    public static Streamable.Input newInput (Serializer szer, String data) {
        return new ClientInput(szer, data);
    }

    /**
     * Returns a {@link Streamable#Output} that obtains its underlying data from the supplied output
     * stream.
     */
    public static Streamable.Output newOutput (Serializer szer, StringBuffer output) {
        return new ClientOutput(szer, output);
    }

    private GWTIO () {} // no constructsky
}
