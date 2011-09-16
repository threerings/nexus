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
 * Handles the streaming of {@link Upstream} and/or nested classes.
 */
public class Streamer_Upstream
{
    /**
     * Handles the streaming of {@link Upstream.Subscribe} instances.
     */
    public static class Subscribe implements Streamer<Upstream.Subscribe> {
        @Override
        public Class<?> getObjectClass () {
            return Upstream.Subscribe.class;
        }

        @Override
        public void writeObject (Streamable.Output out, Upstream.Subscribe obj) {
            writeObjectImpl(out, obj);
        }

        @Override
        public Upstream.Subscribe readObject (Streamable.Input in) {
            return new Upstream.Subscribe(
                in.<Address<?>>readValue()
            );
        }

        public static  void writeObjectImpl (Streamable.Output out, Upstream.Subscribe obj) {
            out.writeValue(obj.addr);
        }
    }

    /**
     * Handles the streaming of {@link Upstream.Unsubscribe} instances.
     */
    public static class Unsubscribe implements Streamer<Upstream.Unsubscribe> {
        @Override
        public Class<?> getObjectClass () {
            return Upstream.Unsubscribe.class;
        }

        @Override
        public void writeObject (Streamable.Output out, Upstream.Unsubscribe obj) {
            writeObjectImpl(out, obj);
        }

        @Override
        public Upstream.Unsubscribe readObject (Streamable.Input in) {
            return new Upstream.Unsubscribe(
                in.readInt()
            );
        }

        public static  void writeObjectImpl (Streamable.Output out, Upstream.Unsubscribe obj) {
            out.writeInt(obj.id);
        }
    }

    /**
     * Handles the streaming of {@link Upstream.PostEvent} instances.
     */
    public static class PostEvent implements Streamer<Upstream.PostEvent> {
        @Override
        public Class<?> getObjectClass () {
            return Upstream.PostEvent.class;
        }

        @Override
        public void writeObject (Streamable.Output out, Upstream.PostEvent obj) {
            writeObjectImpl(out, obj);
        }

        @Override
        public Upstream.PostEvent readObject (Streamable.Input in) {
            return new Upstream.PostEvent(
                in.<NexusEvent>readValue()
            );
        }

        public static  void writeObjectImpl (Streamable.Output out, Upstream.PostEvent obj) {
            out.writeValue(obj.event);
        }
    }

    /**
     * Handles the streaming of {@link Upstream.ServiceCall} instances.
     */
    public static class ServiceCall implements Streamer<Upstream.ServiceCall> {
        @Override
        public Class<?> getObjectClass () {
            return Upstream.ServiceCall.class;
        }

        @Override
        public void writeObject (Streamable.Output out, Upstream.ServiceCall obj) {
            writeObjectImpl(out, obj);
        }

        @Override
        public Upstream.ServiceCall readObject (Streamable.Input in) {
            return new Upstream.ServiceCall(
                in.readInt(),
                in.readInt(),
                in.readShort(),
                in.readShort(),
                in.<List<Object>>readValue()
            );
        }

        public static  void writeObjectImpl (Streamable.Output out, Upstream.ServiceCall obj) {
            out.writeInt(obj.callId);
            out.writeInt(obj.objectId);
            out.writeShort(obj.attrIndex);
            out.writeShort(obj.methodId);
            out.writeValue(obj.args);
        }
    }

    // no streamer for non-Streamable enclosing class: Upstream
}
