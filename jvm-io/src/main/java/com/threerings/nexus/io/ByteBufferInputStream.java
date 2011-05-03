//
// Nexus JVMIO - I/O and network services for Nexus built on java.nio
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Provides an {@link InputStream} interface to a {@link ByteBuffer}.
 */
public class ByteBufferInputStream extends InputStream
{
    /**
     * Configures the buffer from which data should be read.
     */
    public void setBuffer (ByteBuffer buffer)
    {
        _buffer = buffer;
    }

    @Override
    public int read () throws IOException
    {
        // note: we don't rely on "-1 signals EOF" behavior, so we just let read() fail if one
        // attempts to read past the end of the buffer
        return _buffer.get() & 0xFF;
    }

    @Override
    public int read (byte b[], int off, int len) throws IOException
    {
        int count = Math.min(_buffer.remaining(), len);
        _buffer.get(b, off, count);
        return count;
    }

    @Override
    public long skip (long n) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int available () throws IOException
    {
        return _buffer.remaining();
    }

    protected ByteBuffer _buffer;
}
