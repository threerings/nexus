//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.io;

import java.io.IOException;

/**
 * An interface implemented by objects that can be streamed over the network.
 */
public interface Streamable
{
    /**
     * The means by which {@link Streamable} instances read their data from a remote source.
     */
    public interface Input
    {
        boolean readBoolean () throws IOException;
        byte readByte () throws IOException;
        short readShort () throws IOException;
        char readChar () throws IOException;
        int readInt () throws IOException;
        long readLong () throws IOException;
        float readFloat () throws IOException;
        double readDouble () throws IOException;
        String readString () throws IOException;
        Streamable readStreamable () throws IOException;
        // TODO: readList, readSet, readMap?
    }

    /**
     * The means by which {@link Streamable} instances write their data to a remote source.
     */
    public interface Output
    {
        void writeBoolean (boolean value) throws IOException;
        void writeByte (byte value) throws IOException;
        void writeShort (short value) throws IOException;
        void writeChar (char value) throws IOException;
        void writeInt (int value) throws IOException;
        void writeLong (long value) throws IOException;
        void writeFloat (float value) throws IOException;
        void writeDouble (double value) throws IOException;
        void writeString (String value) throws IOException;
        void writeStreamable (Streamable value) throws IOException;
        // TODO: writeList, writeSet, writeMap?
    }

    /**
     * Reads the contents of this streamable instance from the supplied input stream.
     */
    void readObject (Input in) throws IOException;

    /**
     * Writes the contents of this streamable instance to the supplied output stream.
     */
    void writeObject (Output out) throws IOException;
}
