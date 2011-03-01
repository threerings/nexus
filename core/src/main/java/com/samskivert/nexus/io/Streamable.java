//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.io;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An interface implemented by objects that will be streamed over the network.
 */
public interface Streamable
{
    /**
     * The means by which {@link Streamable} instances read their data from a remote source.
     */
    public abstract class Input
    {
        public abstract boolean readBoolean ();
        public abstract byte readByte ();
        public abstract short readShort ();
        public abstract char readChar ();
        public abstract int readInt ();
        public abstract long readLong ();
        public abstract float readFloat ();
        public abstract double readDouble ();
        public abstract String readString ();

        /**
         * Reads a single value from the input (which must have been written via a call to {@link
         * Output#writeValue}).
         */
        public <T> T readValue () {
            return this.<T>readClass().readObject(this);
        }

        /**
         * Reads a series of same-typed values from the input, storing them into {@code into} using
         * the {@link Collection#add} method. The values must have been written via a call to
         * {@link Output#writeValues}.
         */
        public <T> void readValues (Collection<T> into) {
            int count = readShort();
            Streamer<T> s = this.<T>readClass();
            for (; count > 0; --count) {
                into.add(s.readObject(this));
            }
        }

        /**
         * Reads a class identifier from the stream and returns the streamer to be used to unstream
         * instances of that class.
         */
        protected abstract <T> Streamer<T> readClass ();
    }

    /**
     * The means by which {@link Streamable} instances write their data to a remote source.
     */
    public abstract class Output
    {
        public abstract void writeBoolean (boolean value);
        public abstract void writeByte (byte value);
        public abstract void writeShort (short value);
        public abstract void writeChar (char value);
        public abstract void writeInt (int value);
        public abstract void writeLong (long value);
        public abstract void writeFloat (float value);
        public abstract void writeDouble (double value);
        public abstract void writeString (String value);

        /**
         * Writes a value to the output, which may be of any of the primitive types, String, a
         * List, Set or Map collection, a class which implements {@link Streamable}, or null.
         */
        public <T> void writeValue (T value) {
            Streamer<T> s = writeClass(value);
            if (s != null) {
                s.writeObject(this, value);
            }
        }

        /**
         * Writes a series of same-typed values to the output. The values must be of exactly the
         * same type and of a type supported by {@link #writeValue}.
         */
        public <T> void writeValues (int count, Iterator<T> from) {
            assert(count >= 0);
            if (count > Short.MAX_VALUE) {
                throw new IllegalArgumentException(
                    "Cannot write more than " + Short.MAX_VALUE + " values.");
            }
            writeShort((short)count);
            if (count > 0) {
                T first = from.next();
                Streamer<T> s = writeClass(first);
                s.writeObject(this, first);
                while (from.hasNext()) {
                    s.writeObject(this, from.next());
                }
            }
        }

        /**
         * Writes the class code for the supplied value and returns the streamer to be used to
         * stream the value's data.
         */
        protected abstract <T> Streamer<T> writeClass (T value);
    }
}
