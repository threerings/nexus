//
// Nexus GWTServer - server-side support for Nexus GWT/WebSockets services
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.server;

import java.io.File;
import java.io.IOException;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.threerings.nexus.client.GWTClient;
import com.threerings.nexus.distrib.NexusException;
import com.threerings.nexus.io.Serializer;

import static com.threerings.nexus.util.Log.log;

/**
 * Handles starting a Jetty server and configuring it to listen for GWTIO WebSocket requests on a
 * specified port. For systems that don't otherwise make use of Jetty, this simplifies things.
 * Systems that do make use of Jetty can simply wire up the {@link GWTIOJettyServlet} themselves.
 */
public class GWTConnectionManager
{
    /**
     * Creates a Jetty server that listens on the specified hostname and port and binds the
     * WebSocket servlet to the {@link GWTClient#DEFAULT_WS_PATH}.
     */
    public GWTConnectionManager (SessionManager smgr, Serializer szer, String hostname, int port) {
        this(smgr, szer, hostname, port, GWTClient.DEFAULT_WS_PATH);
    }

    /**
     * Creates a Jetty server that listens on the specified hostname and port and binds the
     * WebSocket servlet to the specified path.
     */
    public GWTConnectionManager (SessionManager smgr, Serializer szer,
                                 String hostname, int port, String path) {
        _jetty = new Server(port);

        ServletContextHandler shandler = new ServletContextHandler();
        shandler.setContextPath("/");
        shandler.addServlet(new ServletHolder(new GWTIOJettyServlet(smgr, szer)), path);

        _rhandler = new ResourceHandler();
        _rhandler.setResourceBase("disabled");
        _rhandler.setWelcomeFiles(new String[] { "index.html" });

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { _rhandler, shandler });
        _jetty.setHandler(handlers);
    }

    public void setDocRoot (File docroot) {
        _rhandler.setResourceBase(docroot.getAbsolutePath());
    }

    public void start () {
        try {
            _jetty.start();
        } catch (Exception e) {
            throw new NexusException(e);
        }
    }

    public void shutdown () {
        try {
            _jetty.stop();
        } catch (Exception e) {
            throw new NexusException(e);
        }
    }

    protected Server _jetty;
    protected ResourceHandler _rhandler;
}
