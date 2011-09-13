//
// Nexus GWTServer - server-side support for Nexus GWT/WebSockets services
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.io;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.google.gwt.user.server.Base64Utils;

import com.threerings.nexus.distrib.NexusService;

/**
 * Handles the decoding of an input payload (from the client) into proper values.
 */
public class ServerInput extends Streamable.Input
{
    public ServerInput (GWTIO.Serializer szer, final String data)
    {
        _szer = szer;

        // create an iterator that will extract delimited values
        _valiter = new Iterator<String>() {
            public boolean hasNext () {
                return (_nextIdx != -1);
            }
            public String next () {
                if (_nextIdx == -1) {
                    throw new NoSuchElementException();
                }
                String value = data.substring(_curIdx, _nextIdx);
                _curIdx = _nextIdx+1;
                _nextIdx = data.indexOf(ClientOutput.SEPARATOR, _curIdx);
                return value;
            }
            public void remove () {
                throw new UnsupportedOperationException();
            }
            protected int _curIdx = 0;
            protected int _nextIdx = data.indexOf(ClientOutput.SEPARATOR);
        };
    }

    @Override public boolean readBoolean ()
    {
        return _valiter.next().equals("1");
    }

    @Override public byte readByte ()
    {
        return Byte.parseByte(_valiter.next());
    }

    @Override public short readShort ()
    {
        return Short.parseShort(_valiter.next());
    }

    @Override public char readChar ()
    {
        return (char)readInt();
    }

    @Override public int readInt ()
    {
        return Integer.parseInt(_valiter.next());
    }

    @Override public long readLong ()
    {
        return Base64Utils.longFromBase64(_valiter.next());
    }

    @Override public float readFloat ()
    {
        return (float)readDouble();
    }

    @Override public double readDouble ()
    {
        return Double.parseDouble(_valiter.next());
    }

    @Override public String readString ()
    {
        return readBoolean() ? _valiter.next() : null; // TODO: is wrong?
    }

    @Override public <T extends Streamable> Class<T> readClass ()
    {
        @SuppressWarnings("unchecked") Class<T> c = (Class<T>)_szer.getClass(readShort());
        return c;
    }

    @Override protected <T> Streamer<T> readStreamer ()
    {
        @SuppressWarnings("unchecked") Streamer<T> s = (Streamer<T>)_szer.getStreamer(readShort());
        return s;
    }

    @Override protected <T extends NexusService> ServiceFactory<T> readServiceFactory ()
    {
        @SuppressWarnings("unchecked") ServiceFactory<T> sf =
            (ServiceFactory<T>)_szer.getServiceFactory(readShort());
        return sf;
    }

    protected final GWTIO.Serializer _szer;
    protected final Iterator<String> _valiter;
}
