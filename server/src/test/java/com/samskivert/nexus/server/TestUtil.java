//
// $Id$

package com.samskivert.nexus.server;

import java.util.Properties;

/**
 * Test-related utility methods.
 */
public class TestUtil
{
    public static NexusConfig createTestConfig ()
    {
        Properties props = new Properties();
        props.setProperty("nexus.node", "test");
        props.setProperty("nexus.rpc_timeout", "1000");
        return new NexusConfig(props);
    }
}
