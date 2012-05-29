//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.server;

import java.util.Properties;

/**
 * Provides configuration information for a {@link NexusServer}.
 */
public class NexusConfig
{
    /** The name of this node, which must differ from all other nodes in the network. */
    public final String nodeName;

    /** The public hostname of this node. This will be used by clients to connect to this node. */
    public final String publicHostname;

    /** The timeout, in milliseconds, of RPC requests to other nodes. */
    public final long rpcTimeout;

    /**
     * Creates a configuration instance, obtaining configuration values from the supplied
     * properties instance.
     */
    public NexusConfig (Properties props) {
        nodeName = require(props, "nexus.node");
        publicHostname = require(props, "nexus.hostname");
        rpcTimeout = Long.parseLong(require(props, "nexus.rpc_timeout"));
    }

    protected String require (Properties props, String key) {
        String value = props.getProperty(key, System.getProperty(key));
        if (value == null || value.length() == 0) {
            throw new RuntimeException("Missing required property '" + key + "'");
        }
        return value;
    }
}
