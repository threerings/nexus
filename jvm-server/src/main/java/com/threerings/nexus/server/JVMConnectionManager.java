//
// $Id$
//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import static com.threerings.nexus.util.Log.log;

/**
 * Handles listening for Nexus connections and reading and writing over the sockets. Interacts with
 * a {@link SessionManager} to source and sink messages.
 */
public class JVMConnectionManager
{
    /**
     * Creates a connection manager that will listen for connections on the specified host/port
     * combination and establish sessions with the supplied session manager.
     */
    public JVMConnectionManager (SessionManager smgr)
        throws IOException
    {
        _smgr = smgr;
        _selector = Selector.open();
    }

    /**
     * Binds a listening socket on the specified host and port.
     * @param bindHost the address on which to listen, or null to listen on 0.0.0.0.
     * @param bindPort the port on which to listen.
     * @throws IOException if a failure occurs binding the socket.
     */
    public void listen (String bindHost, int bindPort) throws IOException
    {
        final ServerSocketChannel ssocket = ServerSocketChannel.open();
        ssocket.configureBlocking(false);
        InetSocketAddress addr = Strings.isNullOrEmpty(bindHost) ?
            new InetSocketAddress(bindPort) : new InetSocketAddress(bindHost, bindPort);
        ssocket.socket().bind(addr);

        SelectionKey key = ssocket.register(_selector, SelectionKey.OP_ACCEPT);
        key.attach(new IOHandler() {
            public void handleIO () {
                handleAccept(ssocket);
            }
        });
        _ssocks.add(ssocket);

        log.info("Server listening on " + addr);
    }

    /**
     * Starts the I/O thread that will handle reading from and writing to all sockets.
     */
    public void start ()
    {
        Preconditions.checkState(_state == State.INIT, "Must not call more than once.");
        // note that we're now running (in reading and writing state)
        _state = State.READ_WRITE;

        // start the I/O reader thread
        (_reader = new Thread("JVMConnectionManager I/O reader") {
            public void run () {
                while (_state == JVMConnectionManager.State.READ_WRITE && readLoop()) { /* loop! */ }
            };
        }).start();

        // start the I/O writer thread
        (_writer = new Thread("JVMConnectionManager I/O writer") {
            public void run () {
                while (_state != JVMConnectionManager.State.TERMINATED) {
                    writeLoop();
                }
            };
        }).start();
    }

    /**
     * Unbinds all listening sockets and instructs our I/O thread to enter the write-only mode.
     * This allows any pending outgoing messages to be sent before a call to {@link #shutdown}
     * fully terminates the I/O thread.
     */
    public void disconnect ()
    {
        Preconditions.checkState(
            _state == State.READ_WRITE, "Must call start() prior to disconnect().");

        // close all of our listening sockets
        for (ServerSocketChannel ssock : _ssocks) {
            try {
                ssock.close();
            } catch (IOException ioe) {
                log.warning("Failed to close listening socket", "socket", ssock, ioe);
            }
        }
        _ssocks.clear();

        // tell the I/O thread that we're doing no more accepting or reading
        _state = State.WRITE_ONLY;
        // wake up the reader thread to ensure that it shuts down
        _selector.wakeup();
    }

    /**
     * Shuts down the writer thread and completes the termination of the connection manager.
     */
    public void shutdown ()
    {
        Preconditions.checkState(
            _state == State.WRITE_ONLY, "Must call disconnect() prior to shutdown().");

        _state = State.TERMINATED;
        // trigger the writer thread to shutdown
        _outq.offer(TERMINATOR);
        // TODO: close all of our client sockets?
    }

    /**
     * Returns true if neither the reader nor writer thread are any longer executing. This is only
     * really used by the test framework to ensure that we don't break the shutdown process.
     */
    public boolean isTerminated ()
    {
        return (_state == State.TERMINATED) && !_reader.isAlive() && !_writer.isAlive();
    }

    /**
     * Called when one of our listening sockets has a connection ready to be accepted.
     */
    protected void handleAccept (ServerSocketChannel ssock)
    {
        SocketChannel chan = null;
        try {
            chan = ssock.accept();
            if (chan == null) {
                log.warning("Listen socket reported ready, but has no acceptable socket?",
                            "ssock", ssock);
                return;
            }
            chan.configureBlocking(false);
            SelectionKey key = chan.register(_selector, SelectionKey.OP_READ);
            JVMServerConnection conn = new JVMServerConnection(this, chan);
            String ipaddr = chan.socket().getInetAddress().toString();
            SessionManager.Input sess = _smgr.createSession(ipaddr, conn);
            conn.setSession(sess);
            key.attach(conn);
            log.info("Started new session " + sess);

        } catch (IOException ioe) {
            log.warning("Failure accepting connected socket", "ssock", ssock, ioe);
            if (chan != null) {
                try {
                    chan.socket().close();
                } catch (IOException iioe) {
                    log.warning("Failure closing aborted connection", "chan", chan, "error", iioe);
                }
            }
        }
    }

    /**
     * Queues up a connection that has outgoing messages to send. The I/O writer thread will
     * process this connection on its next iteration through the write loop.
     */
    protected void queueWriter (JVMServerConnection conn)
    {
        _outq.offer(conn);
    }

    /**
     * Queues up a connection that has pending writes but is not currently able to write them (due
     * to full outgoing socket buffers or a still pending asynchronous connection).
     */
    protected void requeueWriter (JVMServerConnection conn)
    {
        // TODO: introduce a delay in case we have nothing else to do but spin trying to write this
        // full writer?
        queueWriter(conn);
    }

    /**
     * Called by a connection when it has been closed (in an orderly fashion, or due to failure).
     * @param cause the cause of failure, if the shutdown was not orderly, null otherwise.
     */
    protected void connectionClosed (SocketChannel chan, IOException cause)
    {
        log.info("Connection closed", "addr", chan.socket().getInetAddress(), "cause", cause);
        // the key itself is automatically canceled when the socket is closed, so we don't need to
        // remove it from our selector, and the handler is attached to they key, so garbage
        // collection cleans everything up for us
    }

    /**
     * Handles a single iteration of the reader thread loop. The reader thread calls this method
     * over and over again repeatedly until it returns false or we transition to the write-only or
     * terminated state.
     */
    protected boolean readLoop ()
    {
        int eventCount; // TODO: what do we need this for?
        try {
            eventCount = _selector.select();
        } catch (IOException ioe) {
            log.warning("Failure selecting", ioe);
            return true; // TODO: terminate reader thread on too many successive errors
        }

        // process all of the channels that are ready for action
        for (SelectionKey key : _selector.selectedKeys()) {
            IOHandler handler = (IOHandler)key.attachment();
            if (handler == null) {
                log.warning("Received network event with no handler",
                            "key", key, "ops", key.readyOps());
                key.cancel();
            } else {
                try {
                    handler.handleIO();
                } catch (Throwable t) {
                    log.warning("IOHandler failure", t);
                }
            }
        }

        // now that we've handled all of the ready keys, we must clear the selected set
        _selector.selectedKeys().clear();

        return true;
    }

    protected void writeLoop ()
    {
        // TODO: we could create a separate selector for postponed writes, which would allow us to
        // block, waiting for a full outgoing channel to have room for its pending writes; this
        // would prevent degeneration into a spinning loop, attempting to write to full sockets in
        // a server with no other writes to perform
        try {
            // handle the next connection that has data to write
            JVMServerConnection conn = _outq.take();
            if (conn != TERMINATOR) {
                conn.writeMessages();
            }
        } catch (InterruptedException ie) {
            log.warning("Writer thread interrupted?");
            // return normally, and keep looping
        }
    }

    protected interface IOHandler {
        void handleIO ();
    }

    protected SessionManager _smgr;
    protected Selector _selector;
    protected Thread _reader, _writer;

    /** Our list of listening sockets. */
    protected List<ServerSocketChannel> _ssocks = Lists.newArrayList();

    /** A queue of connections that have outgoing messages. */
    protected BlockingQueue<JVMServerConnection> _outq =
        new LinkedBlockingQueue<JVMServerConnection>();

    /** Used to proceed through our lifecycle. See {@link #state}. */
    protected enum State { INIT, READ_WRITE, WRITE_ONLY, TERMINATED };

    /** Used to control the behavior of (and eventually terminate) the I/O thread. */
    protected volatile State _state = State.INIT;

    /** A sentinel object used to ensure that the writer thread shuts down. */
    protected final JVMServerConnection TERMINATOR = new JVMServerConnection(null, null);
}
