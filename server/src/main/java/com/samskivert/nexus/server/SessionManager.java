//
// $Id$

package com.samskivert.nexus.server;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

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
         * Called when a message frame is received from the client. The frame may occupy a subset
         * of the supplied binary buffer, as dictated by the offset and length parameters.
         */
        void onMessage (byte[] data, int offset, int length);

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
         * Requests that the supplied message be sent to the client. The session yields control of
         * the supplied byte array to the transport mechanism, so that it may postpone message
         * delivery as needed.
         */
        void send (byte[] data);

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
    }

    /** Provides the ability to send and receive distributed events, etc. */
    protected ObjectManager _omgr;

    /** Maintains the IP to session mappings. */
    protected Multimap<String,Session> _byIP =
        Multimaps.synchronizedListMultimap(ArrayListMultimap.<String,Session>create());
}
