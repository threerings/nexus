//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.distrib;

/**
 * Provides access to interfaces that should not be called by normal clients, but which must be
 * accessible to the Nexus implementation code, which may reside in a different package.
 */
public class DistribUtil
{
    public static void init (NexusObject object, int id, EventSink sink)
    {
        object.init(id, sink);
    }

    public static void clear (NexusObject object)
    {
        object.clear();
    }

    private DistribUtil () {} // no constructsky
}
