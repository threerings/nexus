//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.server;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import react.Signal;
import react.SignalView;
import react.Slot;
import react.Try;

import com.google.common.collect.Maps;

import com.threerings.nexus.distrib.Action;
import com.threerings.nexus.distrib.NexusEvent;
import com.threerings.nexus.distrib.NexusObject;
import com.threerings.nexus.net.Downstream;
import com.threerings.nexus.net.Upstream;

import static com.threerings.nexus.util.Log.log;

/**
 * Represents an active client session.
 */
public class Session
{
    /** Handles network events for our client. Accessed by SessionManager. */
    final SessionManager.Input input = new SessionManager.Input() {
        public void onMessage (Upstream msg) {
            msg.dispatch(_handler);
        }

        public void onSendError (Throwable error) {
            // TODO: log something?
            onDisconnect(error);
        }

        public void onReceiveError (Throwable error) {
            // TODO: log something?
            onDisconnect(error);
        }

        public void onDisconnect () {
            onDisconnect(null);
        }

        @Override public String toString () {
            return Session.this.toString();
        }

        protected void onDisconnect (Throwable cause) {
            synchronized(this) {
                if (_disconnecting) {
                    log.warning("Attempted to disconnect while already disconnecting.", cause);
                    return;
                }

                _disconnecting = true;
            }

            // let interested parties know that we are audi 5000
            try {
                SessionLocal.setCurrent(Session.this);
                _onDisconnect.emit(cause);
            } finally {
                SessionLocal.clearCurrent();
            }

            // clear any object subscriptions we currently hold
            for (Integer id : _subscriptions) {
                _omgr.clearSubscriber(id, _subscriber);
            }
            _subscriptions.clear();

            // let the session manager know that we disconnected
            _smgr.sessionDisconnected(Session.this);
        }

        protected boolean _disconnecting;
    };

    /**
     * A signal that is emitted when this session disconnects expectedly or unexpectedly. The
     * signal will emit an exception if an I/O error encountered during send or receive, or null if
     * the client connection was simply closed. During the emission of this signal, the session in
     * question will be bound as current, so that {@link SessionLocal} can be used to access
     * session-local data.
     */
    public SignalView<Throwable> onDisconnect () {
        return _onDisconnect;
    }

    /**
     * Returns the value of the specified session-local attribute, or null if no value is currently
     * configured for the supplied key.
     */
    public <T> T getLocal (Class<T> key) {
        @SuppressWarnings("unchecked") T value = (T)_locals.get(key);
        return value;
    }

    /**
     * Configures the supplied value as the session-local attribute for the specified key.
     * @return the previously configured value for that key. (TODO: reject overwrites instead?)
     */
    public <T> T setLocal (Class<T> key, T value) {
        @SuppressWarnings("unchecked") T ovalue = (T)_locals.put(key, value);
        return ovalue;
    }

    /**
     * Returns the IP address via which this session is operating. Note that multiple sessions can
     * be operating over the same IP, so do not treat this as a unique key.
     */
    public String getIPAddress () {
        return _ipaddress;
    }

    /**
     * Disconnects this session.
     */
    public void disconnect () {
        _output.disconnect();
    }

    @Override public String toString () {
        // TODO: if authed, report authed id?
        return "[id=" + hashCode() + ", addr=" + _ipaddress + "]";
    }

    protected Session (SessionManager smgr, ObjectManager omgr, String ipaddress,
                       SessionManager.Output output) {
        _smgr = smgr;
        _omgr = omgr;
        _ipaddress = ipaddress;
        _output = output;
    }

    /**
     * Flattens a message into bytes and sends it to the client via the transport layer.
     */
    protected synchronized void sendMessage (Downstream msg) {
        _output.send(msg);
    }

    protected final ObjectManager.Subscriber _subscriber = new ObjectManager.Subscriber() {
        public void forwardEvent (NexusEvent event) {
            sendMessage(new Downstream.DispatchEvent(event));
        }
        public void onCleared (int id) {
            _subscriptions.remove(id);
            sendMessage(new Downstream.ObjectCleared(id));
        }
    };

    protected final Upstream.Handler _handler = new Upstream.Handler() {
        public void onSubscribe (Upstream.Subscribe msg) {
            SessionLocal.setCurrent(Session.this);
            try {
                // TODO: per-session class loaders or other fancy business
                NexusObject object = _omgr.addSubscriber(msg.addr, _subscriber);
                _subscriptions.add(object.getId());
                _omgr.invoke(object.getId(), new Action<NexusObject>() {
                    @Override public void invoke (NexusObject nexusObj) {
                        sendMessage(new Downstream.Subscribe(nexusObj));
                    }
                });
            } catch (Throwable t) {
                sendMessage(new Downstream.SubscribeFailure(msg.addr, t.getMessage()));
            } finally {
                SessionLocal.clearCurrent();
            }
        }

        public void onUnsubscribe (Upstream.Unsubscribe msg) {
            _subscriptions.remove(msg.id);
            _omgr.clearSubscriber(msg.id, _subscriber);
        }

        public void onPostEvent (Upstream.PostEvent msg) {
            // we pass things straight through to the object manager which handles everything
            _omgr.dispatchEvent(msg.event, Session.this);
        }

        public void onServiceCall (final Upstream.ServiceCall msg) {
            Slot<Try<Object>> slot = (msg.callId <= 0) ? null : new Slot<Try<Object>>() {
                public void onEmit (Try<Object> res) {
                    if (res.isSuccess()) sendMessage(
                        new Downstream.ServiceResponse(msg.callId, res.get()));
                    else sendMessage(
                        new Downstream.ServiceFailure(msg.callId, res.getFailure().getMessage()));
                }
            };
            _omgr.dispatchCall(msg.objectId, msg.attrIndex, msg.methodId, msg.args.toArray(),
                               Session.this, slot);
        }
    };

    protected final SessionManager _smgr;
    protected final ObjectManager _omgr;
    protected final String _ipaddress;
    protected final SessionManager.Output _output;

    /** A signal that's emitted when our client disconnects. */
    protected final Signal<Throwable> _onDisconnect = Signal.create();

    /** Tracks our extant object subscriptions. */
    protected final Set<Integer> _subscriptions = new ConcurrentSkipListSet<Integer>();

    /** Tracks session-local attributes. */
    protected final Map<Class<?>, Object> _locals = Maps.newHashMap();

    protected static final byte[] EMPTY_BUFFER = new byte[0];
}
