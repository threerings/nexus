//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.io;

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
        boolean readBoolean ();
        byte readByte ();
        short readShort ();
        char readChar ();
        int readInt ();
        long readLong ();
        float readFloat ();
        double readDouble ();
        String readString ();
        <T extends Streamable> T readStreamable ();
        // TODO: readList, readSet, readMap?
    }

    /**
     * The means by which {@link Streamable} instances write their data to a remote source.
     */
    public interface Output
    {
        void writeBoolean (boolean value);
        void writeByte (byte value);
        void writeShort (short value);
        void writeChar (char value);
        void writeInt (int value);
        void writeLong (long value);
        void writeFloat (float value);
        void writeDouble (double value);
        void writeString (String value);
        <T extends Streamable> void writeStreamable (T value);
        // TODO: writeList, writeSet, writeMap?
    }

    /**
     * Reads the contents of this streamable instance from the supplied input stream.
     *
     * @exception StreamException thrown if an error occurs while reading.
     */
    void readObject (Input in);

    /**
     * Writes the contents of this streamable instance to the supplied output stream.
     *
     * @exception StreamException thrown if an error occurs while writing.
     */
    void writeObject (Output out);
}
