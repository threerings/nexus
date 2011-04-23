//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.net;

import java.util.List;

import com.threerings.nexus.distrib.Address;
import com.threerings.nexus.distrib.NexusEvent;
import com.threerings.nexus.io.Streamable;
import com.threerings.nexus.io.Streamer;

/**
 * Handles the streaming of {@link Upstream} messages.
 */
public class Streamer_Upstream
{
    public static class Subscribe implements Streamer<Upstream.Subscribe>
    {
        public Class<?> getObjectClass () {
            return Upstream.Subscribe.class;
        }
        public void writeObject (Streamable.Output out, Upstream.Subscribe obj) {
            out.writeValue(obj.addr);
        }
        public Upstream.Subscribe readObject (Streamable.Input in) {
            return new Upstream.Subscribe(in.<Address<?>>readValue());
        }
    }

    public static class Unsubscribe implements Streamer<Upstream.Unsubscribe>
    {
        public Class<?> getObjectClass () {
            return Upstream.Unsubscribe.class;
        }
        public void writeObject (Streamable.Output out, Upstream.Unsubscribe obj) {
            out.writeInt(obj.id);
        }
        public Upstream.Unsubscribe readObject (Streamable.Input in) {
            return new Upstream.Unsubscribe(in.readInt());
        }
    }

    public static class PostEvent implements Streamer<Upstream.PostEvent>
    {
        public Class<?> getObjectClass () {
            return Upstream.PostEvent.class;
        }
        public void writeObject (Streamable.Output out, Upstream.PostEvent obj) {
            out.writeValue(obj.event);
        }
        public Upstream.PostEvent readObject (Streamable.Input in) {
            return new Upstream.PostEvent(in.<NexusEvent>readValue());
        }
    }

    public static class ServiceCall implements Streamer<Upstream.ServiceCall>
    {
        public Class<?> getObjectClass () {
            return Upstream.ServiceCall.class;
        }
        public void writeObject (Streamable.Output out, Upstream.ServiceCall obj) {
            out.writeInt(obj.callId);
            out.writeInt(obj.objectId);
            out.writeShort(obj.attrIndex);
            out.writeShort(obj.methodId);
            out.writeValue(obj.args);
        }
        public Upstream.ServiceCall readObject (Streamable.Input in) {
            return new Upstream.ServiceCall(in.readInt(), in.readInt(),
                                            in.readShort(), in.readShort(),
                                            in.<List<Object>>readValue());
        }
    }
}
