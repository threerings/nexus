//
// $Id$

package com.samskivert.nexus.io;

import com.google.gwt.lang.LongLib;

import com.samskivert.nexus.distrib.NexusService;

/**
 * Handles the encoding of streamable data into a string payload (for delivery to the server).
 */
class ClientOutput extends Streamable.Output
{
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
            writeNonNullString(value);
        }
    }

    @Override public void writeClass (Class<? extends Streamable> clazz)
    {
        writeShort(_szer.getCode(clazz));
    }

    @Override public void writeService (Class<? extends NexusService> clazz)
    {
        writeShort(_szer.getServiceCode(clazz));
    }

    @Override protected <T> Streamer<T> writeStreamer (T value)
    {
        return _szer.<T>writeStreamer(this, value);
    }

    protected void writeNonNullString (String value)
    {
        append(quoteString(value));
    }

    protected String quoteString (String value)
    {
        return value; // TODO
    }

    protected void append (String token)
    {
        _output.append(token);
        _output.append(RPC_SEPARATOR_CHAR);
    }

    protected final GWTIO.Serializer _szer;
    protected final StringBuffer _output;

    protected static final char RPC_SEPARATOR_CHAR = '|';
    protected static final Streamer<Void> NULL_STREAMER = new Streamers.Streamer_Null();
}
