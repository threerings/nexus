//
// Nexus Test Support - shared test infrastructure for Nexus framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import com.threerings.nexus.distrib.NexusService;
import com.threerings.nexus.util.Callback;

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
