//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.net;

import com.samskivert.nexus.distrib.Address;
import com.samskivert.nexus.distrib.NexusEvent;
import com.samskivert.nexus.io.Streamable;
import com.samskivert.nexus.io.Streamer;

/**
 * Handles the streaming of {@link Upstream} messages.
 */
public class Streamer_Upstream
{
    public static class Subscribe implements Streamer<Upstream.Subscribe>
    {
        public void writeObject (Streamable.Output out, Upstream.Subscribe obj) {
            out.writeValue(obj.addr);
        }
        public Upstream.Subscribe readObject (Streamable.Input in) {
            return new Upstream.Subscribe(in.<Address<?>>readValue());
        }
    }

    public static class Unsubscribe implements Streamer<Upstream.Unsubscribe>
    {
        public void writeObject (Streamable.Output out, Upstream.Unsubscribe obj) {
            out.writeInt(obj.id);
        }
        public Upstream.Unsubscribe readObject (Streamable.Input in) {
            return new Upstream.Unsubscribe(in.readInt());
        }
    }

    public static class PostEvent implements Streamer<Upstream.PostEvent>
    {
        public void writeObject (Streamable.Output out, Upstream.PostEvent obj) {
            out.writeValue(obj.event);
        }
        public Upstream.PostEvent readObject (Streamable.Input in) {
            return new Upstream.PostEvent(in.<NexusEvent>readValue());
        }
    }
}
