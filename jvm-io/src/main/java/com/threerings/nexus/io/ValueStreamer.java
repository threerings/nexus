//
// Nexus JVMIO - I/O and network services for Nexus built on java.nio
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.io;

import java.util.HashMap;
import java.util.Map;

import com.threerings.nexus.io.Streamable;

/**
 * An enumeration used to stream values. A value is a primitive, a basic type (i.e. String, null),
 * or a collection of values. A value code is streamed followed by the binary data for the value.
 * This is used when streaming parameterized types, like the value contained in a
 * `DValue.ChangedEvent<T>` instance.
 */
public enum ValueStreamer
{
    NULL(0) {
        @Override public Object readValue (Streamable.Input in) {
            return null;
        }
        @Override public void writeValue (Streamable.Output out, Object value) {
            // noop!
        }
    },
    BOOLEAN(1, Boolean.TYPE, Boolean.class) {
        @Override public Object readValue (Streamable.Input in) {
            return in.readBoolean();
        }
        @Override public void writeValue (Streamable.Output out, Object value) {
            out.writeBoolean((Boolean)value);
        }
    },
    BYTE(2, Byte.TYPE, Byte.class) {
        @Override public Object readValue (Streamable.Input in) {
            return in.readByte();
        }
        @Override public void writeValue (Streamable.Output out, Object value) {
            out.writeByte((Byte)value);
        }
    },
    CHAR(3, Character.TYPE, Character.class) {
        @Override public Object readValue (Streamable.Input in) {
            return in.readChar();
        }
        @Override public void writeValue (Streamable.Output out, Object value) {
            out.writeChar((Character)value);
        }
    },
    SHORT(4, Short.TYPE, Short.class) {
        @Override public Object readValue (Streamable.Input in) {
            return in.readShort();
        }
        @Override public void writeValue (Streamable.Output out, Object value) {
            out.writeShort((Short)value);
        }
    },
    INT(5, Integer.TYPE, Integer.class) {
        @Override public Object readValue (Streamable.Input in) {
            return in.readInt();
        }
        @Override public void writeValue (Streamable.Output out, Object value) {
            out.writeInt((Integer)value);
        }
    },
    LONG(6, Long.TYPE, Long.class) {
        @Override public Object readValue (Streamable.Input in) {
            return in.readLong();
        }
        @Override public void writeValue (Streamable.Output out, Object value) {
            out.writeLong((Long)value);
        }
    },
    FLOAT(7, Float.TYPE, Float.class) {
        @Override public Object readValue (Streamable.Input in) {
            return in.readFloat();
        }
        @Override public void writeValue (Streamable.Output out, Object value) {
            out.writeFloat((Float)value);
        }
    },
    DOUBLE(8, Double.TYPE, Double.class) {
        @Override public Object readValue (Streamable.Input in) {
            return in.readDouble();
        }
        @Override public void writeValue (Streamable.Output out, Object value) {
            out.writeDouble((Double)value);
        }
    },
    STRING(9, String.class) {
        @Override public Object readValue (Streamable.Input in) {
            return in.readString();
        }
        @Override public void writeValue (Streamable.Output out, Object value) {
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
    public static ValueStreamer fromCode (byte code) {
        return _fromCode.get(code);
    }

    /** Returns the streamer associated with the supplied value. */
    public static ValueStreamer fromValue (Object value) {
        if (value == null) {
            return NULL;
        }
        ValueStreamer s = _fromType.get(value.getClass());
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

    ValueStreamer (int code, Class<?>... types) {
        this.code = (byte)code;
        this.types = types;
    }

    protected static Map<Byte, ValueStreamer> _fromCode = new HashMap<Byte, ValueStreamer>();
    protected static Map<Class<?>, ValueStreamer> _fromType =
        new HashMap<Class<?>, ValueStreamer>();
    static {
        for (ValueStreamer s : ValueStreamer.values()) {
            _fromCode.put(s.code, s);
            for (Class<?> type : s.types) {
                _fromType.put(type, s);
            }
        }
    }
}
