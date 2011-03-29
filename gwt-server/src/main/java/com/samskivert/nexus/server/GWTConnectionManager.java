//
// $Id$

package com.samskivert.nexus.server;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import org.eclipse.jetty.http.HttpException;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.nio.SelectChannelEndPoint;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.samskivert.nexus.distrib.NexusException;
import com.samskivert.nexus.io.GWTIO;
import com.samskivert.nexus.net.GWTConnection;

import static com.samskivert.nexus.util.Log.log;

/**
 * Handles starting a Jetty server and configuring it to listen for GWTIO WebSocket requests on a
 * specified port. For systems that don't otherwise make use of Jetty, this simplifies things.
 * Systems that do make use of Jetty can simply wire up the {@link GWTIOJettyServlet} themselves.
 */
public class GWTConnectionManager
{
    public GWTConnectionManager (SessionManager smgr, GWTIO.Serializer szer,
                                 String hostname, int port)
    {
        _jetty = new Server();

        // use a custom connector that works around some jetty non-awesomeness
        _jetty.setConnectors(new Connector[] {
            new SaneChannelConnector(hostname, port)
        });

//         // this will magically cause addHandler() to work and dispatch to our contexts
//         _jetty.setHandler(new ContextHandlerCollection());

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        GWTIOJettyServlet servlet = new GWTIOJettyServlet(smgr, szer);
        context.addServlet(new ServletHolder(servlet), "/" + GWTConnection.WS_PATH);

        // context.setResourceBase(new File(_config.getAppRoot(), "samsara").getPath());
        // context.setWelcomeFiles(new String[] { "index.html" });
        _jetty.setHandler(context);
    }

    public void start ()
    {
        try {
            _jetty.start();
        } catch (Exception e) {
            throw new NexusException(e);
        }
    }

    public void shutdown ()
    {
        try {
            _jetty.stop();
        } catch (Exception e) {
            throw new NexusException(e);
        }
    }

    protected static class SaneChannelConnector extends SelectChannelConnector
    {
        public SaneChannelConnector (String httpHost, int httpPort) {
            setHost(httpHost);
            setPort(httpPort);
        }

        @Override // from SelectChannelConnector
        protected Connection newConnection (SocketChannel chan, SelectChannelEndPoint ep) {
            return new HttpConnection(this, ep, getServer()) {
                @Override public Connection handle () throws IOException {
                    try {
                        return super.handle();
                    } catch (NumberFormatException nfe) {
                        // TODO: demote this to log.info in a week or two
                        log.warning("Failing invalid HTTP request", "uri", _uri, "error", nfe);
                        throw new HttpException(400); // bad request
                    } catch (IOException ioe) {
                        if (ioe.getClass() == IOException.class) { // grr
                            log.warning("Failing invalid HTTP request", "uri", _uri, "error", ioe);
                            throw new HttpException(400); // bad request
                        } else {
                            throw ioe;
                        }
                    }
                }
            };
        }
    }

    protected Server _jetty;
}
