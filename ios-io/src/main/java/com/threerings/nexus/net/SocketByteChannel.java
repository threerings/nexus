//
// Nexus iOS IO - I/O and network services for Nexus built on Monotouch via IKVM
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ReadableByteChannel;

import cli.System.Net.Dns;
import cli.System.Net.IPAddress;
import cli.System.Net.IPEndPoint;
import cli.System.Net.Sockets.AddressFamily;
import cli.System.Net.Sockets.ProtocolType;
import cli.System.Net.Sockets.Socket;
import cli.System.Net.Sockets.SocketFlags;
import cli.System.Net.Sockets.SocketType;

import static com.threerings.nexus.util.Log.log;

public class SocketByteChannel
    implements ByteChannel
{
    public static SocketByteChannel open (String host, int port)
        throws IOException
    {
        Socket socket = new Socket(AddressFamily.wrap(AddressFamily.InterNetwork),
            SocketType.wrap(SocketType.Stream), ProtocolType.wrap(ProtocolType.Tcp));

        IPAddress serverAddress = Dns.GetHostEntry(host).get_AddressList()[0];
        IPEndPoint serverEndpoint = new IPEndPoint(serverAddress, port);
        try {
            socket.Connect(serverEndpoint);
        } catch (Throwable t) {
            log.warning("Failed to connect", t);
            throw new IOException(t);
        }
        return new SocketByteChannel(socket);
    }

    public SocketByteChannel (Socket socket)
    {
        _socket = socket;
    }

    @Override public int read (ByteBuffer buf)
        throws IOException
    {
        if (!_socket.get_Connected()) {
            log.warning("Cannot read from an unconnected socket");
            return -1;
        }
        int read = _socket.Receive(buf.array(), buf.position(), buf.remaining(),
            SocketFlags.wrap(SocketFlags.None));
        // since we're dumping into the backing array directly, we must update the position
        // ourselves
        buf.position(buf.position() + read);
        return read;
    }

    @Override public int write (ByteBuffer buf)
        throws IOException
    {
        try {
            return _socket.Send(buf.array(), 0, buf.limit(), SocketFlags.wrap(SocketFlags.None));

        } catch (Throwable t) {
            throw new IOException(t);
        }
    }

    @Override public boolean isOpen ()
    {
        return _socket.get_Connected();
    }

    @Override public void close ()
        throws IOException
    {
        _socket.Close();
    }

    protected Socket _socket;
}
