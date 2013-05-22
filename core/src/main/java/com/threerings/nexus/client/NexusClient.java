//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import react.Function;
import react.RFuture;
import react.RPromise;
import react.Slot;

import com.threerings.nexus.distrib.Address;
import com.threerings.nexus.distrib.NexusException;
import com.threerings.nexus.distrib.NexusObject;
import com.threerings.nexus.net.Connection;

import static com.threerings.nexus.util.Log.log;

/**
 * Manages connections to Nexus servers. Provides access to distributed objects and services.
 */
public abstract class NexusClient
{
    /**
     * Creates a subscriber, see {@link Subscriber} for details.
     */
    public <T extends NexusObject> Subscriber<T> subscriber () {
        return new SubImpl<T>();
    }

    /**
     * Creates and returns a new subscriber. If the supplied {@code subscriber} is non-null, it
     * will be unsubscribed. This is essentially a helper method to simplify a common pattern where
     * an existing subscription, if it exists, needs to be unsubscribed, and a new subscription
     * created, all in one fell swoop.
     */
    public <T extends NexusObject> Subscriber<T> subscriber (Subscriber<?> subscriber) {
        if (subscriber != null) subscriber.unsubscribe();
        return this.<T>subscriber();
    }

    /**
     * Closes all active connections.
     */
    public void closeAll () {
        for (RPromise<Connection> conn : new ArrayList<RPromise<Connection>>(_conns.values())) {
            conn.onSuccess(new Slot<Connection>() {
                public void onEmit (Connection conn) { conn.close(); }
            });
        }
    }

    protected abstract void connect (String host, RPromise<Connection> promise);

    // TODO: should we disconnect immediately when clearing last subscription from a given
    // connection, or should we periodically poll our open connections and disconnect any with no
    // active subscriptions (this would allow a little leeway, so that a usage pattern wherein one
    // unsubscribed from their last object on a given server and then immediately subscribed to a
    // new one, did not cause needless disconnect and reconnect)
    protected void disconnect (String serverHost) {
        // TODO
    }

    /**
     * Establishes a connection with the supplied host (if one does not already exist), and makes
     * it available to the caller via a future. Ensures thread-safety in the process.
     */
    protected synchronized RFuture<Connection> connection (final String host) {
        RPromise<Connection> conn = _conns.get(host);
        if (conn == null) {
            _conns.put(host, conn = RPromise.create());
            final Slot<Throwable> remover = new Slot<Throwable>() {
                public void onEmit (Throwable error) {
                    _conns.remove(host);
                    if (error == null) log.info("Connection to " + host + " closed.");
                    else log.info("Connection to " + host + " failed.", "error", error);
                }
            };
            conn.onFailure(remover).onSuccess(new Slot<Connection>() {
                // listen for connection close and remove our connection then
                public void onEmit (Connection conn) { conn.onClose.connect(remover); }
            });
            log.info("Connecting to " + host);
            connect(host, conn);
        }
        return conn;
    }

    protected class SubImpl<T extends NexusObject> implements Subscriber<T> {
        @Override public RFuture<T> subscribe (final Address<? extends T> addr) {
            if (_id >= 0) throw new IllegalStateException("Subscriber already used");
            _id = 0;
            return connection(addr.host).flatMap(new Function<Connection,RFuture<T>>() {
                public RFuture<T> apply (Connection conn) {
                    if (!_alive) return canceled();
                    return (_conn = conn).subscribe(addr).flatMap(new Function<T,RFuture<T>>() {
                        public RFuture<T> apply (T obj) {
                            _id = obj.getId();
                            // things are normal; complete with this result and we're done
                            if (_alive) return RFuture.success(obj);
                            // otherwise we unsubscribed before the object arrived, handle that
                            unsubscribe();
                            return canceled();
                        }
                    });
                }
            });
        }

        @Override public RFuture<T> apply (Address<? extends T> addr) {
            return _alive ? subscribe(addr) : canceled();
        }

        @Override public void unsubscribe () {
            // if we've already unsubscribed, then we're done
            if (_id == -1) return;
            // if we have not yet received our object, we have to wait for the object to arrive
            // before we can turn around and unsubscribe from it
            else if (_id == 0) _alive = false;
            // otherwise we can send the unsubscribe request
            else {
                _conn.unsubscribe(_id);
                _id = -1;
            }
        }

        protected RFuture<T> canceled () {
            return RFuture.failure(new NexusException("Subscription canceled"));
        }

        protected Connection _conn;
        protected boolean _alive = true;
        protected int _id = -2;
    }

    /** A mapping from hostname to connection instance for all pending and active connections. */
    protected Map<String, RPromise<Connection>> _conns = new HashMap<String, RPromise<Connection>>();
}
