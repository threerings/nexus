//
// Nexus JVMServer - server-side support for Nexus java.nio-based services
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.server;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.threerings.nexus.io.ByteBufferInputStream;
import com.threerings.nexus.io.FrameReader;
import com.threerings.nexus.io.FramingOutputStream;
import com.threerings.nexus.io.JVMIO;
import com.threerings.nexus.io.Streamable;
import com.threerings.nexus.net.Downstream;
import com.threerings.nexus.net.Upstream;

import static com.threerings.nexus.util.Log.log;

/**
 * Handles a connection to a single client.
 */
public class JVMServerConnection
    implements JVMConnectionManager.IOHandler, SessionManager.Output
{
    public JVMServerConnection (JVMConnectionManager cmgr, SocketChannel chan) {
        _cmgr = cmgr;
        _chan = chan;
    }

    public void setSession (SessionManager.Input input) {
        _input = input;
    }

    /**
     * Called by the connection manager I/O writer thread to instruct this connection to write its
     * pending outgoing messages.
     */
    public void writeMessages () {
        try {
            ByteBuffer frame;
            while ((frame = _outq.peek()) != null) {
                _chan.write(frame);
                if (frame.remaining() > 0) {
                    // partial write, requeue ourselves and finish the job later
                    _cmgr.requeueWriter(this);
                    return;
                }
                _outq.poll(); // remove fully written frame
            }

        } catch (NotYetConnectedException nyce) {
            // this means that our async connection is not quite complete, just requeue ourselves
            // and try again later
            _cmgr.requeueWriter(this);

        } catch (IOException ioe) {
            // because we may still be lingering in the connection manager's writable queue, clear
            // out our outgoing queue so that any final calls to writeMessages NOOP
            _outq.clear();
            // now let the usual suspects know that we failed
            _input.onSendError(ioe);
            onClose(ioe);
        }
    }

    // from interface SessionManager.Output
    public synchronized void send (Downstream msg) {
        // we may be called from many threads, this method is serialized to avoid conflicting
        // accesses to the output streams
        _fout.prepareFrame();
        _sout.writeValue(msg);
        ByteBuffer buffer = _fout.frameAndReturnBuffer();

        // as we do not control the supplied buffer, and we may not be able to write it fully to
        // the outgoing socket, we have to copy it; we could also take this opportunity to copy it
        // into a direct buffer, which may improve I/O performance; someday perhaps we'll measure
        // performance with and without such an optimization
        ByteBuffer frame = ByteBuffer.allocate(buffer.limit()-buffer.position());
        frame.put(buffer);
        frame.flip();

        // add this frame to our output queue and tell the connection manager that we're writable
        _outq.offer(frame);
        _cmgr.queueWriter(this);
    }

    // from interface SessionManager.Output
    public void disconnect () {
        try {
            _chan.close();
        } catch (IOException ioe) {
            log.warning("Failed to close socket", "socket", _chan, ioe);
        }
    }

    // from interface JVMConnectionManager.IOHandler
    public void handleIO () {
        try {
            // keep reading and processing frames while we have them
            ByteBuffer frame;
            while ((frame = _reader.readFrame(_chan)) != null) {
                try {
                    _bin.setBuffer(frame);
                    _input.onMessage(_sin.<Upstream>readValue());
                } catch (Throwable t) {
                    log.warning("Failure decoding incoming message", "chan", _chan, t);
                }
            }

        } catch (EOFException eofe) {
            _input.onDisconnect();
            onClose(null);

        } catch (IOException ioe) {
            _input.onReceiveError(ioe);
            onClose(ioe);
        }
    }

    protected void onClose (IOException cause) {
        if (_chan == null) return; // no double closeage
        try {
            _chan.close();
        } catch (IOException ioe) {
            log.warning("Failed to close socket channel", "chan", _chan, "error", ioe);
        }
        _cmgr.connectionClosed(_chan, cause);
        _chan = null;
    }

    protected final JVMConnectionManager _cmgr;
    protected SocketChannel _chan;
    protected SessionManager.Input _input;

    // these are used for message I/O
    protected final ByteBufferInputStream _bin = new ByteBufferInputStream();
    protected final Streamable.Input _sin = JVMIO.newInput(_bin);
    protected final FramingOutputStream _fout = new FramingOutputStream();
    protected final Streamable.Output _sout = JVMIO.newOutput(_fout);

    protected final FrameReader _reader = new FrameReader();
    protected final Queue<ByteBuffer> _outq = new ConcurrentLinkedQueue<ByteBuffer>();
}
