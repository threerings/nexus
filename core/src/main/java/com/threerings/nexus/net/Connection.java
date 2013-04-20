//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.net;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import react.Signal;

import com.threerings.nexus.distrib.Address;
import com.threerings.nexus.distrib.DistribUtil;
import com.threerings.nexus.distrib.EventSink;
import com.threerings.nexus.distrib.NexusEvent;
import com.threerings.nexus.distrib.NexusException;
import com.threerings.nexus.distrib.NexusObject;
import com.threerings.nexus.distrib.NexusService;
import com.threerings.nexus.util.Callback;
import com.threerings.nexus.util.Util;

import static com.threerings.nexus.util.Log.log;

/**
 * Manages a connection to a particular server.
 */
public abstract class Connection
    implements Downstream.Handler, EventSink
{
    /** A signal emitted when this connection is closed. The payload will be non-null if the
     * connection was closed in error, null if it was closed in an orderly manner. */
    public final Signal<Throwable> onClose = Signal.create();

    /**
     * Subscribes to the Nexus object with the specified address. Success (i.e. the object) or
     * failure will be communicated to {@code cb}.
     */
    public <T extends NexusObject> void subscribe (Address<T> addr, Callback<T> cb) {
        if (!_penders.addPender(addr, cb)) {
            send(new Upstream.Subscribe(addr));
        }
    }

    /**
     * Unsubscribes from the Nexus object with the specified id.
     */
    public void unsubscribe (int id) {
        if (_objects.remove(id) == null) {
            log.warning("Requested to unsubscribe from unknown object", "id", id);
            return;
        }

        // TODO: make a note that we just unsubscribed, and so not to warn about events that arrive
        // for this object in the near future (the server doesn't yet know that we've unsubscribed)
        send(new Upstream.Unsubscribe(id));
    }

    /**
     * Closes this connection in an orderly fashion. Any messages currently queued up should be
     * delivered prior to closure.
     */
    public abstract void close ();

    // from interface EventSink
    public String getHost () {
        return _host;
    }

    // from interface EventSink
    public void postEvent (NexusObject source, NexusEvent event) {
        send(new Upstream.PostEvent(event));
    }

    // from interface EventSink
    public void postCall (NexusObject source, short attrIndex, short methodId, Object[] args) {
        // determine whether we have a callback (which the service generator code will enforce is
        // always the final argument of the method)
        int callId, lastIdx = args.length-1;
        if (lastIdx > -1 && args[lastIdx] instanceof Callback<?>) {
            callId = 1;
            for (Integer key : _calls.keySet()) {
                callId = Math.max(callId, key+1);
            }
            _calls.put(callId, (Callback<?>)args[lastIdx]);
            args[lastIdx] = null; // we don't send the Callback over the wire
        } else {
            callId = 0; // no callback, no call id
        }

        // finally send the service call
        send(new Upstream.ServiceCall(callId, source.getId(), attrIndex,
                                      methodId, Arrays.asList(args)));
    }

    // from interface Downstream.Handler
    public void onSubscribe (Downstream.Subscribe msg) {
        // let this object know that we are its event sink
        DistribUtil.init(msg.object, msg.object.getId(), this);
        // the above must precede this call, as obtaining the object's address requires that the
        // event sink be configured
        Address<?> addr = msg.object.getAddress();
        List<Callback<?>> penders = _penders.getPenders(addr);
        if (penders == null) {
            log.warning("Missing pender list", "addr", addr);
            send(new Upstream.Unsubscribe(msg.object.getId())); // clear our subscription
            return;
        }

        // store the object in our local table
        _objects.put(msg.object.getId(), msg.object);

        // notify the pending listeners that the object has arrived
        for (Callback<?> pender : penders) {
            @SuppressWarnings("unchecked") Callback<NexusObject> cb =
                (Callback<NexusObject>)pender;
            Util.notifySuccess(cb, msg.object);
        }
    }

    // from interface Downstream.Handler
    public void onSubscribeFailure (Downstream.SubscribeFailure msg) {
        List<Callback<?>> penders = _penders.getPenders(msg.addr);
        if (penders == null) {
            log.warning("Missing pender list", "addr", msg.addr);
        } else {
            Exception cause = new Exception(msg.cause);
            for (Callback<?> pender : penders) Util.notifyFailure(pender, cause);
        }
    }

    // from interface Downstream.Handler
    public void onDispatchEvent (final Downstream.DispatchEvent msg) {
        final NexusObject target = _objects.get(msg.event.targetId);
        if (target == null) {
            log.warning("Missing target of event", "event", msg.event);
            return;
        }
        msg.event.applyTo(target);
    }

    // from interface Downstream.Handler
    public void onServiceResponse (Downstream.ServiceResponse msg) {
        Callback<?> callback = _calls.remove(msg.callId);
        if (callback == null) {
            log.warning("Received service response for unknown call", "msg", msg);
            return;
        }
        // if the result contains NexusObjects, they must be initialized
        if (msg.result instanceof NexusService.ObjectResponse) {
            for (NexusObject obj : ((NexusService.ObjectResponse)msg.result).getObjects()) {
                DistribUtil.init(obj, obj.getId(), this);
                _objects.put(obj.getId(), obj); // store the object in our local table
            }
        }
        @SuppressWarnings("unchecked") Callback<Object> ccb = (Callback<Object>)callback;
        Util.notifySuccess(ccb, msg.result);
    }

    // from interface Downstream.Handler
    public void onServiceFailure (Downstream.ServiceFailure msg) {
        Callback<?> callback = _calls.remove(msg.callId);
        if (callback == null) {
            log.warning("Received service failure for unknown call", "msg", msg);
            return;
        }
        Util.notifyFailure(callback, new NexusException(msg.cause));
    }

    // from interface Downstream.Handler
    public void onObjectCleared (Downstream.ObjectCleared msg) {
        final NexusObject target = _objects.remove(msg.id);
        if (target == null) {
            log.warning("Unknown object cleared", "id", msg.id);
            return;
        }
        Util.emit(target.onLost, null);
    }

    protected Connection (String host) {
        _host = host;
    }

    /**
     * Called to send a request message to the server.
     */
    protected abstract void send (Upstream request);

    /**
     * Dispatches the requested unit of work (generally the handling of an event) on the
     * appropriate thread.
     */
    protected abstract void dispatch (Runnable run);

    /**
     * Called when a message is received from the server.
     */
    protected void onReceive (final Downstream message) {
        dispatch(new Runnable() {
            public void run () {
                message.dispatch(Connection.this);
            }
        });
    }

    /**
     * Called when our connection is closed. If {@code error} is non-null, the closure will be
     * treated as an unexpected failure. If {@code error} is null, the closure will be treated as
     * orderly (as a result of a prior call to {@link #close}.
     */
    protected void onClose (final Throwable error) {
        // we want to pass a non-null exception to penders, failed calls and lost objects
        final Throwable perror = (error != null) ? error : new Exception("Connection closed");

        // do the bulk of our close processing on the dispatch thread
        dispatch(new Runnable() {
            public void run () {
                // notify any penders that we're not going to hear back
                _penders.onClose(perror, Connection.this);

                // notify any in-flight service calls that they failed
                if (!_calls.isEmpty()) {
                    log.info("Clearing " + _calls.size() + " calls.");
                    for (Callback<?> cb : _calls.values()) Util.notifyFailure(cb, perror);
                    _calls.clear();
                }

                // notify any dangling objects that they were lost
                if (!_objects.isEmpty()) {
                    log.info("Clearing " + _objects.size() + " objects.");
                    for (NexusObject obj : _objects.values()) Util.emit(obj.onLost, perror);
                    _objects.clear();
                }

                // finally notify listeners of our closure (here error may be null)
                Util.emit(onClose, error);
            }
        });
    }

    /** This map may be accessed by multiple threads, be sure its methods are synchronized. */
    protected static class PenderMap {
        public synchronized boolean addPender (Address<?> addr, Callback<?> cb) {
            boolean wasPending = true;
            List<Callback<?>> penders = _penders.get(addr);
            if (penders == null) {
                _penders.put(addr, penders = new ArrayList<Callback<?>>());
                wasPending = false;
            }
            penders.add(cb);
            return wasPending;
        }

        public synchronized List<Callback<?>> getPenders (Address<?> addr) {
            return _penders.remove(addr);
        }

        public synchronized void onClose (final Throwable error, Connection conn) {
            if (_penders.isEmpty()) return;
            log.info("Clearing " + _penders.size() + " penders.");
            for (final List<Callback<?>> penders : _penders.values()) {
                for (Callback<?> pender : penders) Util.notifyFailure(pender, error);
            }
            _penders.clear();
        }

        protected Map<Address<?>, List<Callback<?>>> _penders =
            new HashMap<Address<?>, List<Callback<?>>>();
    }

    /** The name of the host with which we're communicating. */
    protected final String _host;

    /** Tracks pending subscribers. */
    protected final PenderMap _penders = new PenderMap();

    /** Tracks pending service calls. */
    protected final Map<Integer, Callback<?>> _calls = new HashMap<Integer, Callback<?>>();

    /** Tracks currently subscribed objects. */
    protected final Map<Integer, NexusObject> _objects = new HashMap<Integer, NexusObject>();
}
