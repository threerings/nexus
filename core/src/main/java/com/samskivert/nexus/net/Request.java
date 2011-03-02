//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.net;

import com.samskivert.nexus.distrib.NexusEvent;
import com.samskivert.nexus.io.Streamable;

/**
 * A base interface for all request messages sent to a server.
 */
public interface Request extends Streamable
{
    /** A request to subscribe to a singleton object. */
    public static class Subscribe implements Request
    {
        /** The class name of the singleton object to which a subscription is desired. */
        public final String clazz;

        public Subscribe (String clazz) {
            this.clazz = clazz;
        }
    }

    /** A request to post an event on the server. */
    public static class PostEvent implements Request
    {
        /** The event to be posted. */
        public final NexusEvent event;

        public PostEvent (NexusEvent event) {
            this.event = event;
        }
    }
}
