//
// $Id$

package com.samskivert.nexus.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.samskivert.nexus.io.StreamException;
import com.samskivert.nexus.io.Streamable;

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
            public <T extends Streamable> T readStreamable () {
                throw new UnsupportedOperationException(); // TODO: reflect!
            }
            @SuppressWarnings("unchecked") public <T> T readValue () {
                return (T)Streamer.fromCode(readByte()).readValue(this);
            }
            public <T> Collection<T> readValues () {
                throw new UnsupportedOperationException(); // TODO
//                 try {
//                     return din.readBoolean();
//                 } catch (IOException ioe) {
//                     throw new StreamException(ioe);
//                 }
            }
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
            public <T extends Streamable> void writeStreamable (T value) {
                throw new UnsupportedOperationException(); // TODO: reflect!
            }
            @SuppressWarnings("unchecked") public <T> void writeValue (T value) {
                Streamer s = Streamer.fromValue(value);
                writeByte(s.code);
                s.writeValue(this, value);
            }
            public <T> void writeValues (Collection<T> value) {
                throw new UnsupportedOperationException(); // TODO
//                 try {
//                     dout.writeBoolean(value);
//                 } catch (IOException ioe) {
//                     throw new StreamException(ioe);
//                 }
            }
        };
    }

    private JVMIO () {} // no constructy

    protected static enum Streamer {
        NULL(0) {
            public Object readValue (Streamable.Input in) {
                return null;
            }
            public void writeValue (Streamable.Output out, Object value) {
                // noop!
            }
        },
        BOOLEAN(1, Boolean.TYPE, Boolean.class) {
            public Object readValue (Streamable.Input in) {
                return in.readBoolean();
            }
            public void writeValue (Streamable.Output out, Object value) {
                out.writeBoolean((Boolean)value);
            }
        },
        BYTE(2, Byte.TYPE, Byte.class) {
            public Object readValue (Streamable.Input in) {
                return in.readByte();
            }
            public void writeValue (Streamable.Output out, Object value) {
                out.writeByte((Byte)value);
            }
        },
        CHAR(3, Character.TYPE, Character.class) {
            public Object readValue (Streamable.Input in) {
                return in.readChar();
            }
            public void writeValue (Streamable.Output out, Object value) {
                out.writeChar((Character)value);
            }
        },
        SHORT(4, Short.TYPE, Short.class) {
            public Object readValue (Streamable.Input in) {
                return in.readShort();
            }
            public void writeValue (Streamable.Output out, Object value) {
                out.writeShort((Short)value);
            }
        },
        INT(5, Integer.TYPE, Integer.class) {
            public Object readValue (Streamable.Input in) {
                return in.readInt();
            }
            public void writeValue (Streamable.Output out, Object value) {
                out.writeInt((Integer)value);
            }
        },
        LONG(6, Long.TYPE, Long.class) {
            public Object readValue (Streamable.Input in) {
                return in.readLong();
            }
            public void writeValue (Streamable.Output out, Object value) {
                out.writeLong((Long)value);
            }
        },
        FLOAT(7, Float.TYPE, Float.class) {
            public Object readValue (Streamable.Input in) {
                return in.readFloat();
            }
            public void writeValue (Streamable.Output out, Object value) {
                out.writeFloat((Float)value);
            }
        },
        DOUBLE(8, Double.TYPE, Double.class) {
            public Object readValue (Streamable.Input in) {
                return in.readDouble();
            }
            public void writeValue (Streamable.Output out, Object value) {
                out.writeDouble((Double)value);
            }
        },
        STRING(9, String.class) {
            public Object readValue (Streamable.Input in) {
                return in.readString();
            }
            public void writeValue (Streamable.Output out, Object value) {
                out.writeString((String)value);
            }
        };
        // TODO: list, set, map?

        /** Reads a value of our associated type from the supplied input. (The code is assumed to
         * already have been read from the stream and used to select this streamer.) */
        public abstract Object readValue (Streamable.Input in);

        /** Writes a value of our associated type to the supplied input. (The code is assumed to
         * already have been written to the stream by the caller.) */
        public abstract void writeValue (Streamable.Output out, Object value);

        /** Returns the streamer associated with the supplied code. */
        public static Streamer fromCode (byte code) {
            return _fromCode.get(code);
        }

        /** Returns the streamer associated with the supplied value. */
        public static Streamer fromValue (Object value) {
            if (value == null) {
                return NULL;
            }
            Streamer s = _fromType.get(value.getClass());
            if (s == null) {
                throw new StreamException(
                    "Requested to stream unsupported value type " + value.getClass());
            }
            return s;
        }

        /** The code used to prefix this type on the wire. */
        public final byte code;

        /** The types handled by this streamer. */
        public final Class<?>[] types;

        Streamer (int code, Class<?>... types) {
            this.code = (byte)code;
            this.types = types;
        }

        protected static Map<Byte, Streamer> _fromCode = new HashMap<Byte, Streamer>();
        protected static Map<Class<?>, Streamer> _fromType = new HashMap<Class<?>, Streamer>();
        static {
            for (Streamer s : Streamer.values()) {
                _fromCode.put(s.code, s);
                for (Class<?> type : s.types) {
                    _fromType.put(type, s);
                }
            }
        }
    }
}
