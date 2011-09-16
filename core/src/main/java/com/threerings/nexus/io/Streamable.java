//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.io;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.threerings.nexus.distrib.DService;
import com.threerings.nexus.distrib.NexusService;

/**
 * An interface implemented by objects that will be streamed over the network.
 */
public interface Streamable
{
    /**
     * The means by which {@link Streamable} instances read their data from a remote source.
     */
    public abstract class Input {
        public abstract boolean readBoolean ();
        public abstract byte readByte ();
        public abstract short readShort ();
        public abstract char readChar ();
        public abstract int readInt ();
        public abstract long readLong ();
        public abstract float readFloat ();
        public abstract double readDouble ();
        public abstract String readString ();
        public abstract <T extends Streamable> Class<T> readClass ();

        /**
         * Reads an enumerated value from the stream.
         */
        public <T extends Enum<T>> T readEnum (Class<T> eclass) {
            String name = readString();
            return (name.length() == 0) ? null : Enum.valueOf(eclass, name); // TODO: use ordinal
        }

        /**
         * Reads an array of boolean values from the stream. The result will not be null.
         */
        public boolean[] readBooleans () {
            // TODO: encode into bitmask
            boolean[] data = new boolean[readInt()];
            for (int ii = 0; ii < data.length; ii++) {
                data[ii] = readBoolean();
            }
            return data;
        }

        /**
         * Reads an array of byte values from the stream. The result will not be null.
         */
        public byte[] readBytes () {
            byte[] data = new byte[readInt()];
            for (int ii = 0; ii < data.length; ii++) {
                data[ii] = readByte();
            }
            return data;
        }

        /**
         * Reads an array of short values from the stream. The result will not be null.
         */
        public short[] readShorts () {
            short[] data = new short[readInt()];
            for (int ii = 0; ii < data.length; ii++) {
                data[ii] = readShort();
            }
            return data;
        }

        /**
         * Reads an array of char values from the stream. The result will not be null.
         */
        public char[] readChars () {
            char[] data = new char[readInt()];
            for (int ii = 0; ii < data.length; ii++) {
                data[ii] = readChar();
            }
            return data;
        }

        /**
         * Reads an array of int values from the stream. The result will not be null.
         */
        public int[] readInts () {
            int[] data = new int[readInt()];
            for (int ii = 0; ii < data.length; ii++) {
                data[ii] = readInt();
            }
            return data;
        }

        /**
         * Reads an array of long values from the stream. The result will not be null.
         */
        public long[] readLongs () {
            long[] data = new long[readInt()];
            for (int ii = 0; ii < data.length; ii++) {
                data[ii] = readLong();
            }
            return data;
        }

        /**
         * Reads an array of float values from the stream. The result will not be null.
         */
        public float[] readFloats () {
            float[] data = new float[readInt()];
            for (int ii = 0; ii < data.length; ii++) {
                data[ii] = readFloat();
            }
            return data;
        }

        /**
         * Reads an array of double values from the stream. The result will not be null.
         */
        public double[] readDoubles () {
            double[] data = new double[readInt()];
            for (int ii = 0; ii < data.length; ii++) {
                data[ii] = readDouble();
            }
            return data;
        }

        /**
         * Reads an array of strings from the stream. The result will not be null.
         */
        public String[] readStrings () {
            String[] data = new String[readInt()];
            for (int ii = 0; ii < data.length; ii++) {
                data[ii] = readString();
            }
            return data;
        }

        public <T extends NexusService> DService<T> readService () {
            return this.<T>readServiceFactory().createService();
        }

        /**
         * Reads a single value from the input (which must have been written via a call to {@link
         * Output#writeValue}).
         */
        public <T> T readValue () {
            return this.<T>readStreamer().readObject(this);
        }

        /**
         * Reads a series of same-typed values from the input, storing them into {@code into} using
         * the {@link Collection#add} method. The values must have been written via a call to
         * {@link Output#writeValues}.
         */
        public <T> void readValues (Collection<T> into) {
            int count = readShort();
            if (count > 0) {
                Streamer<T> s = this.<T>readStreamer();
                for (; count > 0; --count) {
                    into.add(s.readObject(this));
                }
            }
        }

        /**
         * Reads a class identifier from the stream and returns the streamer to be used to unstream
         * instances of that class.
         */
        protected abstract <T> Streamer<T> readStreamer ();

        /**
         * Reads a service factory, which can be used to create Nexus service attributes.
         */
        protected abstract <T extends NexusService> ServiceFactory<T> readServiceFactory ();
    }

    /**
     * The means by which {@link Streamable} instances write their data to a remote source.
     */
    public abstract class Output {
        public abstract void writeBoolean (boolean value);
        public abstract void writeByte (byte value);
        public abstract void writeShort (short value);
        public abstract void writeChar (char value);
        public abstract void writeInt (int value);
        public abstract void writeLong (long value);
        public abstract void writeFloat (float value);
        public abstract void writeDouble (double value);
        public abstract void writeString (String value);
        public abstract void writeClass (Class<? extends Streamable> clazz);
        public abstract void writeService (DService<?> service);

        /**
         * Writes an enumerated value to the stream. The value may be null.
         */
        public void writeEnum (Enum<?> value) {
            writeString(value == null ? "" : value.name()); // TODO: use ordinal
        }

        /**
         * Writes an array of boolean values to the stream.
         * @throws NullPointerException if the supplied array is null.
         */
        public void writeBooleans (boolean[] values) {
            // TODO: encode into bitmask
            writeInt(values.length);
            for (boolean value : values) {
                writeBoolean(value);
            }
        }

        /**
         * Writes an array of byte values to the stream.
         * @throws NullPointerException if the supplied array is null.
         */
        public void writeBytes (byte[] values) {
            writeInt(values.length);
            for (byte value : values) {
                writeByte(value);
            }
        }

        /**
         * Writes an array of short values to the stream.
         * @throws NullPointerException if the supplied array is null.
         */
        public void writeShorts (short[] values) {
            writeInt(values.length);
            for (short value : values) {
                writeShort(value);
            }
        }

        /**
         * Writes an array of char values to the stream.
         * @throws NullPointerException if the supplied array is null.
         */
        public void writeChars (char[] values) {
            writeInt(values.length);
            for (char value : values) {
                writeChar(value);
            }
        }

        /**
         * Writes an array of int values to the stream.
         * @throws NullPointerException if the supplied array is null.
         */
        public void writeInts (int[] values) {
            writeInt(values.length);
            for (int value : values) {
                writeInt(value);
            }
        }

        /**
         * Writes an array of long values to the stream.
         * @throws NullPointerException if the supplied array is null.
         */
        public void writeLongs (long[] values) {
            writeInt(values.length);
            for (long value : values) {
                writeLong(value);
            }
        }

        /**
         * Writes an array of float values to the stream.
         * @throws NullPointerException if the supplied array is null.
         */
        public void writeFloats (float[] values) {
            writeInt(values.length);
            for (float value : values) {
                writeFloat(value);
            }
        }

        /**
         * Writes an array of double values to the stream.
         * @throws NullPointerException if the supplied array is null.
         */
        public void writeDoubles (double[] values) {
            writeInt(values.length);
            for (double value : values) {
                writeDouble(value);
            }
        }

        /**
         * Writes an array of strings to the stream.
         * @throws NullPointerException if the supplied array is null.
         */
        public void writeStrings (String[] values) {
            writeInt(values.length);
            for (String value : values) {
                writeString(value);
            }
        }

        /**
         * Writes a value to the output, which may be of any of the primitive types, String, a
         * List, Set or Map collection, a class which implements {@link Streamable}, or null.
         */
        public <T> void writeValue (T value) {
            Streamer<T> s = writeStreamer(value);
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
                Streamer<T> s = writeStreamer(first);
                s.writeObject(this, first);
                while (--count > 0) {
                    s.writeObject(this, from.next());
                }
            }
        }

        /**
         * Writes the class code for the supplied value and returns the streamer to be used to
         * stream the value's data.
         */
        protected abstract <T> Streamer<T> writeStreamer (T value);
    }
}
