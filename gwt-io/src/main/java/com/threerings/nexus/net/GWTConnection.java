//
// Nexus GWTIO - I/O and network services for Nexus built on GWT and WebSockets
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.net;

import com.google.gwt.core.client.JavaScriptObject;

import react.RPromise;

import com.threerings.nexus.distrib.NexusException;
import com.threerings.nexus.io.GWTIO;
import com.threerings.nexus.io.Serializer;
import com.threerings.nexus.util.Log;

/**
 * Implements a Nexus connection using WebSockets.
 */
public class GWTConnection extends Connection
{
    /**
     * Creates an instance which will initiate a Nexus protocol connection with the specified host.
     * @param callback will be notified on connection completion, or failure.
     */
    public GWTConnection (String host, int port, String path, Serializer szer,
                          RPromise<Connection> callback) {
        super(Log.log, host);
        _szer = szer;
        _callback = callback;
        wsConnect("ws://" + host + ":" + port + path);
    }

    @Override // from Connection
    public void close () {
        wsClose(_ws);
    }

    @Override // from Connection
    protected void send (Upstream request) {
        StringBuffer payload = new StringBuffer();
        GWTIO.newOutput(_szer, payload).writeValue(request);
        wsSend(_ws, payload.toString());
    }

    @Override // from Connection
    protected void dispatch (Runnable run) {
        // we have no threads in the browser, so we run things directly
        run.run();
    }

    protected void onOpen (JavaScriptObject ws) {
        _ws = ws;
        _callback.succeed(this);
        _callback = null; // note that we successfully connected
    }

    protected void onMessage (String data) {
        onReceive(GWTIO.newInput(_szer, data).<Downstream>readValue());
    }

    protected void onError (String reason) {
        if (_callback != null) {
            // if we were trying to connect, report to our listener that connection failed
            _callback.fail(new NexusException(reason));
        } else {
            // otherwise we were in the middle of a running session
            onClose(new NexusException(reason));
        }
    }

    protected void onClose () {
        if (_callback != null) {
            _callback.fail(new NexusException("Server unavailable"));
        } else {
            onClose(null);
        }
    }

    protected native void wsConnect (String url) /*-{
        if (!$wnd.WebSocket) {
             this.@com.threerings.nexus.net.GWTConnection::onError(Ljava/lang/String;)(
                 "WebSocket not supported by this browser.");
            return;
        }

        var conn = this;
        var ws = new $wnd.WebSocket(url);
        ws.onopen = function (event) {
            conn.@com.threerings.nexus.net.GWTConnection::onOpen(Lcom/google/gwt/core/client/JavaScriptObject;)(ws);
        };
        ws.onmessage = function (event) {
            if (event.data) {
                conn.@com.threerings.nexus.net.GWTConnection::onMessage(Ljava/lang/String;)(
                    event.data);
            }
        };
        ws.onerror = function (event) {
             conn.@com.threerings.nexus.net.GWTConnection::onError(Ljava/lang/String;)(
                 "TODO");
        };
        ws.onclose = function (event) {
             conn.@com.threerings.nexus.net.GWTConnection::onClose()();
        };
    }-*/;

    protected native void wsSend (JavaScriptObject ws, String message) /*-{
        ws.send(message);
    }-*/;

    protected native void wsClose (JavaScriptObject ws) /*-{
        ws.close();
    }-*/;

    protected final Serializer _szer;
    protected RPromise<Connection> _callback;
    protected JavaScriptObject _ws;
}
