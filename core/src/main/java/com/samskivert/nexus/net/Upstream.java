//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.net;

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

        /** Dispatches a post event request. */
        void onPostEvent (PostEvent message);
    }

    /** A request to subscribe to a singleton object. */
    public static class Subscribe implements Upstream
    {
        /** The class name of the singleton object to which a subscription is desired. */
        public final String clazz;

        public Subscribe (String clazz) {
            this.clazz = clazz;
        }

        public void dispatch (Handler handler) {
            handler.onSubscribe(this);
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
