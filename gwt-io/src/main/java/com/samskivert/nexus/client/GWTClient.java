//
// $Id$

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
     * @param szer the serializer that knows about all types that will cross the wire.
     */
    public static NexusClient create (GWTIO.Serializer szer)
    {
        return new GWTClient(szer);
    }

    protected GWTClient (GWTIO.Serializer szer)
    {
        _szer = szer;
    }

    protected void connect (String host, Callback<Connection> callback)
    {
        new GWTConnection(host, _szer, callback);
    }

    protected GWTIO.Serializer _szer;
}
