//
// Nexus iOS IO - I/O and network services for Nexus built on Monotouch via IKVM
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.net;

import java.io.IOException;
import java.nio.channels.ByteChannel;

import java.util.concurrent.Executor;

import react.RPromise;

import com.threerings.nexus.util.Log;

/**
 * Implements a Nexus connection using IOS I/O.
 */
public class IOSConnection extends JVMConnection
{
    /**
     * Creates an instance which will initiate a Nexus protocol connection with the specified host,
     * on the specified port.
     *
     * @param callback will be notified on connection completion, or failure.
     */
    public IOSConnection (String host, int port, Executor exec, RPromise<Connection> callback) {
        super(Log.log, host, port, exec, callback);
    }

    @Override protected ByteChannel openChannel (String host, int port) throws IOException {
        return SocketByteChannel.open(host, port);
    }
}
