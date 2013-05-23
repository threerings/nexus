//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import com.threerings.nexus.io.Streamable;

/**
 * Handles the streaming of {@link DAttribute} and/or nested classes.
 */
public class Streamer_DAttribute
{
    /**
     * Handles the streaming of {@link DAttribute.Event} instances.
     */
    public static class Event
    {
        public static  void writeObjectImpl (Streamable.Output out, DAttribute.Event obj) {
            Streamer_NexusEvent.writeObjectImpl(out, obj);
            out.writeShort(obj.index);
        }
    }

    // no streamer for non-Streamable enclosing class: DAttribute
}
