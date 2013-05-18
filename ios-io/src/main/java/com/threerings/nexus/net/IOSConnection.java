//
// Nexus iOS IO - I/O and network services for Nexus built on Monotouch via IKVM
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.net;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

import react.RPromise;

import com.threerings.nexus.io.ByteBufferInputStream;
import com.threerings.nexus.io.FrameReader;
import com.threerings.nexus.io.FramingOutputStream;
import com.threerings.nexus.io.IOSIO;
import com.threerings.nexus.io.Streamable;

import static com.threerings.nexus.util.Log.log;

/**
 * Implements a Nexus connection using IOS I/O.
 */
public class IOSConnection extends Connection
{
    /**
     * Creates an instance which will initiate a Nexus protocol connection with the specified host,
     * on the specified port.
     *
     * @param callback will be notified on connection completion, or failure.
     */
    public IOSConnection (String host, int port, Executor exec, RPromise<Connection> callback) {
        super(host);
        _exec = exec;
        // start the reader, which will connect and, if successful, create and start the writer
        _reader = new Reader(host, port, callback);
        _reader.start();
    }

    @Override // from Connection
    public synchronized void close () {
        // if we have no writer, we're still in the process of initializing ourselves, so we'll
        // trigger the reader to abort once it finishes initializing
        if (_writer == null) {
            _reader.abort();
        } else {
            // otherwise posting TERMINATE to the writer will trigger an orderly shutdown
            send(TERMINATE);
        }
    }

    @Override // from Connection
    protected void send (Upstream request) {
        _outq.offer(request);
    }

    @Override // from Connection
    protected void dispatch (Runnable run) {
        _exec.execute(run);
    }

    /**
     * Called by the reader, once we have established our connection with the server.
     */
    protected synchronized void connectionEstablished (ByteChannel channel) {
        // start up the writer thread now that we're connected
        _writer = new Writer(channel);
        _writer.start();
    }

    /**
     * Called by the reader when our socket is fully closed.
     */
    protected void connectionClosed () {
        log.info("Connection closed. TODO!");
        // TODO: report to our client (or observer) that we were shutdown
    }

    /**
     * Called by the reader or writer if socket I/O fails.
     */
    protected void connectionFailed (Throwable cause) {
        // close everything down; if the writer failed, this will already have happened, but if the
        // reader failed, this will trigger the writer to close the socket and tidy things up
        close();
        // TODO: report to our client (or observer) that we failed
    }

    protected static void closeChannel (ByteChannel channel) {
        try {
            channel.close();
        } catch (IOException ioe) {
            log.warning("Error closing socket", ioe);
        }
    }

    protected class Reader extends Thread {
        public Reader (String host, int port, RPromise<Connection> callback) {
            _host = host;
            _port = port;
            _callback = callback;
        }

        public void abort () {
            // the thread will shutdown either when it gets an EOF exception on the reading socket,
            // or next time through the loop if data is currently being read
            _running = false;
        }

        @Override public void run () {
            try {
                _channel = SocketByteChannel.open(_host, _port);

                // if we're shutdown while trying to establish a connection, we need to abort here
                // and close everything ourselves
                if (!_running) {
                    throw new IOException("Reader shutdown during initialization.");
                }

                // otherwise we're up and running and we can create our writer
                connectionEstablished(_channel);

                // let our callback know that we're ready to go
                log.info("Established server connection", "host", _host, "port", _port);
                _callback.succeed(IOSConnection.this);

            } catch (IOException ioe) {
                if (_channel != null) {
                    closeChannel(_channel);
                }
                _callback.fail(ioe);
                return;
            }

            // from here on out, we're connected, so network failures are reported differently
            try {
                while (_running) {
                    // try to read a full frame from the channel, if it's not full, simply loop
                    // back and keep reading until it is
                    ByteBuffer frame = _reader.readFrame(_channel);
                    if (frame == null) {
                        continue;
                    }

                    // decode the message from the frame data and pass it on
                    _bin.setBuffer(frame);
                    onReceive(_sin.<Downstream>readValue());
                    // TODO: if decoding fails, proceed to the next frame and keep going?
                }

            } catch (EOFException eofe) {
                connectionClosed();

            } catch (Throwable t) {
                log.warning("Error reading network data", t);
                connectionFailed(t);
            }
        }

        protected String _host;
        protected int _port;
        protected RPromise<Connection> _callback;

        protected volatile boolean _running = true;

        protected ByteChannel _channel;
        protected FrameReader _reader = new FrameReader();
        protected ByteBufferInputStream _bin = new ByteBufferInputStream();
        protected Streamable.Input _sin = IOSIO.newInput(_bin);
    }

    protected class Writer extends Thread {
        public Writer (ByteChannel channel) {
            _channel = channel;
        }

        @Override public void run () {
            while (true) {
                Upstream msg;
                try {
                    msg = _outq.take();
                } catch (InterruptedException ie) {
                    log.warning("Writer thread interrupted?");
                    continue;
                }
                if (msg == TERMINATE) {
                    break;
                }

                // TODO: outgoing rate throttling

                try {
                    // flatten the message into a byte array
                    _fout.prepareFrame();
                    _sout.writeValue(msg);

                    // frame and write the data to the output stream
                    ByteBuffer buffer = _fout.frameAndReturnBuffer();
                    int wrote = _channel.write(buffer);
                    if (wrote != buffer.limit()) {
                        log.warning("Failed to write complete message!", "msg", msg,
                                    "size", buffer.limit(), "wrote", wrote);
                    }

                } catch (Throwable t) {
                    log.warning("Error writing network data", "msg", msg, t);
                    connectionFailed(t);
                    break;
                }
            }

            // since we exist, we're responsible for closing the channel
            closeChannel(_channel);
        }

        protected ByteChannel _channel;
        protected FramingOutputStream _fout = new FramingOutputStream();
        protected Streamable.Output _sout = IOSIO.newOutput(_fout);
    }

    /** The executor used to dispatch events. */
    protected Executor _exec;

    /** A thread that handles reading incoming network data. */
    protected Reader _reader;

    /** A thread that handles writing outgoing network data. */
    protected Writer _writer;

    /** The message queue that holds our outgoing messages. */
    protected BlockingQueue<Upstream> _outq = new LinkedBlockingQueue<Upstream>();

    /** A marker instance used to terminate the writer thread. */
    protected static final Upstream TERMINATE = new Upstream() {
        public void dispatch (Handler h) { /* noop! */ }
    };
}
