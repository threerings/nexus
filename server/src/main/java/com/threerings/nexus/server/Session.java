//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import react.SignalView;
import react.UnitSignal;

import com.google.common.collect.Maps;

import com.threerings.nexus.distrib.Action;
import com.threerings.nexus.distrib.NexusEvent;
import com.threerings.nexus.distrib.NexusException;
import com.threerings.nexus.distrib.NexusObject;
import com.threerings.nexus.distrib.NexusService;
import com.threerings.nexus.net.Downstream;
import com.threerings.nexus.net.Upstream;
import com.threerings.nexus.util.Callback;

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
            // TODO
        }

        public void onReceiveError (Throwable error) {
            // TODO
        }

        public void onDisconnect () {
            // let interested parties know that we are audi 5000
            try {
                SessionLocal.setCurrent(Session.this);
                _onDisconnect.emit();
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
    };

    /**
     * A signal that is emitted when this session disconnects unexpectedly. During the emission of
     * this signal, the session in question will be bound as current, so that {@link SessionLocal}
     * can be used to access session-local data.
     */
    public SignalView<Void> onDisconnect () {
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
                sendMessage(new Downstream.Subscribe(object));
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
            // if we have a call id, stick a callback in the final slot which communicates the
            // result back to the calling client
            Object[] args = msg.args.toArray();
            if (msg.callId > 0) {
                assert(args[args.length-1] == null);
                args[args.length-1] = new Callback<Object>() {
                    public void onSuccess (Object result) {
                        // add ourselves as a subscriber to any objects in the response
                        if (result instanceof NexusService.ObjectResponse) {
                            for (NexusObject obj : ((NexusService.ObjectResponse)result).
                                     getObjects()) {
                                _omgr.addSubscriber(obj, _subscriber);
                                _subscriptions.add(obj.getId());
                            }
                        }
                        sendMessage(new Downstream.ServiceResponse(msg.callId, result));
                    }
                    public void onFailure (Throwable cause) {
                        sendMessage(new Downstream.ServiceFailure(msg.callId, cause.getMessage()));
                    }
                };
            }
            _omgr.dispatchCall(msg.objectId, msg.attrIndex, msg.methodId, args, Session.this);
        }
    };

    protected final SessionManager _smgr;
    protected final ObjectManager _omgr;
    protected final String _ipaddress;
    protected final SessionManager.Output _output;

    /** A signal that's notified when our client disconnects. */
    protected final UnitSignal _onDisconnect = new UnitSignal();

    /** Tracks our extant object subscriptions. */
    protected final Set<Integer> _subscriptions = new ConcurrentSkipListSet<Integer>();

    /** Tracks session-local attributes. */
    protected final Map<Class<?>, Object> _locals = Maps.newHashMap();

    protected static final byte[] EMPTY_BUFFER = new byte[0];
}
