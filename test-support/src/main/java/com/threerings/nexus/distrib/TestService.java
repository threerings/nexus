//
// Nexus Test Support - shared test infrastructure for Nexus framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import react.RFuture;

import com.threerings.nexus.distrib.NexusService;

/**
 * A simple test service.
 */
public interface TestService extends NexusService
{
    /** Adds one to the supplied value. */
    RFuture<Integer> addOne (int value);

    /** Launches the missiles. Who needs confirmation for that? Pah! */
    void launchMissiles ();
}
