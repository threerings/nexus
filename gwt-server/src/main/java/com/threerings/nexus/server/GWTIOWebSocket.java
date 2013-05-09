//
// Nexus GWTServer - server-side support for Nexus GWT/WebSockets services
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.server;

import java.io.IOException;

import com.threerings.nexus.io.GWTServerIO;
import com.threerings.nexus.io.Serializer;
import com.threerings.nexus.net.Downstream;
import com.threerings.nexus.net.Upstream;

import static com.threerings.nexus.util.Log.log;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;

/**
 * Handles a web socket client.
 */
public class GWTIOWebSocket implements WebSocketListener, SessionManager.Output
{
    public GWTIOWebSocket (SessionManager smgr, Serializer szer) {
        _smgr = smgr;
        _szer = szer;
    }

    // from interface WebSocketListener
    @Override public void onWebSocketConnect (Session session) {
        _sess = session;
        _conn = session.getRemote();
        _ipaddr = session.getRemoteAddress().toString();
        _input = _smgr.createSession(_ipaddr, this);
    }

    // from interface WebSocketListener
    @Override public void onWebSocketClose (int statusCode, String reason) {
        // TODO: interpret statusCode?
        _input.onDisconnect();
    }

    // from interface WebSocketListener
    @Override public void onWebSocketError (Throwable cause) {
        _input.onReceiveError(cause);
    }

    // from interface WebSocketListener
    @Override public void onWebSocketText (String data) {
        try {
            _input.onMessage(GWTServerIO.newInput(_szer, data).<Upstream>readValue());
        } catch (Throwable t) {
            log.warning("WebSocket decode failure", "addr", _ipaddr, "data", data, t);
        }
    }

    // from interface WebSocketListener
    @Override public void onWebSocketBinary (byte[] payload, int offset, int len) {
        log.warning("Got binary message", "addr", _ipaddr, "bytes", len);
    }

    // from interface SessionManager.Input
    public void send (Downstream msg) {
        if (!_sess.isOpen()) {
            log.warning("Dropping outbound message to closed WebSocket", "addr", _ipaddr);
            return;
        }

        String data;
        // send may be called from multiple threads, so synchronize access to our payload buffer
        synchronized (_buffer) {
            GWTServerIO.newOutput(_szer, _buffer).writeValue(msg);
            data = _buffer.getPayload();
        }

        try {
            _conn.sendString(data);
        } catch (IOException ioe) {
            log.warning("WebSocket send failure", "addr", _ipaddr, "data", data, ioe);
            _input.onSendError(ioe);
            disconnect();
        }
    }

    // from interface SessionManager.Input
    public void disconnect () {
        try {
            _sess.disconnect();
        } catch (IOException ioe) {
            log.warning("WebSocket disconnect failed", "addr", _ipaddr, ioe);
        }
    }

    protected final SessionManager _smgr;
    protected final Serializer _szer;

    protected String _ipaddr;
    protected Session _sess;
    protected RemoteEndpoint _conn;
    protected SessionManager.Input _input;
    protected GWTServerIO.PayloadBuffer _buffer = new GWTServerIO.PayloadBuffer();
}
