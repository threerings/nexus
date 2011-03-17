//
// $Id$
//
// Nexus JVMIO - I/O and network services for Nexus built on java.nio
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * The framed input stream reads input that was framed by a framing output stream. Framing in this
 * case simply means writing the length of the frame followed by the data associated with the frame
 * so that an entire frame can be loaded from the network layer before any higher layer attempts to
 * process it. Additionally, any failure in decoding a frame won't result in the entire stream
 * being skewed due to the remainder of the undecoded frame remaining in the input stream.
 *
 * <p>The framed input stream reads an entire frame worth of data into its internal buffer when
 * <code>readFrame()</code> is called. It then behaves as if this is the only data available on the
 * stream (meaning that when the data in the frame is exhausted, it will behave as if the end of
 * the stream has been reached). The buffer can only contain a single frame at a time, so any data
 * left over from a previous frame will disappear when <code>readFrame()</code> is called again.
 * </p>
 *
 * <p><em>Note:</em> The framing input stream does not synchronize reads from its internal buffer.
 * It is intended to only be accessed from a single thread. </p>
 */
public class FramedInputStream extends InputStream
{
    public FramedInputStream ()
    {
        _buffer = ByteBuffer.allocate(INITIAL_BUFFER_CAPACITY);
    }

    /**
     * Reads a frame from the provided channel, appending to any partially read frame. If the
     * entire frame data is not yet available, <code>readFrame</code> will return false, otherwise
     * true.
     *
     * <p> <em>Note:</em> when this method returns true, it is required that the caller read
     * <em>all</em> of the frame data from the stream before again calling {@link #readFrame} as
     * the previous frame's data will be elimitated upon the subsequent call. </p>
     *
     * @return true if the entire frame has been read, false if the buffer contains only a partial
     * frame.
     */
    public boolean readFrame (ReadableByteChannel source)
        throws IOException
    {
        // flush data from any previous frame from the buffer
        if (_buffer.limit() == _length) {
            // this will remove the old frame's bytes from the buffer, shift our old data to the
            // start of the buffer, position the buffer appropriately for appending new data onto
            // the end of our existing data, and set the limit to the capacity
            _buffer.limit(_have);
            _buffer.position(_length);
            _buffer.compact();
            _have -= _length;

            // we may have picked up the next frame in a previous read, so try decoding the length
            // straight away
            _length = decodeLength();
        }

        // we may already have the next frame entirely in the buffer from a previous read
        if (checkForCompleteFrame()) {
            return true;
        }

        // read whatever data we can from the source
        do {
            int got = source.read(_buffer);
            if (got == -1) {
                throw new EOFException();
            }
            _have += got;

            if (_length == -1) {
                // if we didn't already have our length, see if we have enough data to obtain it
                _length = decodeLength();
            }

            // if there's room remaining in the buffer, that means we've read all there is to read,
            // so we can move on to inspecting what we've got
            if (_buffer.remaining() > 0) {
                break;
            }

            // if the buffer happened to be exactly as long as we needed, we need to break as well
            if ((_length > 0) && (_have >= _length)) {
                break;
            }

            // otherwise, we've filled up our buffer as a result of this read, expand it and try
            // reading some more
            ByteBuffer newbuf = ByteBuffer.allocate(_buffer.capacity() << 1);
            newbuf.put((ByteBuffer)_buffer.flip());
            _buffer = newbuf;

            // don't let things grow without bounds
        } while (_buffer.capacity() < MAX_BUFFER_CAPACITY);

        // finally check to see if there's a complete frame in the buffer
        return checkForCompleteFrame();
    }

    @Override
    public int read ()
    {
        return (_buffer.remaining() > 0) ? (_buffer.get() & 0xFF) : -1;
    }

    @Override
    public int read (byte[] b, int off, int len)
    {
        // if they want no bytes, we give them no bytes; this is purportedly the right thing to do
        // regardless of whether we're at EOF or not
        if (len == 0) {
            return 0;
        }

        // trim the amount to be read to what is available; if they wanted bytes and we have none,
        // return -1 to indicate EOF
        if ((len = Math.min(len, _buffer.remaining())) == 0) {
            return -1;
        }

        _buffer.get(b, off, len);
        return len;
    }

    @Override
    public long skip (long n)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int available ()
    {
        return _buffer.remaining();
    }

    @Override
    public boolean markSupported ()
    {
        return false;
    }

    @Override
    public void mark (int readAheadLimit)
    {
        // not supported; do nothing
    }

    @Override
    public void reset ()
    {
        // position our buffer at the beginning of the frame data
        _buffer.position(HEADER_SIZE);
    }

    /**
     * Decodes and returns the length of the current frame from the buffer, if possible. Returns -1
     * otherwise.
     */
    protected final int decodeLength ()
    {
        // if we don't have enough bytes to determine our frame size, stop here and let the caller
        // know that we're not ready
        if (_have < HEADER_SIZE) {
            return -1;
        }

        // decode the frame length
        _buffer.rewind();
        int length = (_buffer.get() & 0xFF) << 24;
        length += (_buffer.get() & 0xFF) << 16;
        length += (_buffer.get() & 0xFF) << 8;
        length += (_buffer.get() & 0xFF);
        _buffer.position(_have);

        return length;
    }

    /**
     * Returns true if a complete frame is in the buffer, false otherwise. If a complete frame is
     * in the buffer, the buffer will be prepared to deliver that frame via our {@link InputStream}
     * interface.
     */
    protected final boolean checkForCompleteFrame ()
    {
        if (_length == -1 || _have < _length) {
            return false;
        }

        // prepare the buffer such that this frame can be read
        _buffer.position(HEADER_SIZE);
        _buffer.limit(_length);
        return true;
    }

    /** The buffer in which we maintain our frame data. */
    protected ByteBuffer _buffer;

    /** The length of the current frame being read. */
    protected int _length = -1;

    /** The number of bytes total that we have in our buffer (these bytes may comprise more than
     * one frame). */
    protected int _have = 0;

    /** The size of the frame header (a 32-bit integer). */
    protected static final int HEADER_SIZE = 4;

    /** The default initial size of the internal buffer. */
    protected static final int INITIAL_BUFFER_CAPACITY = 32;

    /** The maximum allowed buffer size. No need to get out of hand. */
    protected static final int MAX_BUFFER_CAPACITY = 512 * 1024;
}
