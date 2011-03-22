//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.io;

import com.samskivert.nexus.distrib.DService;
import com.samskivert.nexus.distrib.NexusService;

/**
 * Used to create service marshallers.
 */
public interface ServiceFactory<T extends NexusService>
{
    /** Creates a marshaller for our service bound to the supplied attribute. */
    T createMarshaller (DService<T> attr);
}
