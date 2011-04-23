//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.io;

import com.threerings.nexus.distrib.DService;
import com.threerings.nexus.distrib.NexusService;

/**
 * Used to create service attributes.
 */
public interface ServiceFactory<T extends NexusService>
{
    /**
     * Creates an attribute for our service that will marshall calls into network service requests.
     */
    DService<T> createService ();
}
