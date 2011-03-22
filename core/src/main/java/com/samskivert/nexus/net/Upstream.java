//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.net;

import com.samskivert.nexus.distrib.Address;
import com.samskivert.nexus.distrib.NexusEvent;
import com.samskivert.nexus.io.Streamable;

/**
 * A base interface for all messages sent to a server.
 */
public interface Upstream extends Streamable
{
    /** Used to dispatch upstream messages. */
    public interface Handler {
        /** Dispatches a subscribe request. */
        void onSubscribe (Subscribe message);

        /** Dispatches an unsubscribe request. */
        void onUnsubscribe (Unsubscribe message);

        /** Dispatches a post event request. */
        void onPostEvent (PostEvent message);

        /** Dispatches a service call request. */
        void onServiceCall (ServiceCall message);
    }

    /** A request to subscribe to a Nexus object. */
    public static class Subscribe implements Upstream
    {
        /** The address of object to which a subscription is desired. */
        public final Address<?> addr;

        public Subscribe (Address<?> addr) {
            this.addr = addr;
        }

        public void dispatch (Handler handler) {
            handler.onSubscribe(this);
        }
    }

    public static class Unsubscribe implements Upstream
    {
        /** The id of the object from which we are unsubscribing. */
        public final int id;

        public Unsubscribe (int id) {
            this.id = id;
        }

        public void dispatch (Handler handler) {
            handler.onUnsubscribe(this);
        }
    }

    /** A request to post an event on the server. */
    public static class PostEvent implements Upstream
    {
        /** The event to be posted. */
        public final NexusEvent event;

        public PostEvent (NexusEvent event) {
            this.event = event;
        }

        public void dispatch (Handler handler) {
            handler.onPostEvent(this);
        }
    }

    /** A request to post a service call on the server. */
    public static class ServiceCall implements Upstream
    {
        /** An id that will be used to correlate this call with a response. If the id is zero, the
         * call is not expecting a response. */
        public final int callId;

        /** The id of the object that contains the service in question. */
        public final int objectId;

        /** The index of the service attribute on said object. */
        public final short attrIndex;

        /** The id of the method to be called. */
        public final short methodId;

        /** The arguments to be supplied to the method call. */
        public final Object[] args;

        public ServiceCall (int callId, int objectId, short attrIndex,
                            short methodId, Object[] args) {
            this.callId = callId;
            this.objectId = objectId;
            this.attrIndex = attrIndex;
            this.methodId = methodId;
            this.args = args;
        }

        public void dispatch (Handler handler) {
            handler.onServiceCall(this);
        }
    }

    /** Dispatches this message to the appropriate method on the supplied handler. */
    void dispatch (Handler handler);
}
