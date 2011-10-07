//
// Nexus GWTServer - server-side support for Nexus GWT/WebSockets services
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.server;

import java.io.IOException;

import org.eclipse.jetty.websocket.WebSocket;

import com.threerings.nexus.io.GWTServerIO;
import com.threerings.nexus.io.Serializer;
import com.threerings.nexus.net.Downstream;
import com.threerings.nexus.net.Upstream;

import static com.threerings.nexus.util.Log.log;

/**
 * Handles a web socket client.
 */
public class GWTIOWebSocket implements WebSocket, WebSocket.OnTextMessage, SessionManager.Output
{
    public GWTIOWebSocket (SessionManager smgr, Serializer szer, String ipaddress) {
        _smgr = smgr;
        _szer = szer;
        _ipaddress = ipaddress;
    }

    // from interface WebSocket
    public void onOpen (WebSocket.Connection conn) {
        _conn = conn;
        _input = _smgr.createSession(_ipaddress, this);
    }

    // from interface WebSocket.OnTextMessage
    public void onMessage (String data) {
        try {
            _input.onMessage(GWTServerIO.newInput(_szer, data).<Upstream>readValue());
        } catch (Throwable t) {
            log.warning("WebSocket decode failure", "addr", _ipaddress, "data", data, t);
        }
    }

    // from interface WebSocket
    public void onClose (int closeCode, String message) {
        // TODO: interpret closeCode?
        _input.onDisconnect();
    }

    // from interface SessionManager.Input
    public void send (Downstream msg) {
        if (!_conn.isOpen()) {
            log.warning("Dropping outbound message to closed WebSocket", "addr", _ipaddress);
            return;
        }

        String data;
        // send may be called from multiple threads, so synchronize access to our payload buffer
        synchronized (_buffer) {
            GWTServerIO.newOutput(_szer, _buffer).writeValue(msg);
            data = _buffer.getPayload();
        }

        try {
            _conn.sendMessage(data);
        } catch (IOException ioe) {
            log.warning("WebSocket send failure", "addr", _ipaddress, "data", data, ioe);
            _input.onSendError(ioe);
            _conn.disconnect();
        }
    }

    // from interface SessionManager.Input
    public void disconnect () {
        _conn.disconnect();
    }

    protected final SessionManager _smgr;
    protected final Serializer _szer;
    protected final String _ipaddress;

    protected SessionManager.Input _input;
    protected WebSocket.Connection _conn;
    protected GWTServerIO.PayloadBuffer _buffer = new GWTServerIO.PayloadBuffer();
}
