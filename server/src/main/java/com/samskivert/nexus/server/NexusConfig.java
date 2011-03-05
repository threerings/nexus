//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.server;

import java.util.Properties;

/**
 * Provides configuration information for a {@link NexusServer}.
 */
public class NexusConfig
{
    /** The name of this node, which must differ from all other nodes in the network. */
    public final String nodeName;

    /** The timeout, in milliseconds, of RPC requests to other nodes. */
    public final long rpcTimeout;

    /**
     * Creates a configuration instance, obtaining configuration values from the supplied
     * properties instance.
     */
    public NexusConfig (Properties props)
    {
        nodeName = require(props, "nexus.node");
        rpcTimeout = Long.parseLong(require(props, "nexus.rpc_timeout"));
    }

    protected String require (Properties props, String key)
    {
        String value = props.getProperty(key, System.getProperty(key));
        if (value == null || value.length() == 0) {
            throw new RuntimeException("Missing required property '" + key + "'");
        }
        return value;
    }
}
