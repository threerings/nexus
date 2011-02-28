//
// $Id$

package com.samskivert.nexus.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.samskivert.nexus.io.StreamException;
import com.samskivert.nexus.io.Streamable;
import com.samskivert.nexus.io.Streamer;

/**
 * Provides {@link Streamable#Input} and {@link Streamable#Output} using reflection and I/O support
 * provided by the JVM.
 */
public class JVMIO
{
    /**
     * Returns a {@link Streamable#Input} that obtains its underlying data from the supplied input
     * stream.
     */
    public static Streamable.Input newInput (InputStream in)
    {
        final DataInputStream din = new DataInputStream(in);
        return new Streamable.Input() {
            public boolean readBoolean () {
                try {
                    return din.readBoolean();
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            public byte readByte () {
                try {
                    return din.readByte();
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            public short readShort () {
                try {
                    return din.readShort();
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            public char readChar () {
                try {
                    return din.readChar();
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            public int readInt () {
                try {
                    return din.readInt();
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            public long readLong () {
                try {
                    return din.readLong();
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            public float readFloat () {
                try {
                    return din.readFloat();
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            public double readDouble () {
                try {
                    return din.readDouble();
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            public String readString () {
                try {
                    return din.readUTF();
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            public Class<?> readClass () {
                short code = readShort();
                if (code == 0) {
                    return null; // zero means we're sending the null class
                }
                if (code > 0) {
                    Class<?> clazz = _classes.get(code);
                    if (clazz == null) {
                        throw new StreamException("Received unknown class code: " + code);
                    }
                    return clazz;
                }
                try {
                    Class<?> clazz = Class.forName(readString());
                    _classes.put((short)-code, clazz);
                    return clazz;
                } catch (Exception e) {
                    throw new StreamException(e);
                }
            }

            public <T extends Streamable> T readStreamable () {
                @SuppressWarnings("unchecked") Class<T> clazz = (Class<T>)readClass();
                return (clazz == null) ? null : // null class means null instance
                    getStreamer(_streamers, clazz).readObject(this);
            }

            @SuppressWarnings("unchecked") public <T> T readValue () {
                return (T)ValueStreamer.fromCode(readByte()).readValue(this);
            }

            public <T> Collection<T> readValues () {
                throw new UnsupportedOperationException(); // TODO
            }

            protected Map<Short, Class<?>> _classes = new HashMap<Short, Class<?>>();
            protected Map<Class<?>, Streamer<?>> _streamers = new HashMap<Class<?>, Streamer<?>>();
        };
    }

    /**
     * Returns a {@link Streamable#Output} that obtains its underlying data from the supplied output
     * stream.
     */
    public static Streamable.Output newOutput (OutputStream out)
    {
        final DataOutputStream dout = new DataOutputStream(out);
        return new Streamable.Output() {
            public void writeBoolean (boolean value) {
                try {
                    dout.writeBoolean(value);
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            public void writeByte (byte value) {
                try {
                    dout.writeByte(value);
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            public void writeShort (short value) {
                try {
                    dout.writeShort(value);
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            public void writeChar (char value) {
                try {
                    dout.writeChar(value);
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            public void writeInt (int value) {
                try {
                    dout.writeInt(value);
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            public void writeLong (long value) {
                try {
                    dout.writeLong(value);
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            public void writeFloat (float value) {
                try {
                    dout.writeFloat(value);
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            public void writeDouble (double value) {
                try {
                    dout.writeDouble(value);
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            public void writeString (String value) {
                try {
                    dout.writeUTF(value);
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            public void writeClass (Class<?> value) {
                Short code = _classes.get(value);
                if (code != null) {
                    writeShort(code);
                } else {
                    if (_nextCode >= Short.MAX_VALUE) {
                        throw new StreamException(
                            "Cannot stream more than " + Short.MAX_VALUE + " unique classes.");
                    }
                    _classes.put(value, code = (short)++_nextCode);
                    writeShort((short)-code);
                    writeString(value.getName());
                }
            }

            public <T extends Streamable> void writeStreamable (T value) {
                if (value == null) {
                    writeShort((short)0); // send the null class for the null instance
                } else {
                    @SuppressWarnings("unchecked") Class<T> clazz = (Class<T>)value.getClass();
                    writeClass(clazz);
                    getStreamer(_streamers, clazz).writeObject(this, value);
                }
            }

            @SuppressWarnings("unchecked") public <T> void writeValue (T value) {
                ValueStreamer s = ValueStreamer.fromValue(value);
                writeByte(s.code);
                s.writeValue(this, value);
            }

            public <T> void writeValues (Collection<T> value) {
                throw new UnsupportedOperationException(); // TODO
            }

            protected int _nextCode = 0;
            protected Map<Class<?>, Short> _classes = new HashMap<Class<?>, Short>();
            protected Map<Class<?>, Streamer<?>> _streamers = new HashMap<Class<?>, Streamer<?>>();
        };
    }

    private JVMIO () {} // no constructy

    /**
     * Returns the name of the streamer class `pkg.Streamer_Name` for a given class `pkg.Name`.
     */
    protected static String getStreamerName (Class<?> clazz)
    {
        int pkpre = clazz.getPackage().getName().length()+1;
        String fullName = clazz.getName();
        return fullName.substring(0, pkpre) + "Streamer_" + fullName.substring(pkpre);
    }

    protected static <T extends Streamable> Streamer<T> getStreamer (
        Map<Class<?>, Streamer<?>> cache, Class<T> clazz)
    {
        Streamer<?> s = cache.get(clazz);
        if (s == null) {
            try {
                s = (Streamer<?>)Class.forName(getStreamerName(clazz)).newInstance();
                cache.put(clazz, s);
            } catch (Exception e) {
                throw new StreamException(
                    "Error creating streamer for " + clazz.getName(), e);
            }
        }
        @SuppressWarnings("unchecked") Streamer<T> typed = (Streamer<T>)s;
        return typed;
    }
}
