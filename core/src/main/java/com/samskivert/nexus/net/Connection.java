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

import com.samskivert.nexus.distrib.Dispatcher;
import com.samskivert.nexus.distrib.EventSink;
import com.samskivert.nexus.distrib.NexusEvent;
import com.samskivert.nexus.distrib.NexusObject;
import com.samskivert.nexus.util.Callback;

import static com.samskivert.nexus.util.Log.log;

/**
 * Manages a connection to a particular server.
 */
public abstract class Connection
    implements Response.Handler, EventSink
{
    /**
     * Requests to subscribe to the specified singleton Nexus object. Success (i.e. the object) or
     * failure will be communicated via the supplied callback.
     */
    public <T extends NexusObject> void subscribe (Class<T> clazz, Callback<T> cb)
    {
        String oclass = clazz.getName();
        if (!_penders.addPender(oclass, cb)) {
            sendRequest(new Request.Subscribe(oclass));
        }
    }

    // TODO: how to communicate connection termination/failure?

    // from EventSink
    public void postEvent (NexusObject source, NexusEvent event)
    {
        sendRequest(new Request.PostEvent(event));
    }

    // from interface Response.Handler
    public void onSubscribe (Response.Subscribe response)
    {
        String oclass = response.object.getClass().getName();
        List<Callback<?>> penders = _penders.getPenders(oclass);
        if (penders == null) {
            log.warning("Missing pender list", "oclass", oclass);
            // TODO: clear our subscription
        } else {
            response.object.init(this); // we are this object's event sink
            for (Callback<?> pender : penders) {
                @SuppressWarnings("unchecked") Callback<NexusObject> cb =
                    (Callback<NexusObject>)pender;
                cb.onSuccess(response.object);
            }
        }
    }

    // from interface Response.Handler
    public void onSubscribeFailure (Response.SubscribeFailure response)
    {
        List<Callback<?>> penders = _penders.getPenders(response.oclass);
        if (penders == null) {
            log.warning("Missing pender list", "oclass", response.oclass);
        } else {
            Exception cause = new Exception(response.cause);
            for (Callback<?> pender : penders) {
                pender.onFailure(cause);
            }
        }
    }

    // from interface Response.Handler
    public void onDispatchEvent (Response.DispatchEvent response)
    {
        // the dispatcher will locate the target object and dispatch the event
        _dispatcher.dispatchEvent(response.event);
    }

    protected Connection (Dispatcher dispatcher)
    {
        _dispatcher = dispatcher;
    }

    /**
     * Called to send a request message to the server.
     */
    protected abstract void sendRequest (Request request);

    /**
     * Called when a response message is received from the server.
     */
    protected void onResponse (Response response)
    {
        response.dispatch(this);
    }

    /** This map is accessed by multiple threads, be sure its method are synchronized. */
    protected static class PenderMap
    {
        public synchronized boolean addPender (String oclass, Callback<?> cb) {
            boolean wasPending = true;
            List<Callback<?>> penders = _penders.get(oclass);
            if (penders == null) {
                _penders.put(oclass, penders = new ArrayList<Callback<?>>());
                wasPending = false;
            }
            penders.add(cb);
            return wasPending;
        }

        public synchronized List<Callback<?>> getPenders (String oclass) {
            return _penders.remove(oclass);
        }

        protected Map<String, List<Callback<?>>> _penders = new HashMap<String, List<Callback<?>>>();
    }

    /** Handles the dispatch of events received from the server. */
    protected final Dispatcher _dispatcher;

    /** A map of singleton object class to pending subscriber list. */
    protected final PenderMap _penders = new PenderMap();
}
