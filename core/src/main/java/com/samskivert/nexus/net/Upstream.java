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

    /** Dispatches this message to the appropriate method on the supplied handler. */
    void dispatch (Handler handler);
}
