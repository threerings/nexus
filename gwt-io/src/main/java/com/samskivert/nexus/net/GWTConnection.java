//
// $Id$
//
// Nexus GWTIO - I/O and network services for Nexus built on GWT and WebSockets
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.net;

import com.samskivert.nexus.distrib.NexusException;
import com.samskivert.nexus.io.GWTIO;
import com.samskivert.nexus.util.Callback;

/**
 * Implements a Nexus connection using WebSockets.
 */
public class GWTConnection extends Connection
{
    /**
     * Creates an instance which will initiate a Nexus protocol connection with the specified host.
     * @param callback will be notified on connection completion, or failure.
     */
    public GWTConnection (String host, int port, GWTIO.Serializer szer, Callback<Connection> callback)
    {
        super(host);
        _szer = szer;
        _callback = callback;
        wsConnect("ws://" + host + ":" + port + "/nexusws");
    }

    @Override // from Connection
    public void close ()
    {
        wsClose();
    }

    @Override // from Connection
    protected void send (Upstream request)
    {
        StringBuffer payload = new StringBuffer();
        GWTIO.newOutput(_szer, payload).writeValue(request);
        wsSend(payload.toString());
    }

    @Override // from Connection
    protected void dispatch (Runnable run)
    {
        // we have no threads in the browser, so we run things directly
        run.run();
    }

    protected void onOpen ()
    {
        _callback.onSuccess(this);
        _callback = null; // note that we successfully connected
    }

    protected void onMessage (String data)
    {
        onReceive(GWTIO.newInput(_szer, data).<Downstream>readValue());
    }

    protected void onError (String reason)
    {
        if (_callback != null) {
            // if we were trying to connect, report to our listener that connection failed
            _callback.onFailure(new NexusException(reason));
        } else {
            // otherwise we were in the middle of a running session
            // TODO: notify someone?
        }
    }

    protected void onClose ()
    {
        // TODO: notify someone?
    }

    protected native void wsConnect (String url) /*-{
        var that = this;
        if (!$wnd.WebSocket) {
             that.@com.samskivert.nexus.net.GWTConnection::onError(Ljava/lang/String;)(
                 "WebSocket not supported by this browser.");
            return;
        }
        console.log("WebSocket connecting to " + url);
        that._ws = new $wnd.WebSocket(url);
        console.log("WebSocket connected " + that._ws.readyState);

        that._ws.onopen = function (event) {
            console.log("WebSocket["+url+"].onopen: " + event);
            that.@com.samskivert.nexus.net.GWTConnection::onOpen();
        };

        that._ws.onmessage = function (event) {
            console.log("WebSocket["+url+"].onmessage: " + event);
            if (event.data) {
                that.@com.samskivert.nexus.net.GWTConnection::onMessage(Ljava/lang/String;)(
                    event.data);
            }
        };

        that._ws.onerror = function (event) {
             console.log("WebSocket["+url+"].onerror: " + event);
             that.@com.samskivert.nexus.net.GWTConnection::onError(Ljava/lang/String;)(
                 "TODO");
        };

        that._ws.onclose = function (event) {
             console.log("WebSocket["+url+"].onclose: " + event);
             that.@com.samskivert.nexus.net.GWTConnection::onClose()();
        };
    }-*/;

    protected native void wsSend (String message) /*-{
        if (this._ws) {
            this._ws.send(message);
        }
    }-*/;

    protected native void wsClose () /*-{
        if (this._ws) {
            this._ws.close();
        }
    }-*/;

    protected final GWTIO.Serializer _szer;
    protected Callback<Connection> _callback;
}
