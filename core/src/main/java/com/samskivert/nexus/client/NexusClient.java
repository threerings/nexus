//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.client;

import java.util.HashMap;
import java.util.Map;

import com.samskivert.nexus.distrib.Address;
import com.samskivert.nexus.distrib.NexusObject;
import com.samskivert.nexus.net.Connection;
import com.samskivert.nexus.util.Callback;
import com.samskivert.nexus.util.CallbackList;

import static com.samskivert.nexus.util.Log.log;

/**
 * Manages connections to Nexus servers. Provides access to distributed objects and services.
 */
public abstract class NexusClient
{
    /**
     * Requests to subscribe to the object identified by the supplied address.
     */
    public <T extends NexusObject> void subscribe (final Address<T> addr, final Callback<T> callback)
    {
        withConnection(addr.host, new Callback.Chain<Connection>(callback) {
            public void onSuccess (Connection conn) {
                conn.subscribe(addr, callback);
            }
        });
    }

    /**
     * Unsubscribes from the specified object. Any events in-flight will be sent to the server, and
     * any events generated after this unsubscription request will be dropped.
     */
    public void unsubscribe (NexusObject object)
    {
        // TODO: object.postEvent(new UnsubscribeMarker())
        // TODO: catch UnsubscribeMarker in Connection.postEvent, and unsubscribe instead of
        // forwarding the event to the server
    }

    /**
     * Creates a callback that will subscribe to an object of the specified address and pass the
     * successfully subscribed object through to the supplied callback. All failures will be
     * propagated through to the supplied callback as well. This simplifies the handling of a
     * common pattern, which is to make a service request, receive an object address in response
     * and immediately subscribe to the object in question. One can write code like so:
     * <pre>{@code
     * // assume RoomService.joinRoom (String roomId, Callback<Address<RoomObject>> callback);
     * obj.joinRoom(roomId, client.subscriber(new Callback<RoomObject>() {
     *     public void onSuccess (RoomObject obj) { ... }
     *     public void onFailure (Throwable cause) { ... }
     * }));
     * }</pre>
     */
    public <T extends NexusObject> Callback<Address<T>> subscriber (final Callback<T> callback)
    {
        return new Callback<Address<T>>() {
            public void onSuccess (Address<T> address) {
                subscribe(address, callback);
            }
            public void onFailure (Throwable cause) {
                callback.onFailure(cause);
            }
        };
    }

    protected abstract void connect (String host, Callback<Connection> callback);

    // TODO: should we disconnect immediately when clearing last subscription from a given
    // connection, or should we periodically poll our open connections and disconnect any with no
    // active subscriptions (this would allow a little leeway, so that a usage pattern wherein one
    // unsubscribed from their last object on a given server and then immediately subscribed to a
    // new one, did not cause needless disconnect and reconnect)
    protected void disconnect (String serverHost)
    {
        // TODO
    }

    /**
     * Establishes a connection with the supplied host (if one does not already exist), and invokes
     * the supplied action with said connection. Ensures thread-safety in the process.
     */
    protected synchronized void withConnection (final String host, Callback<Connection> action)
    {
        Connection conn = _connections.get(host);
        if (conn != null) {
            action.onSuccess(conn);
            return;
        }

        CallbackList<Connection> plist = _penders.get(host);
        if (_penders != null) {
            plist.add(action);
            return;
        }

        _penders.put(host, plist = CallbackList.create(action));
        connect(host, new Callback<Connection>() {
            public void onSuccess (Connection conn) {
                onConnectSuccess(conn);
            }
            public void onFailure (Throwable cause) {
                onConnectFailure(host, cause);
            }
        });
    }

    protected synchronized void onConnectSuccess (Connection conn)
    {
        CallbackList<Connection> plist = _penders.remove(conn.getHost());
        if (plist == null) {
            log.warning("Have no penders for established connection?!", "host", conn.getHost());
            conn.close(); // shutdown and drop connection
        } else {
            _connections.put(conn.getHost(), conn);
            plist.onSuccess(conn);
        }
    }

    protected synchronized void onConnectFailure (String host, Throwable cause)
    {
        CallbackList<Connection> plist = _penders.remove(host);
        if (plist == null) {
            log.warning("Have no penders for failed connection?!", "host", host);
        } else {
            plist.onFailure(cause);
        }
    }

    /** A mapping from hostname to connection instance for all active connections. */
    protected Map<String, Connection> _connections = new HashMap<String, Connection>();

    /** A mapping from hostname to listener list, for all pending connections. */
    protected Map<String, CallbackList<Connection>> _penders =
        new HashMap<String, CallbackList<Connection>>();
}
