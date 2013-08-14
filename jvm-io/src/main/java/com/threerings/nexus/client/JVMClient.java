//
// Nexus JVMIO - I/O and network services for Nexus built on java.nio
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.client;

import java.awt.EventQueue;
import java.util.concurrent.Executor;

import react.RPromise;

import com.threerings.nexus.net.Connection;
import com.threerings.nexus.net.JVMConnection;

/**
 * Provides a Nexus client based on JVM-based I/O.
 */
public class JVMClient extends NexusClient
{
    /** Creates an executor that executes commands on the AWT thread. */
    public static Executor awtExecutor () {
        return new Executor() {
            public void execute (Runnable command) {
                EventQueue.invokeLater(command);
            }
        };
    }

    /** @deprecated Use JVMClient constructor. */
    @Deprecated
    public static NexusClient create (Executor exec, int port) {
        return new JVMClient(exec, port);
    }

    /** @deprecated Use JVMClient constructor and {@link #awtExecutor}. */
    @Deprecated
    public static NexusClient create (int port) {
        return create(awtExecutor(), port);
    }

    /**
     * Creates a Nexus client.
     * @param exec the executor on which to dispatch distributed object events.
     * @param port the port on which to connect to servers.
     */
    public JVMClient (Executor exec, int port) {
        _exec = exec;
        _port = port;
    }

    @Override protected int port () {
        return _port;
    }

    @Override protected void connect (String host, RPromise<Connection> callback) {
        new JVMConnection(log(), host, _port, _exec, callback);
    }

    protected Executor _exec;
    protected int _port;
}
