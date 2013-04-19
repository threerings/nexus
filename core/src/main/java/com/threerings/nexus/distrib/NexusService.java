//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import com.threerings.nexus.io.Streamable;
import com.threerings.nexus.util.Callback;

/**
 * A marker interface identifying a Nexus distributed service.
 */
public interface NexusService
{
    /** If the object returned in a {@link Callback} contains any {@link NexusObject}s, the
     * returned object must implement this interface and provide a reference to the contained
     * objects. This is necessary for Nexus to register the objects with the system when they are
     * received over the network. */
    interface ObjectResponse extends Streamable {
        /** Returns the objects contained within this response object. */
        NexusObject[] getObjects ();
    }
}
