//
// Nexus GWTServer - server-side support for Nexus GWT/WebSockets services
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.server;

import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

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
    public void configure (WebSocketServletFactory factory) {
        factory.getPolicy().setIdleTimeout(10000);
        factory.setCreator(new WebSocketCreator() {
            @Override public Object createWebSocket (UpgradeRequest req, UpgradeResponse resp) {
                return new GWTIOWebSocket(_smgr, _szer);
            }
        });
    }

    protected SessionManager _smgr;
    protected Serializer _szer;
}
