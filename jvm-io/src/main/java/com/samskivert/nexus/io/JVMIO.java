//
// $Id$
//
// Nexus JVMIO - I/O and network services for Nexus built on java.nio
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;

import com.samskivert.nexus.io.StreamException;
import com.samskivert.nexus.io.Streamable;
import com.samskivert.nexus.io.Streamer;
import com.samskivert.nexus.io.Streamers;

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
            @Override public boolean readBoolean () {
                try {
                    return din.readBoolean();
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            @Override public byte readByte () {
                try {
                    return din.readByte();
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            @Override public short readShort () {
                try {
                    return din.readShort();
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            @Override public char readChar () {
                try {
                    return din.readChar();
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            @Override public int readInt () {
                try {
                    return din.readInt();
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            @Override public long readLong () {
                try {
                    return din.readLong();
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            @Override public float readFloat () {
                try {
                    return din.readFloat();
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            @Override public double readDouble () {
                try {
                    return din.readDouble();
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            @Override public String readString () {
                try {
                    return din.readBoolean() ? din.readUTF() : null;
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            @Override public <T extends Streamable> Class<T> readClass () {
                short code = readResolveClassCode();
                Class<?> clazz = _classes.get(code);
                if (clazz == null) {
                    throw new StreamException("Received unknown class code " + code);
                }
                @SuppressWarnings("unchecked") Class<T> tclazz = (Class<T>)clazz;
                return tclazz;
            }

            @Override protected <T> Streamer<T> readStreamer () {
                short code = readResolveClassCode();
                Streamer<?> s = _streamers.get(code);
                if (s == null) {
                    throw new StreamException("Received unknown class code " + code);
                }
                @SuppressWarnings("unchecked") Streamer<T> ts = (Streamer<T>)s;
                return ts;
            }

            protected final short readResolveClassCode () {
                short code = readShort();
                if (code < 0) {
                    code = (short)-code;
                    resolveStreamer(code);
                }
                return code;
            }

            protected final void resolveStreamer (short code) {
                String cname = readString();
                Streamer<?> s = findStreamer(cname);
                // extract the target type from the Streamer class (for use by readClass())
                try {
                    _classes.put(code, s.getClass().getDeclaredMethod(
                                     "readObject", Streamable.Input.class).getReturnType());
                } catch (Throwable t) {
                    throw new StreamException("Error creating streamer (" + s.getClass() + ")", t);
                }
                _streamers.put(code, s);
            }

            protected Map<Short, Class<?>> _classes = Maps.newHashMap();
            protected Map<Short, Streamer<?>> _streamers = Maps.newHashMap(STREAMERS);
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
            @Override public void writeBoolean (boolean value) {
                try {
                    dout.writeBoolean(value);
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            @Override public void writeByte (byte value) {
                try {
                    dout.writeByte(value);
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            @Override public void writeShort (short value) {
                try {
                    dout.writeShort(value);
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            @Override public void writeChar (char value) {
                try {
                    dout.writeChar(value);
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            @Override public void writeInt (int value) {
                try {
                    dout.writeInt(value);
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            @Override public void writeLong (long value) {
                try {
                    dout.writeLong(value);
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            @Override public void writeFloat (float value) {
                try {
                    dout.writeFloat(value);
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            @Override public void writeDouble (double value) {
                try {
                    dout.writeDouble(value);
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            @Override public void writeString (String value) {
                try {
                    if (value == null) {
                        dout.writeBoolean(false);
                    } else {
                        dout.writeBoolean(true);
                        dout.writeUTF(value);
                    }
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            }

            @Override public void writeClass (Class<? extends Streamable> clazz) {
                Short code = _classes.get(clazz);
                if (code != null) {
                    writeShort(code);
                } else {
                    resolveAndWriteClass(clazz);
                }
            }

            @Override protected <T> Streamer<T> writeStreamer (T value) {
                if (value == null) {
                    return this.<T>writeClass((short)0); // null streamer has code 0
                }

                // if the class is known, just look up the code and write it
                Class<?> vclass = value.getClass();
                Short code = _classes.get(vclass);
                if (code != null) {
                    return this.<T>writeClass(code);
                }

                // if the class is some more obscure subtype of list/set/map, use the stock
                // streamer and cache this type with the same code
                if (value instanceof List) {
                    _classes.put(vclass, code = _classes.get(ArrayList.class));
                    return this.<T>writeClass(code);
                } else if (value instanceof Set) {
                    _classes.put(vclass, code = _classes.get(HashSet.class));
                    return this.<T>writeClass(code);
                } else if (value instanceof Map) {
                    _classes.put(vclass, code = _classes.get(HashMap.class));
                    return this.<T>writeClass(code);
                }

                // otherwise we need to load and cache the streamer for this class
                return resolveAndWriteClass(vclass);
            }

            @SuppressWarnings("unchecked") 
            protected final <T> Streamer<T> writeClass (Short code) {
                writeShort(code);
                return (Streamer<T>)_streamers.get(code);
            }

            protected <T> Streamer<T> resolveAndWriteClass (Class<?> clazz) {
                if (_nextCode == Short.MAX_VALUE) {
                    throw new StreamException("Cannot stream more than " + Short.MAX_VALUE +
                                              " different value types.");
                }
                Short code = (short)++_nextCode;
                String cname = clazz.getName();
                @SuppressWarnings("unchecked") Streamer<T> s = (Streamer<T>)findStreamer(cname);
                _streamers.put(code, s);
                _classes.put(clazz, code); // do this after findStreamer, which may fail
                writeShort((short)-code);
                writeString(cname);
                return s;
            }

            protected Map<Class<?>, Short> _classes = Maps.newHashMap(CLASSES);
            protected Map<Short, Streamer<?>> _streamers = Maps.newHashMap(STREAMERS);
            protected int _nextCode = _streamers.size()-1;
        };
    }

    private JVMIO () {} // no constructy

    protected static Streamer<?> findStreamer (String cname)
    {
        int didx = cname.lastIndexOf(".");
        // the below works whether didx is -1 or a valid index
        String sname = cname.substring(0, didx+1) + "Streamer_" + cname.substring(didx+1);
        try {
            return (Streamer<?>)Class.forName(sname).newInstance();
        } catch (Exception e) {
            throw new StreamException("Error instantiating streamer " + sname, e);
        }
    }

    protected static void mapStreamer (int code, Streamer<?> streamer, Class<?>... classes)
    {
        STREAMERS.put((short)code, streamer);
        for (Class<?> clazz : classes) {
            CLASSES.put(clazz, (short)code);
        }
    }

    protected static final Map<Short,Streamer<?>> STREAMERS = Maps.newHashMap();
    protected static final Map<Class<?>,Short> CLASSES = Maps.newHashMap();
    static {
        // map the streamers for our basic types
        mapStreamer(0, new Streamers.Streamer_Null());
        mapStreamer(1, new Streamers.Streamer_Boolean(), Boolean.class);
        mapStreamer(2, new Streamers.Streamer_Byte(), Byte.class);
        mapStreamer(3, new Streamers.Streamer_Character(), Character.class);
        mapStreamer(4, new Streamers.Streamer_Short(), Short.class);
        mapStreamer(5, new Streamers.Streamer_Integer(), Integer.class);
        mapStreamer(6, new Streamers.Streamer_Long(), Long.class);
        mapStreamer(7, new Streamers.Streamer_Float(), Float.class);
        mapStreamer(8, new Streamers.Streamer_Double(), Double.class);
        mapStreamer(9, new Streamers.Streamer_String(), String.class);
        // fast path for common implementations; a slow path will catch all other types with a
        // series of instanceof checks
        mapStreamer(10, new Streamers.Streamer_List(), ArrayList.class);
        mapStreamer(11, new Streamers.Streamer_Set(), HashSet.class);
        mapStreamer(12, new Streamers.Streamer_Map(), HashMap.class);
    }
}
