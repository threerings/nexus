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
 * Handles the streaming of {@link Downstream} and/or nested classes.
 */
public class Streamer_Downstream
{
    /**
     * Handles the streaming of {@link Downstream.Subscribe} instances.
     */
    public static class Subscribe
        implements Streamer<Downstream.Subscribe>
    {
        @Override
        public Class<?> getObjectClass () {
            return Downstream.Subscribe.class;
        }

        @Override
        public void writeObject (Streamable.Output out, Downstream.Subscribe obj) {
            writeObjectImpl(out, obj);
        }

        @Override
        public Downstream.Subscribe readObject (Streamable.Input in) {
            return new Downstream.Subscribe(
                in.<NexusObject>readValue()
            );
        }

        public static  void writeObjectImpl (Streamable.Output out, Downstream.Subscribe obj) {
            out.writeValue(obj.object);
        }
    }

    /**
     * Handles the streaming of {@link Downstream.SubscribeFailure} instances.
     */
    public static class SubscribeFailure
        implements Streamer<Downstream.SubscribeFailure>
    {
        @Override
        public Class<?> getObjectClass () {
            return Downstream.SubscribeFailure.class;
        }

        @Override
        public void writeObject (Streamable.Output out, Downstream.SubscribeFailure obj) {
            writeObjectImpl(out, obj);
        }

        @Override
        public Downstream.SubscribeFailure readObject (Streamable.Input in) {
            return new Downstream.SubscribeFailure(
                in.<Address<?>>readValue(),
                in.readString()
            );
        }

        public static  void writeObjectImpl (Streamable.Output out, Downstream.SubscribeFailure obj) {
            out.writeValue(obj.addr);
            out.writeString(obj.cause);
        }
    }

    /**
     * Handles the streaming of {@link Downstream.DispatchEvent} instances.
     */
    public static class DispatchEvent
        implements Streamer<Downstream.DispatchEvent>
    {
        @Override
        public Class<?> getObjectClass () {
            return Downstream.DispatchEvent.class;
        }

        @Override
        public void writeObject (Streamable.Output out, Downstream.DispatchEvent obj) {
            writeObjectImpl(out, obj);
        }

        @Override
        public Downstream.DispatchEvent readObject (Streamable.Input in) {
            return new Downstream.DispatchEvent(
                in.<NexusEvent>readValue()
            );
        }

        public static  void writeObjectImpl (Streamable.Output out, Downstream.DispatchEvent obj) {
            out.writeValue(obj.event);
        }
    }

    /**
     * Handles the streaming of {@link Downstream.ServiceResponse} instances.
     */
    public static class ServiceResponse
        implements Streamer<Downstream.ServiceResponse>
    {
        @Override
        public Class<?> getObjectClass () {
            return Downstream.ServiceResponse.class;
        }

        @Override
        public void writeObject (Streamable.Output out, Downstream.ServiceResponse obj) {
            writeObjectImpl(out, obj);
        }

        @Override
        public Downstream.ServiceResponse readObject (Streamable.Input in) {
            return new Downstream.ServiceResponse(
                in.readInt(),
                in.<Object>readValue()
            );
        }

        public static  void writeObjectImpl (Streamable.Output out, Downstream.ServiceResponse obj) {
            out.writeInt(obj.callId);
            out.writeValue(obj.result);
        }
    }

    /**
     * Handles the streaming of {@link Downstream.ServiceFailure} instances.
     */
    public static class ServiceFailure
        implements Streamer<Downstream.ServiceFailure>
    {
        @Override
        public Class<?> getObjectClass () {
            return Downstream.ServiceFailure.class;
        }

        @Override
        public void writeObject (Streamable.Output out, Downstream.ServiceFailure obj) {
            writeObjectImpl(out, obj);
        }

        @Override
        public Downstream.ServiceFailure readObject (Streamable.Input in) {
            return new Downstream.ServiceFailure(
                in.readInt(),
                in.readString()
            );
        }

        public static  void writeObjectImpl (Streamable.Output out, Downstream.ServiceFailure obj) {
            out.writeInt(obj.callId);
            out.writeString(obj.cause);
        }
    }

    // no streamer for non-Streamable enclosing class: Downstream
}
