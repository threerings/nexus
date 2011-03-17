//
// $Id$
//
// Nexus JVMIO - I/O and network services for Nexus built on java.nio
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.net;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.samskivert.nexus.distrib.Dispatcher;
import com.samskivert.nexus.io.FramedInputStream;
import com.samskivert.nexus.io.FramingOutputStream;
import com.samskivert.nexus.io.JVMIO;
import com.samskivert.nexus.io.Streamable;
import com.samskivert.nexus.util.Callback;

import static com.samskivert.nexus.util.Log.log;

/**
 * Implements a Nexus connection using JVM I/O.
 */
public class JVMConnection extends Connection
{
    /**
     * Creates an instance which will initiate a Nexus protocol connection with the specified host,
     * on the specified port.
     *
     * @param callback will be notified on connection completion, or failure.
     */
    public JVMConnection (String host, int port, Dispatcher dispatcher, Callback<Connection> callback)
    {
        super(host, dispatcher);
        // start the reader, which will connect and, if successful, create and start the writer
        _reader = new Reader(host, port, callback);
        _reader.start();
    }

    @Override // from Connection
    public synchronized void close ()
    {
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
    protected void send (Upstream request)
    {
        _outq.offer(request);
    }

    /**
     * Called by the reader, once we have established our connection with the server.
     */
    protected synchronized void connectionEstablished (SocketChannel channel)
    {
        // start up the writer thread now that we're connected
        _writer = new Writer(channel);
        _writer.start();
    }

    /**
     * Called by the reader when our socket is fully closed.
     */
    protected void connectionClosed ()
    {
        // TODO: report to our client (or observer) that we were shutdown
    }

    /**
     * Called by the reader or writer if socket I/O fails.
     */
    protected void connectionFailed (Throwable cause)
    {
        // close everything down; if the writer failed, this will already have happened, but if the
        // reader failed, this will trigger the writer to close the socket and tidy things up
        close();
        // TODO: report to our client (or observer) that we failed
    }

    protected static void closeChannel (SocketChannel channel)
    {
        try {
            channel.close();
        } catch (IOException ioe) {
            log.warning("Error closing socket", ioe);
        }
    }

    protected class Reader extends Thread
    {
        public Reader (String host, int port, Callback<Connection> callback) {
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
                // resolve the hostname of our target and establish a connection thereto
                InetAddress addr = InetAddress.getByName(_host);
                _channel = SocketChannel.open(new InetSocketAddress(addr, _port));
                _channel.configureBlocking(true);

                // if we're shutdown while trying to establish a connection, we need to abort here
                // and close everything ourselves
                if (!_running) {
                    throw new IOException("Reader shutdown during initialization.");
                }

                // otherwise we're up and running and we can create our writer
                connectionEstablished(_channel);

            } catch (IOException ioe) {
                if (_channel != null) {
                    closeChannel(_channel);
                }
                _callback.onFailure(ioe);
                return;
            }

            // from here on out, we're connected, so network failures are reported differently
            try {
                while (_running) {
                    // try to read a full frame from the channel, if it's not full, simply loop
                    // back and keep reading until it is
                    if (!_fin.readFrame(_channel)) {
                        continue;
                    }

                    // decode the message from the frame data and pass it on
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
        protected Callback<Connection> _callback;

        protected volatile boolean _running = true;

        protected SocketChannel _channel;
        protected FramedInputStream _fin = new FramedInputStream();
        protected Streamable.Input _sin = JVMIO.newInput(_fin);
    }

    protected class Writer extends Thread
    {
        public Writer (SocketChannel channel) {
            _channel = channel;
        }

        @Override public void run () {
            while (true) {
                Upstream msg = _outq.poll();
                if (msg == TERMINATE) {
                    break;
                }

                // TODO: outgoing rate throttling

                try {
                    // flatten the message into a byte array
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

        protected SocketChannel _channel;
        protected FramingOutputStream _fout = new FramingOutputStream();
        protected Streamable.Output _sout = JVMIO.newOutput(_fout);
    }

    /** A thread that handles reading incoming network data. */
    protected Reader _reader;

    /** A thread that handles writing outgoing network data. */
    protected Writer _writer;

    /** The message queue that holds our outgoing messages. */
    protected Queue<Upstream> _outq = new ConcurrentLinkedQueue<Upstream>();

    /** A marker instance used to terminate the writer thread. */
    protected static final Upstream TERMINATE = new Upstream() {
        public void dispatch (Handler h) { /* noop! */ }
    };
}
