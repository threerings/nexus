//
// Nexus GWTIO - I/O and network services for Nexus built on GWT and WebSockets
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.io;

import com.google.gwt.lang.LongLib;

import com.threerings.nexus.distrib.DService;
import com.threerings.nexus.distrib.NexusService;

/**
 * Handles the encoding of streamable data into a string payload (for delivery to the server).
 */
class ClientOutput extends Streamable.Output
{
    /** The character used to separate values in an encoded payload. */
    public static final char SEPARATOR = '|';

    public ClientOutput (GWTIO.Serializer szer, StringBuffer output)
    {
        _szer = szer;
        _output = output;
    }

    @Override public void writeBoolean (boolean value)
    {
        append(value ? "1" : "0");
    }

    @Override public void writeByte (byte value)
    {
        append(String.valueOf(value));
    }

    @Override public void writeShort (short value)
    {
        append(String.valueOf(value));
    }

    @Override public void writeChar (char value)
    {
        // use an int, to avoid having to cope with escaping things
        writeInt((int)value);
    }

    @Override public void writeInt (int value)
    {
        append(String.valueOf(value));
    }

    @Override public void writeLong (long value)
    {
        append(LongLib.toBase64(value));
    }

    @Override public void writeFloat (float value)
    {
        writeDouble(value);
    }

    @Override public void writeDouble (double value)
    {
        append(String.valueOf(value));
    }

    @Override public void writeString (String value)
    {
        if (value == null) {
            writeBoolean(false);
        } else {
            writeBoolean(true);
            append(quoteString(value));
        }
    }

    @Override public void writeEnum (Enum<?> value)
    {
        if (value == null) writeShort((short)0);
        else {
            writeShort(_szer.getCode(value.getClass()));
            writeString(value.name()); // TODO: use ordinal()
        }
    }

    @Override public void writeClass (Class<? extends Streamable> clazz)
    {
        writeShort(_szer.getCode(clazz));
    }

    @Override public void writeService (DService<?> service)
    {
        writeShort(_szer.getServiceCode(service.getServiceClass()));
    }

    @Override protected <T> Streamer<T> writeStreamer (T value)
    {
        return _szer.<T>writeStreamer(this, value);
    }

    protected String quoteString (String value)
    {
        return value; // TODO
    }

    protected void append (String token)
    {
        _output.append(token);
        _output.append(SEPARATOR);
    }

    protected final GWTIO.Serializer _szer;
    protected final StringBuffer _output;
}
