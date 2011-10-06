//
// Nexus GWTServer - server-side support for Nexus GWT/WebSockets services
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.io;

/**
 * Provides {@link Streamable#Input} and {@link Streamable#Output} using precomputed class
 * mappings, and writes data to and from UTF8 strings. Note that this protocol is assymetric and
 * the corresponding client reader and writer are in the nexus-gwt-io project.
 */
public class GWTServerIO
{
    /** Used to accumulate output. Can be used on successive calls to {@link #newOutput}.
     */
    public static class PayloadBuffer {
        /** Returns the string payload, ready for delivery to the client. */
        public String getPayload () {
            if (_buffer.length() == 1) {
                _buffer.append(']');
            } else {
                // if we have at least one value, we'll have a trailing separator to overwrite
                _buffer.setCharAt(_buffer.length()-1, ']');
            }
            return _buffer.toString();
        }

        protected void prepare () {
            _buffer.setLength(0);
            _buffer.append('[');
        }

        protected void appendSeparator () {
            _buffer.append(',');
        }

        /** The buffer to which this payload accumulates. */
        protected final StringBuffer _buffer = new StringBuffer();
    }

    /**
     * Returns a {@link Streamable#Input} that obtains its underlying data from the supplied string
     * payload.
     */
    public static Streamable.Input newInput (Serializer szer, String data) {
        return new ServerInput(szer, data);
    }

    /**
     * Returns a {@link Streamable#Output} that obtains its underlying data from the supplied output
     * stream.
     */
    public static Streamable.Output newOutput (Serializer szer, PayloadBuffer buffer) {
        return new ServerOutput(szer, buffer);
    }

    private GWTServerIO () {} // no constructsky
}
