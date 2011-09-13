//
// Nexus GWTServer - server-side support for Nexus GWT/WebSockets services
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.io;

import com.threerings.nexus.distrib.DService;
import com.threerings.nexus.distrib.NexusService;

import com.google.gwt.user.server.Base64Utils;

/**
 * Handles the encoding of streamable data into a string payload (for delivery to the client).
 */
public class ServerOutput extends Streamable.Output
{
    public ServerOutput (GWTIO.Serializer szer, GWTServerIO.PayloadBuffer output)
    {
        _szer = szer;
        _output = output;
        _output.prepare();
    }

    @Override public void writeBoolean (boolean value)
    {
        _output._buffer.append(value ? "1" : "0");
        _output.appendSeparator();
    }

    @Override public void writeByte (byte value)
    {
        _output._buffer.append(String.valueOf(value));
        _output.appendSeparator();
    }

    @Override public void writeShort (short value)
    {
        _output._buffer.append(String.valueOf(value));
        _output.appendSeparator();
    }

    @Override public void writeChar (char value)
    {
        // use an int, to avoid having to cope with escaping things
        writeInt((int)value);
    }

    @Override public void writeInt (int value)
    {
        _output._buffer.append(String.valueOf(value));
        _output.appendSeparator();
    }

    @Override public void writeLong (long value)
    {
        _output._buffer.append('\'').append(Base64Utils.toBase64(value)).append('\'');
        _output.appendSeparator();
    }

    @Override public void writeFloat (float value)
    {
        writeDouble(value);
    }

    @Override public void writeDouble (double value)
    {
        _output._buffer.append(String.valueOf(value));
        _output.appendSeparator();
    }

    @Override public void writeString (String value)
    {
        if (value == null) {
            _output._buffer.append("null");
        } else {
            _output._buffer.append('\"').append(escapeString(value)).append('\"');
        }
        _output.appendSeparator();
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

    protected String escapeString (String value)
    {
        return value; // TODO
    }

    protected final GWTIO.Serializer _szer;
    protected final GWTServerIO.PayloadBuffer _output;
}
