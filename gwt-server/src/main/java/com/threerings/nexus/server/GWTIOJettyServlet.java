//
// Nexus GWTServer - server-side support for Nexus GWT/WebSockets services
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.server;

import java.io.IOException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;

import com.threerings.nexus.io.Serializer;

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
        // TODO: we should never get here, send back a server error
        getServletContext().getNamedDispatcher("default").forward(req, rsp);
    }

    @Override
    protected WebSocket doWebSocketConnect (HttpServletRequest req, String protocol) {
        return new GWTIOWebSocket(_smgr, _szer, req.getRemoteAddr());
    }

    protected SessionManager _smgr;
    protected Serializer _szer;
}
