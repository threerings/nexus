//
// Nexus GWTIO - I/O and network services for Nexus built on GWT and WebSockets
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.client;

import com.google.gwt.core.client.GWT;

import react.RPromise;

import com.threerings.nexus.io.Serializer;
import com.threerings.nexus.net.Connection;
import com.threerings.nexus.net.GWTConnection;
import com.threerings.nexus.util.Log;

/**
 * Provides a Nexus client using WebSockets and GWT-based I/O.
 */
public class GWTClient extends NexusClient
{
    /** The default (root-relative) path at which we will make our WebSocket connections. */
    public static final String DEFAULT_WS_PATH = "/nexusws";

    /**
     * Creates a Nexus client which will use WebSockets to connect to hosts.
     * @param port the port on which to make WebSocket connections.
     * @param path the path to the GWTIO WebSockets servlet. Must start with '/'.
     * @param szer the serializer that knows about all types that will cross the wire.
     */
    public static NexusClient create (int port, String path, Serializer szer) {
        if (!path.startsWith("/")) throw new IllegalArgumentException("Path must start with '/'.");
        Log.log = GWT_LOGGER; // configure the logger to use GWT
        return new GWTClient(port, path, szer);
    }

    /**
     * Creates a Nexus client which will use WebSockets to connect to hosts. Uses the default
     * servlet path (i.e. {@link #DEFAULT_WS_PATH}).
     * @param port the port on which to make WebSocket connections.
     * @param szer the serializer that knows about all types that will cross the wire.
     */
    public static NexusClient create (int port, Serializer szer) {
        return create(port, DEFAULT_WS_PATH, szer);
    }

    protected GWTClient (int port, String path, Serializer szer) {
        _port = port;
        _path = path;
        _szer = szer;
    }

    @Override protected int port () {
        return _port;
    }

    @Override protected void connect (String host, RPromise<Connection> callback) {
        new GWTConnection(host, _port, _path, _szer, callback);
    }

    protected final int _port;
    protected final String _path;
    protected final Serializer _szer;

    protected static final Log.Logger GWT_LOGGER = new Log.Logger() {
        @Override public void temp (String message, Object... args) {
            format(null, message, args);
        }
        @Override public void info (String message, Object... args) {
            if (!_warnOnly) {
                format(null, message, args);
            }
        }
        @Override public void warning (String message, Object... args) {
            format(null, message, args);
        }
        @Override public void log (Object level, String message, Throwable cause) {
            if (GWT.isScript()) {
                sendToBrowserConsole(message, cause);
            } else {
                GWT.log(message, cause);
            }
        }
        @Override public void setWarnOnly (boolean warnOnly) {
            _warnOnly = warnOnly;
        }
        protected boolean _warnOnly;
    };

    protected static native void sendToBrowserConsole(String msg, Throwable e) /*-{
        if ($wnd.console && $wnd.console.info) {
            if (e != null) {
                $wnd.console.info(msg, e);
            } else {
                $wnd.console.info(msg);
            }
        }
    }-*/;
}
