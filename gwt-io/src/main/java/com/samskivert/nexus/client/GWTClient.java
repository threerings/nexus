//
// $Id$
//
// Nexus GWTIO - I/O and network services for Nexus built on GWT and WebSockets
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.client;

import com.samskivert.nexus.io.GWTIO;
import com.samskivert.nexus.net.Connection;
import com.samskivert.nexus.net.GWTConnection;
import com.samskivert.nexus.util.Callback;

/**
 * Provides a Nexus client using WebSockets and GWT-based I/O.
 */
public class GWTClient extends NexusClient
{
    /**
     * Creates a Nexus client which will use WebSockets to connect to hosts.
     * @param port the port on which to make WebSocket connections.
     * @param szer the serializer that knows about all types that will cross the wire.
     */
    public static NexusClient create (int port, GWTIO.Serializer szer)
    {
        return new GWTClient(port, szer);
    }

    protected GWTClient (int port, GWTIO.Serializer szer)
    {
        _port = port;
        _szer = szer;
    }

    protected void connect (String host, Callback<Connection> callback)
    {
        new GWTConnection(host, _port, _szer, callback);
    }

    protected int _port;
    protected GWTIO.Serializer _szer;
}
