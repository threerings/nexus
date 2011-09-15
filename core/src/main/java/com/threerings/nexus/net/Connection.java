//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.net;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.threerings.nexus.distrib.Address;
import com.threerings.nexus.distrib.EventSink;
import com.threerings.nexus.distrib.DistribUtil;
import com.threerings.nexus.distrib.NexusEvent;
import com.threerings.nexus.distrib.NexusException;
import com.threerings.nexus.distrib.NexusObject;
import com.threerings.nexus.util.Callback;

import static com.threerings.nexus.util.Log.log;

/**
 * Manages a connection to a particular server.
 */
public abstract class Connection
    implements Downstream.Handler, EventSink
{
    /**
     * Requests to subscribe to the specified Nexus object. Success (i.e. the object) or failure
     * will be communicated via the supplied callback.
     */
    public <T extends NexusObject> void subscribe (Address<T> addr, Callback<T> cb)
    {
        if (!_penders.addPender(addr, cb)) {
            send(new Upstream.Subscribe(addr));
        }
    }

    /**
     * Requests to unsubscribe from the specified Nexus object.
     */
    public void unsubscribe (NexusObject object)
    {
        if (_objects.remove(object.getId()) == null) {
            log.warning("Requested to unsubscribe from unknown object", "id", object.getId());
            return;
        }

        // TODO: make a note that we just unsubscribed, and so not to warn about events that arrive
        // for this object in the near future (the server doesn't yet know that we've unsubscribed)
        send(new Upstream.Unsubscribe(object.getId()));
    }

    // TODO: how to communicate connection termination/failure?

    /**
     * Closes this connection in an orderly fashion. Any messages currently queued up should be
     * delivered prior to closure.
     */
    public abstract void close ();

    // from interface EventSink
    public String getHost ()
    {
        return _host;
    }

    // from interface EventSink
    public void postEvent (NexusObject source, NexusEvent event)
    {
        send(new Upstream.PostEvent(event));
    }

    // from interface EventSink
    public void postCall (NexusObject source, short attrIndex, short methodId, Object[] args)
    {
        // determine whether we have a callback (which the service generator code will enforce is
        // always the final argument of the method)
        int callId, lastIdx = args.length-1;
        if (args[lastIdx] instanceof Callback<?>) {
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
    public void onSubscribe (Downstream.Subscribe msg)
    {
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
            cb.onSuccess(msg.object);
        }
    }

    // from interface Downstream.Handler
    public void onSubscribeFailure (Downstream.SubscribeFailure msg)
    {
        List<Callback<?>> penders = _penders.getPenders(msg.addr);
        if (penders == null) {
            log.warning("Missing pender list", "addr", msg.addr);
        } else {
            Exception cause = new Exception(msg.cause);
            for (Callback<?> pender : penders) {
                pender.onFailure(cause);
            }
        }
    }

    // from interface Downstream.Handler
    public void onDispatchEvent (final Downstream.DispatchEvent msg)
    {
        final NexusObject target = _objects.get(msg.event.targetId);
        if (target == null) {
            log.warning("Missing target of event", "event", msg.event);
            return;
        }
        msg.event.applyTo(target);
    }

    // from interface Downstream.Handler
    public void onServiceResponse (Downstream.ServiceResponse msg)
    {
        Callback<?> callback = _calls.remove(msg.callId);
        if (callback == null) {
            log.warning("Received service response for unknown call", "msg", msg);
            return;
        }
        // if the result is a NexusObject, it must be initialized
        if (msg.result instanceof NexusObject) {
            NexusObject obj = (NexusObject)msg.result;
            DistribUtil.init(obj, obj.getId(), this);
        }
        try {
            @SuppressWarnings("unchecked") Callback<Object> ccb = (Callback<Object>)callback;
            ccb.onSuccess(msg.result);
        } catch (Throwable t) {
            log.warning("Failure delivering service response", t);
        }
    }

    // from interface Downstream.Handler
    public void onServiceFailure (Downstream.ServiceFailure msg)
    {
        Callback<?> callback = _calls.remove(msg.callId);
        if (callback == null) {
            log.warning("Received service failure for unknown call", "msg", msg);
            return;
        }
        try {
            callback.onFailure(new NexusException(msg.cause));
        } catch (Throwable t) {
            log.warning("Failure delivering service failure", t);
        }
    }

    protected Connection (String host)
    {
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
    protected void onReceive (final Downstream message)
    {
        log.info("Received "+ message);
        dispatch(new Runnable() {
            public void run () {
                message.dispatch(Connection.this);
            }
        });
    }

    /** This map may be accessed by multiple threads, be sure its methods are synchronized. */
    protected static class PenderMap
    {
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
