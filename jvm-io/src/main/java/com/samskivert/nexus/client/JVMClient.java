//
// $Id$
//
// Nexus JVMIO - I/O and network services for Nexus built on java.nio
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.client;

import java.awt.EventQueue;
import java.util.concurrent.Executor;

import com.samskivert.nexus.distrib.NexusEvent;
import com.samskivert.nexus.net.Connection;
import com.samskivert.nexus.net.JVMConnection;
import com.samskivert.nexus.util.Callback;

/**
 * Provides a Nexus client based on JVM-based I/O.
 */
public class JVMClient extends NexusClient
{
    /**
     * Creates a Nexus client.
     * @param exec the executor on which to dispatch distributed object events.
     * @param port the port on which to connect to servers.
     */
    public static NexusClient create (Executor exec, int port)
    {
        return new JVMClient(exec, port);
    }

    /**
     * Creates a Nexus client that will dispatch events on the AWT event queue.
     * @param port the port on which to connect to servers.
     */
    public static NexusClient create (int port)
    {
        return create(new Executor() {
            public void execute (Runnable command) {
                EventQueue.invokeLater(command);
            }
        }, port);
    }

    protected JVMClient (Executor exec, int port)
    {
        _exec = exec;
        _port = port;
    }

    protected void connect (String host, Callback<Connection> callback)
    {
        new JVMConnection(host, _port, _exec, callback);
    }

    protected Executor _exec;
    protected int _port;
}
