//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.distrib;

import com.samskivert.nexus.distrib.NexusService;
import com.samskivert.nexus.util.Callback;

/**
 * A simple test service.
 */
public interface TestService extends NexusService
{
    /** Adds one to the supplied value. */
    void addOne (int value, Callback<Integer> callback);

    /** Launches the missiles. Who needs confirmation for that? Pah! */
    void launchMissiles ();
}
