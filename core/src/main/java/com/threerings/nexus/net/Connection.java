//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.net;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import react.RFuture;
import react.RPromise;
import react.Signal;

import com.threerings.nexus.distrib.Address;
import com.threerings.nexus.distrib.DistribUtil;
import com.threerings.nexus.distrib.EventSink;
import com.threerings.nexus.distrib.NexusEvent;
import com.threerings.nexus.distrib.NexusException;
import com.threerings.nexus.distrib.NexusObject;
import com.threerings.nexus.util.Log;
import com.threerings.nexus.util.Util;

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
    public <T extends NexusObject> RFuture<T> subscribe (Address<T> addr) {
        @SuppressWarnings("unchecked") RPromise<T> promise = (RPromise<T>)_penders.get(addr);
        if (promise == null) {
            _penders.put(addr, promise = RPromise.create());
            send(new Upstream.Subscribe(addr));
        }
        return promise;
    }

    /**
     * Unsubscribes from the Nexus object with the specified id.
     */
    public void unsubscribe (int id) {
        if (_objects.remove(id) == null) {
            _log.warning("Requested to unsubscribe from unknown object", "id", id);
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
    public <R> void postCall (NexusObject source, short attrIndex, short methodId, Object[] args,
                              RPromise<R> result) {
        // if we have a result, assign an id to this call, and save the result for later
        int callId;
        if (result == null) callId = 0;
        else {
            callId = 1;
            for (Integer key : _calls.keySet()) {
                callId = Math.max(callId, key+1);
            }
            _calls.put(callId, result);
        }
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
        @SuppressWarnings("unchecked") RPromise<NexusObject> promise =
            (RPromise<NexusObject>)_penders.remove(addr);
        if (promise == null) {
            _log.warning("No one pending on object?", "addr", addr);
            send(new Upstream.Unsubscribe(msg.object.getId())); // clear our subscription
            return;
        }

        // store the object in our local table
        _objects.put(msg.object.getId(), msg.object);

        // notify the pending listeners that the object has arrived
        promise.succeed(msg.object);
    }

    // from interface Downstream.Handler
    public void onSubscribeFailure (Downstream.SubscribeFailure msg) {
        RPromise<?> promise = _penders.remove(msg.addr);
        if (promise != null) promise.fail(new NexusException(msg.cause));
        else _log.warning("No one pending on failed subscribe?", "addr", msg.addr, "err", msg.cause);
    }

    // from interface Downstream.Handler
    public void onDispatchEvent (final Downstream.DispatchEvent msg) {
        final NexusObject target = _objects.get(msg.event.targetId);
        if (target == null) {
            _log.warning("Missing target of event", "event", msg.event);
            return;
        }
        try {
            msg.event.applyTo(target);
        } catch (Throwable err) {
            _log.warning("Event dispatch failure", "target", target, "msg", msg, err);
        }
    }

    // from interface Downstream.Handler
    public void onServiceResponse (Downstream.ServiceResponse msg) {
        RPromise<?> promise = _calls.remove(msg.callId);
        if (promise == null) _log.warning("Received service response for unknown call", "msg", msg);
        else {
            @SuppressWarnings("unchecked") RPromise<Object> pr = (RPromise<Object>)promise;
            pr.succeed(msg.result);
        }
    }

    // from interface Downstream.Handler
    public void onServiceFailure (Downstream.ServiceFailure msg) {
        RPromise<?> promise = _calls.remove(msg.callId);
        if (promise == null) {
            _log.warning("Received service failure for unknown call", "msg", msg);
            return;
        }
        promise.fail(new NexusException(msg.cause));
    }

    // from interface Downstream.Handler
    public void onObjectCleared (Downstream.ObjectCleared msg) {
        final NexusObject target = _objects.remove(msg.id);
        if (target == null) {
            _log.warning("Unknown object cleared", "id", msg.id);
            return;
        }
        target.onLost.emit(null);
    }

    protected Connection (Log.Logger log, String host) {
        _log = log;
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
        final Throwable perror = (error != null) ? error : new NexusException("Connection closed");

        // do the bulk of our close processing on the dispatch thread
        dispatch(new Runnable() {
            public void run () {
                // notify any penders that we're not going to hear back
                for (RPromise<?> promise : _penders.values()) promise.fail(perror);

                // notify any in-flight service calls that they failed
                if (!_calls.isEmpty()) {
                    _log.info("Clearing " + _calls.size() + " calls.");
                    for (RPromise<?> pr : _calls.values()) pr.fail(perror);
                    _calls.clear();
                }

                // notify any dangling objects that they were lost
                if (!_objects.isEmpty()) {
                    _log.info("Clearing " + _objects.size() + " objects.");
                    for (NexusObject obj : _objects.values()) Util.emit(obj.onLost, perror);
                    _objects.clear();
                }

                // finally notify listeners of our closure (here error may be null)
                Util.emit(onClose, error);
            }
        });
    }

    /** The logger to which we send log messages. */
    protected final Log.Logger _log;

    /** The name of the host with which we're communicating. */
    protected final String _host;

    /** Tracks pending subscribers. */
    protected final Map<Address<?>, RPromise<?>> _penders = new HashMap<Address<?>, RPromise<?>>();

    /** Tracks pending service calls. */
    protected final Map<Integer, RPromise<?>> _calls = new HashMap<Integer, RPromise<?>>();

    /** Tracks currently subscribed objects. */
    protected final Map<Integer, NexusObject> _objects = new HashMap<Integer, NexusObject>();
}
