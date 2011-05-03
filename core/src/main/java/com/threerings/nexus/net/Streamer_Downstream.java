//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.net;

import com.threerings.nexus.distrib.Address;
import com.threerings.nexus.distrib.NexusEvent;
import com.threerings.nexus.distrib.NexusObject;
import com.threerings.nexus.io.Streamable;
import com.threerings.nexus.io.Streamer;

/**
 * Handles the streaming of {@link Downstream} messages.
 */
public class Streamer_Downstream
{
    public static class Subscribe implements Streamer<Downstream.Subscribe>
    {
        public Class<?> getObjectClass () {
            return Downstream.Subscribe.class;
        }
        public void writeObject (Streamable.Output out, Downstream.Subscribe obj) {
            out.writeValue(obj.object);
        }
        public Downstream.Subscribe readObject (Streamable.Input in) {
            return new Downstream.Subscribe(in.<NexusObject>readValue());
        }
    }

    public static class SubscribeFailure implements Streamer<Downstream.SubscribeFailure>
    {
        public Class<?> getObjectClass () {
            return Downstream.SubscribeFailure.class;
        }
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
        public Class<?> getObjectClass () {
            return Downstream.DispatchEvent.class;
        }
        public void writeObject (Streamable.Output out, Downstream.DispatchEvent obj) {
            out.writeValue(obj.event);
        }
        public Downstream.DispatchEvent readObject (Streamable.Input in) {
            return new Downstream.DispatchEvent(in.<NexusEvent>readValue());
        }
    }

    public static class ServiceResponse implements Streamer<Downstream.ServiceResponse>
    {
        public Class<?> getObjectClass () {
            return Downstream.ServiceResponse.class;
        }
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
        public Class<?> getObjectClass () {
            return Downstream.ServiceFailure.class;
        }
        public void writeObject (Streamable.Output out, Downstream.ServiceFailure obj) {
            out.writeInt(obj.callId);
            out.writeString(obj.cause);
        }
        public Downstream.ServiceFailure readObject (Streamable.Input in) {
            return new Downstream.ServiceFailure(in.readInt(), in.readString());
        }
    }
}
