//
// Nexus GWTIO - I/O and network services for Nexus built on GWT and WebSockets
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.client;

import com.google.gwt.core.client.GWT;

import com.threerings.nexus.io.GWTIO;
import com.threerings.nexus.io.Serializer;
import com.threerings.nexus.net.Connection;
import com.threerings.nexus.net.GWTConnection;
import com.threerings.nexus.util.Callback;
import com.threerings.nexus.util.Log;

/**
 * Provides a Nexus client using WebSockets and GWT-based I/O.
 */
public class GWTClient extends NexusClient
{
    /**
     * Creates a Nexus client which will use WebSockets to connect to hosts.
     * @param port the port on which to make WebSocket connections.
     * @param szer the serializer that knows about all types that will cross the wire.
     */
    public static NexusClient create (int port, Serializer szer) {
        Log.log = GWT_LOGGER; // configure the logger to use GWT
        return new GWTClient(port, szer);
    }

    protected GWTClient (int port, Serializer szer) {
        _port = port;
        _szer = szer;
    }

    protected void connect (String host, Callback<Connection> callback) {
        new GWTConnection(host, _port, _szer, callback);
    }

    protected int _port;
    protected Serializer _szer;

    protected static final Log.Logger GWT_LOGGER = new Log.Logger() {
        public void info (String message, Object... args) {
            if (!_warnOnly) {
                format(null, message, args);
            }
        }
        public void warning (String message, Object... args) {
            format(null, message, args);
        }
        public void log (Object level, String message, Throwable cause) {
            GWT.log(message, cause);
        }
        public void setWarnOnly () {
            _warnOnly = true;
        }
        protected boolean _warnOnly;
    };
}
