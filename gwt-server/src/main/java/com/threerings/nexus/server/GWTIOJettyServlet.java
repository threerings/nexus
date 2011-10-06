//
// Nexus GWTServer - server-side support for Nexus GWT/WebSockets services
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.server;

import java.io.IOException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.threerings.nexus.io.GWTServerIO;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;

import com.threerings.nexus.io.GWTIO;
import com.threerings.nexus.io.Serializer;
import com.threerings.nexus.net.Downstream;
import com.threerings.nexus.net.Upstream;
import com.threerings.nexus.server.SessionManager;

import static com.threerings.nexus.util.Log.log;

/**
 * Wires our GWT/IO into Jetty WebSockets.
 */
public class GWTIOJettyServlet extends WebSocketServlet
{
    public GWTIOJettyServlet (SessionManager smgr, Serializer szer) {
        _smgr = smgr;
        _szer = szer;
    }

    @Override
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws ServletException, IOException {
        getServletContext().getNamedDispatcher("default").forward(req, rsp);
    }

    @Override
    protected WebSocket doWebSocketConnect (HttpServletRequest req, String protocol) {
        return new GWTIOWebSocket(req.getRemoteAddr());
    }

    protected class GWTIOWebSocket implements WebSocket, SessionManager.Output {
        public GWTIOWebSocket (String ipaddress) {
            _ipaddress = ipaddress;
        }

        // from interface WebSocket
        public void onConnect (Outbound outbound) {
            _outbound = outbound;
            _input = _smgr.createSession(_ipaddress, this);
        }

        // from interface WebSocket
        public void onMessage (byte frame, byte[] data, int offset, int length) {
            log.info("Got binary message " + frame);
            // TODO: is this really supported?
        }

        // from interface WebSocket
        public void onMessage (byte frame, String data) {
            try {
                _input.onMessage(GWTServerIO.newInput(_szer, data).<Upstream>readValue());
            } catch (Throwable t) {
                log.warning("WebSocket decode failure", "addr", _ipaddress, "data", data, t);
            }
        }

        // from interface WebSocket
        public void onDisconnect () {
            _input.onDisconnect();
        }

        // from interface WebSocket
        public void onFragment (boolean more, byte opcode, byte[] data, int offset, int length) {
            log.info("Got fragment " + more + "/" + opcode);
            // nada
        }

        // from interface SessionManager.Input
        public void send (Downstream msg) {
            if (!_outbound.isOpen()) {
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
                _outbound.sendMessage(data);
            } catch (IOException ioe) {
                log.warning("WebSocket send failure", "addr", _ipaddress, "data", data, ioe);
                _input.onSendError(ioe);
                _outbound.disconnect();
            }
        }

        // from interface SessionManager.Input
        public void disconnect () {
            _outbound.disconnect();
        }

        protected String _ipaddress;
        protected SessionManager.Input _input;
        protected Outbound _outbound;
        protected GWTServerIO.PayloadBuffer _buffer = new GWTServerIO.PayloadBuffer();
    }

    protected SessionManager _smgr;
    protected Serializer _szer;
}
