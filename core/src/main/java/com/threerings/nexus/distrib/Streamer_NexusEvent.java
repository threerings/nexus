//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import com.threerings.nexus.io.Streamable;

/**
 * Handles the streaming of {@link NexusEvent} and/or nested classes.
 */
public class Streamer_NexusEvent
{
    public static  void writeObjectImpl (Streamable.Output out, NexusEvent obj) {
        out.writeInt(obj.targetId);
    }
}
