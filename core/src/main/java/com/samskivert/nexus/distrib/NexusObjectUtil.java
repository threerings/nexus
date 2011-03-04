//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.distrib;

/**
 * Provides backdoor access to interfaces that should not be called by normal clients, but which
 * must be accessible to the Nexus server code, which necessarily resides in a different package.
 */
public class NexusObjectUtil
{
    public static void init (NexusObject object, int id, EventSink sink)
    {
        object.init(id, sink);
    }

    public static void clear (NexusObject object)
    {
        object.clear();
    }
}
