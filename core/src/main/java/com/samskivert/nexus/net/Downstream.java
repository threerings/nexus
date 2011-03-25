//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.net;

import com.samskivert.nexus.distrib.Address;
import com.samskivert.nexus.distrib.NexusEvent;
import com.samskivert.nexus.distrib.NexusObject;
import com.samskivert.nexus.io.Streamable;

/**
 * A base interface for all messages received from a server.
 */
public interface Downstream extends Streamable
{
    /** Used to dispatch downstream messages. */
    public interface Handler {
        /** Dispatches a successful subscribe response. */
        void onSubscribe (Subscribe message);

        /** Dispatches a failed subscribe response. */
        void onSubscribeFailure (SubscribeFailure message);

        /** Dispatches an event originating on the server. */
        void onDispatchEvent (DispatchEvent message);

        /** Dispatches a service response from the server. */
        void onServiceResponse (ServiceResponse message);

        /** Dispatches a failed service notification from the server. */
        void onServiceFailure (ServiceFailure message);
    }

    /** A successful response to a subscription request. */
    public static class Subscribe implements Downstream
    {
        /** The requested object. */
        public final NexusObject object;

        public Subscribe (NexusObject object) {
            assert(object != null); // le cheque du sanity
            this.object = object;
        }

        public void dispatch (Handler handler) {
            handler.onSubscribe(this);
        }
    }

    /** A failure response to a subscription request. */
    public static class SubscribeFailure implements Downstream
    {
        /** The address of the object requested. */
        public final Address<?> addr;

        /** The reason for the failure. */
        public final String cause;

        public SubscribeFailure (Address<?> addr, String cause) {
            // les cheques du sanity
            assert(addr != null);
            assert(cause != null);

            this.addr = addr;
            this.cause = cause;
        }

        public void dispatch (Handler handler) {
            handler.onSubscribeFailure(this);
        }
    }

    /** Notifies the client of an event originating from the server. */
    public static class DispatchEvent implements Downstream
    {
        /** The event to be dispatched. */
        public final NexusEvent event;

        public DispatchEvent (NexusEvent event) {
            assert(event != null); // le cheque du sanity
            this.event = event;
        }

        public void dispatch (Handler handler) {
            handler.onDispatchEvent(this);
        }
    }

    /** Delivers a response to a service call from the server. */
    public static class ServiceResponse implements Downstream
    {
        /** The id of the originating call. */
        public final int callId;

        /** The result of the call. */
        public final Object result;

        public ServiceResponse (int callId, Object result) {
            // le cheque du sanity
            assert(callId > 0);
            // result can be anything since the service returned it

            this.callId = callId;
            this.result = result;
        }

        public void dispatch (Handler handler) {
            handler.onServiceResponse(this);
        }
    }

    /** Delivers a failure response to a service call from the server. */
    public static class ServiceFailure implements Downstream
    {
        /** The id of the originating call. */
        public final int callId;

        /** The reason for the failure. */
        public final String cause;

        public ServiceFailure (int callId, String cause) {
            // les cheques du sanity
            assert(callId > 0);
            assert(cause != null);

            this.callId = callId;
            this.cause = cause;
        }

        public void dispatch (Handler handler) {
            handler.onServiceFailure(this);
        }
    }

    /** Dispatches this message to the appropriate method on the supplied handler. */
    void dispatch (Handler handler);
}
