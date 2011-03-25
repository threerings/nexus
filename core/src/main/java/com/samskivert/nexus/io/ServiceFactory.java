//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.io;

import com.samskivert.nexus.distrib.DService;
import com.samskivert.nexus.distrib.NexusService;

/**
 * Used to create service attributes.
 */
public interface ServiceFactory<T extends NexusService>
{
    /** Creates an attribute for our service that will dispatch calls over the network. */
    DService<T> createService ();
}
