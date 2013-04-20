//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.client;

import java.util.HashMap;
import java.util.Map;

import react.Slot;

import com.threerings.nexus.distrib.Address;
import com.threerings.nexus.distrib.NexusObject;
import com.threerings.nexus.net.Connection;
import com.threerings.nexus.util.Callback;
import com.threerings.nexus.util.CallbackList;

import static com.threerings.nexus.util.Log.log;

/**
 * Manages connections to Nexus servers. Provides access to distributed objects and services.
 */
public abstract class NexusClient
{
    /**
     * Requests to subscribe to the object identified by the supplied address.
     *
     * @return a subscription handle that can be used to terminate the subscription.
     */
    public <T extends NexusObject> Subscription subscribe (Address<T> addr, Callback<T> callback) {
        return new SubImpl<T>(addr, callback); // ctor starts things off
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
    public <T extends NexusObject> Callback<Address<T>> subscriber (final Callback<T> callback) {
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
    protected void disconnect (String serverHost) {
        // TODO
    }

    /**
     * Establishes a connection with the supplied host (if one does not already exist), and invokes
     * the supplied action with said connection. Ensures thread-safety in the process.
     */
    protected synchronized void withConnection (final String host,
                                                Callback<? super Connection> action) {
        Connection conn = _connections.get(host);
        if (conn != null) {
            action.onSuccess(conn);
            return;
        }

        CallbackList<Connection> plist = _penders.get(host);
        if (plist != null) {
            plist.add(action);
            return;
        }

        log.info("Connecting to " + host);
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

    protected synchronized void onConnectSuccess (final Connection conn) {
        CallbackList<Connection> plist = _penders.remove(conn.getHost());
        if (plist == null) {
            log.warning("Have no penders for established connection?!", "host", conn.getHost());
            conn.close(); // shutdown and drop connection
        } else {
            _connections.put(conn.getHost(), conn);
            // we want to be notified when this connection closes
            conn.onClose.connect(new Slot<Throwable>() {
                @Override public void onEmit (Throwable error) {
                    onConnectionClose(conn, error);
                }
            });
            plist.onSuccess(conn);
        }
    }

    protected synchronized void onConnectFailure (String host, Throwable cause) {
        CallbackList<Connection> plist = _penders.remove(host);
        if (plist == null) {
            log.warning("Have no penders for failed connection?!", "host", host);
        } else {
            plist.onFailure(cause);
        }
    }

    protected synchronized void onConnectionClose (Connection conn, Throwable error) {
        // TODO: do we care about orderly versus exceptional closure?
        _connections.remove(conn.getHost());
    }

    protected class SubImpl<T extends NexusObject> implements Subscription {
        public final Address<T> addr;

        public SubImpl (Address<T> addr, Callback<T> callback) {
            this.addr = addr;
            _callback = callback;
            withConnection(addr.host, new Callback<Connection>() {
                public void onSuccess (Connection conn) { gotConnection(conn); }
                public void onFailure (Throwable cause) { notifyFailure(cause); }
            });
        }

        @Override public void unsubscribe () {
            // if we've already unsubscribed, then we're done
            if (_id == -1) return;
            // if we have not yet received our object, we have to wait for the object to arrive
            // before we can turn around and unsubscribe from it
            else if (_id == 0) {
                _wantUnsubscribe = true;
                _callback = null; // don't report future success or failure
            }
            // otherwise we can send the unsubscribe request
            else doUnsubscribe();
        }

        protected void gotConnection (Connection conn) {
            conn.subscribe(addr, new Callback<T>() {
                public void onSuccess (T object) {
                    _id = object.getId();
                    // if we unsubscribed before the object arrived, process that now
                    if (_wantUnsubscribe) doUnsubscribe();
                    // we should never lack a callback here, so freak out if we do
                    else if (_callback == null) log.warning(
                        "Got object but have no callback!?", "addr", addr, "id", _id);
                    // things are normal; notify our listener and be done
                    else {
                        _callback.onSuccess(object);
                        _callback = null;
                    }
                }
                public void onFailure (Throwable cause) { notifyFailure(cause); }
            });
        }

        protected void doUnsubscribe () {
            final int id = _id;
            _id = -1;
            withConnection(addr.host, new Callback<Connection>() {
                public void onSuccess (Connection conn) { conn.unsubscribe(id); }
                public void onFailure (Throwable cause) {
                    log.warning("Failed to obtain connection for unsubscribe", "addr", addr,
                                "id", id, "error", cause);
                }
            });
        }

        protected void notifyFailure (Throwable cause) {
            if (_callback != null) {
                _callback.onFailure(cause);
                _callback = null;
            }
        }

        protected int _id;
        protected Callback<T> _callback;
        protected boolean _wantUnsubscribe;
    }

    /** A mapping from hostname to connection instance for all active connections. */
    protected Map<String, Connection> _connections = new HashMap<String, Connection>();

    /** A mapping from hostname to listener list, for all pending connections. */
    protected Map<String, CallbackList<Connection>> _penders =
        new HashMap<String, CallbackList<Connection>>();
}
