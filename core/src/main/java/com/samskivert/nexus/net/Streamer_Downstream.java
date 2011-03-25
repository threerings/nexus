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
import com.samskivert.nexus.io.Streamer;

/**
 * Handles the streaming of {@link Downstream} messages.
 */
public class Streamer_Downstream
{
    public static class Subscribe implements Streamer<Downstream.Subscribe>
    {
        public void writeObject (Streamable.Output out, Downstream.Subscribe obj) {
            out.writeValue(obj.object);
        }
        public Downstream.Subscribe readObject (Streamable.Input in) {
            return new Downstream.Subscribe(in.<NexusObject>readValue());
        }
    }

    public static class SubscribeFailure implements Streamer<Downstream.SubscribeFailure>
    {
        public void writeObject (Streamable.Output out, Downstream.SubscribeFailure obj) {
            out.writeValue(obj.addr);
            out.writeString(obj.cause);
        }
        public Downstream.SubscribeFailure readObject (Streamable.Input in) {
            return new Downstream.SubscribeFailure(in.<Address<?>>readValue(), in.readString());
        }
    }

    public static class DispatchEvent implements Streamer<Downstream.DispatchEvent>
    {
        public void writeObject (Streamable.Output out, Downstream.DispatchEvent obj) {
            out.writeValue(obj.event);
        }
        public Downstream.DispatchEvent readObject (Streamable.Input in) {
            return new Downstream.DispatchEvent(in.<NexusEvent>readValue());
        }
    }

    public static class ServiceResponse implements Streamer<Downstream.ServiceResponse>
    {
        public void writeObject (Streamable.Output out, Downstream.ServiceResponse obj) {
            out.writeInt(obj.callId);
            out.writeValue(obj.result);
        }
        public Downstream.ServiceResponse readObject (Streamable.Input in) {
            return new Downstream.ServiceResponse(in.readInt(), in.readValue());
        }
    }

    public static class ServiceFailure implements Streamer<Downstream.ServiceFailure>
    {
        public void writeObject (Streamable.Output out, Downstream.ServiceFailure obj) {
            out.writeInt(obj.callId);
            out.writeString(obj.cause);
        }
        public Downstream.ServiceFailure readObject (Streamable.Input in) {
            return new Downstream.ServiceFailure(in.readInt(), in.readString());
        }
    }
}
