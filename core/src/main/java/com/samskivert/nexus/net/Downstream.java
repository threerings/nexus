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
    }

    /** A successful response to a subscription request. */
    public static class Subscribe implements Downstream
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
    public static class SubscribeFailure implements Downstream
    {
        /** The address of the object requested. */
        public final Address<?> addr;

        /** The reason for the failure. */
        public final String cause;

        public SubscribeFailure (Address<?> addr, String cause) {
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
            this.event = event;
        }

        public void dispatch (Handler handler) {
            handler.onDispatchEvent(this);
        }
    }

    /** Dispatches this message to the appropriate method on the supplied handler. */
    void dispatch (Handler handler);
}
