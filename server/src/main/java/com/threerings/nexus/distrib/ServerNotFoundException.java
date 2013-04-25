//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

/**
 * An exception thrown when a request is made to invoke an action or request on a specific server
 * in the network, but that server no longer exists. In general this will only happen when a server
 * unexpectedly fails.
 */
public class ServerNotFoundException extends NexusException
{
    /** The id of the server that is unknown. */
    public int serverId;

    public ServerNotFoundException (int serverId) {
        super("No server registered with id: ");
        this.serverId = serverId;
    }

    @Override public String getMessage () {
        return super.getMessage() + serverId;
    }
}
