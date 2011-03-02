//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.net;

import com.samskivert.nexus.distrib.NexusEvent;
import com.samskivert.nexus.distrib.NexusObject;
import com.samskivert.nexus.io.Streamable;

/**
 * A base interface for all response messages received from a server.
 */
public interface Response extends Streamable
{
    /** Used to dispatch response messages. */
    public interface Handler {
        /** Dispatches a successful subscribe response. */
        void onSubscribe (Subscribe response);

        /** Dispatches a failed subscribe response. */
        void onSubscribeFailure (SubscribeFailure response);

        /** Dispatches an event originating on the server. */
        void onDispatchEvent (DispatchEvent response);
    }

    /** A successful response to a subscription request. */
    public static class Subscribe implements Response
    {
        /** The requested object. */
        public final NexusObject object;

        public Subscribe (NexusObject object) {
            this.object = object;
        }

        public void dispatch (Handler handler) {
            handler.onSubscribe(this);
        }
    }

    /** A failure response to a subscription request. */
    public static class SubscribeFailure implements Response
    {
        /** The name of the singleton object class requested. */
        public final String oclass;

        /** The reason for the failure. */
        public final String cause;

        public SubscribeFailure (String oclass, String cause) {
            this.oclass = oclass;
            this.cause = cause;
        }

        public void dispatch (Handler handler) {
            handler.onSubscribeFailure(this);
        }
    }

    /** Notifies the client of an event originating from the server. */
    public static class DispatchEvent implements Request
    {
        /** The event to be dispatched. */
        public final NexusEvent event;

        public DispatchEvent (NexusEvent event) {
            this.event = event;
        }

        public void dispatch (Handler handler) {
            handler.onDispatchEvent(this);
        }
    }

    /** Dispatches this response to the appropriate method on the supplied handler. */
    void dispatch (Handler handler);
}
