//
// $Id$
//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.server;

import java.io.OutputStream;
import java.nio.ByteBuffer;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import com.samskivert.nexus.net.Downstream;
import com.samskivert.nexus.net.Upstream;

/**
 * Manages active client sessions. Actual communication is handled by external entities so that
 * raw-socket, WebSocket, and potentially other session types can be simultaneously supported.
 */
public class SessionManager
{
    /**
     * An interface via which transport mechanisms inject messages into the Nexus system.
     */
    public interface Input {
        /**
         * Called when a message is received from the client.
         */
        void onMessage (Upstream msg);

        /**
         * Called when a request to send to the client has failed. This will result in the session
         * terminating the connection, but knowing the cause of failure is useful for diagnostic
         * purposes.
         */
        void onSendError (Throwable error);

        /**
         * Called when a transport failure occurs when reading a message from the client. This will
         * result in the session terminating the connection, but knowing the cause of failure is
         * useful for diagnostic purposes.
         */
        void onReceiveError (Throwable error);

        /**
         * Called when the client connection is closed in an orderly manner.
         */
        void onDisconnect ();
    }

    /**
     * An interface via which a session sends messages to clients via a transport mechanism, and
     * otherwise manages the client connection.
     */
    public interface Output {
        /**
         * Requests that supplied message be sent to the client.
         */
        void send (Downstream msg);

        /**
         * Requests that the client connection be closed. This may be called following receipt of a
         * read or write error, or to shut the connection down in an orderly manner.
         */
        void disconnect ();
    }

    public SessionManager (ObjectManager omgr)
    {
        _omgr = omgr;
    }

    /**
     * Creates a new session for a client with the supplied IP address. The supplied {@link Output}
     * instance will be used to communicate messages to the client, and the returned {@link Input}
     * instance should be delivered messages coming from the client.
     */
    public Input createSession (String ipaddress, Output output)
    {
        Session session = new Session(this, _omgr, ipaddress, output);
        _byIP.put(ipaddress, session);
        return session;
    }

    protected void sessionDisconnected (Session sess)
    {
        // remove the session from our (ip -> sessions) mapping
        _byIP.remove(sess.getIPAddress(), sess);

        // TODO: anything else?
    }

    /** Provides the ability to send and receive distributed events, etc. */
    protected ObjectManager _omgr;

    /** Maintains the IP to sessions mapping. */
    protected Multimap<String,Session> _byIP =
        Multimaps.synchronizedListMultimap(ArrayListMultimap.<String,Session>create());
}
