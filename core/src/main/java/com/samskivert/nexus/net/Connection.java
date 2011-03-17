//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.net;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.samskivert.nexus.distrib.Address;
import com.samskivert.nexus.distrib.Dispatcher;
import com.samskivert.nexus.distrib.EventSink;
import com.samskivert.nexus.distrib.NexusEvent;
import com.samskivert.nexus.distrib.NexusObject;
import com.samskivert.nexus.distrib.NexusObjectUtil;
import com.samskivert.nexus.util.Callback;

import static com.samskivert.nexus.util.Log.log;

/**
 * Manages a connection to a particular server.
 */
public abstract class Connection
    implements Downstream.Handler, EventSink
{
    /**
     * Requests to subscribe to the specified singleton Nexus object. Success (i.e. the object) or
     * failure will be communicated via the supplied callback.
     */
    public <T extends NexusObject> void subscribe (Address<T> addr, Callback<T> cb)
    {
        if (!_penders.addPender(addr, cb)) {
            send(new Upstream.Subscribe(addr));
        }
    }

    // TODO: how to communicate connection termination/failure?

    /**
     * Closes this connection in an orderly fashion. Any messages currently queued up should be
     * delivered prior to closure.
     */
    public abstract void close ();

    // from EventSink
    public abstract String getHost ();

    // from EventSink
    public void postEvent (NexusObject source, NexusEvent event)
    {
        send(new Upstream.PostEvent(event));
    }

    // from interface Downstream.Handler
    public void onSubscribe (Downstream.Subscribe msg)
    {
        // we are this object's event sink
        NexusObjectUtil.init(msg.object, msg.object.getId(), this);
        // the above must precede this call, as obtaining the object's address requires that the
        // event sink be configured
        Address<?> addr = msg.object.getAddress();
        List<Callback<?>> penders = _penders.getPenders(addr);
        if (penders == null) {
            log.warning("Missing pender list", "addr", addr);
            // TODO: clear our subscription
        } else {
            for (Callback<?> pender : penders) {
                @SuppressWarnings("unchecked") Callback<NexusObject> cb =
                    (Callback<NexusObject>)pender;
                cb.onSuccess(msg.object);
            }
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
    public void onDispatchEvent (Downstream.DispatchEvent msg)
    {
        // the dispatcher will locate the target object and dispatch the event
        _dispatcher.dispatchEvent(msg.event);
    }

    protected Connection (Dispatcher dispatcher)
    {
        _dispatcher = dispatcher;
    }

    /**
     * Called to send a request message to the server.
     */
    protected abstract void send (Upstream request);

    /**
     * Called when a message is received from the server.
     */
    protected void onReceive (Downstream message)
    {
        message.dispatch(this);
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

    /** Handles the dispatch of events received from the server. */
    protected final Dispatcher _dispatcher;

    /** A map of singleton object class to pending subscriber list. */
    protected final PenderMap _penders = new PenderMap();
}
