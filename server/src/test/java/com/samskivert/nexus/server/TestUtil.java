//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.server;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;

/**
 * Test-related utility methods.
 */
public class TestUtil
{
    public static NexusConfig createTestConfig ()
    {
        Properties props = new Properties();
        props.setProperty("nexus.node", "test");
        props.setProperty("nexus.hostname", "localhost");
        props.setProperty("nexus.rpc_timeout", "1000");
        return new NexusConfig(props);
    }

    public static void awaitTermination (ExecutorService exec)
    {
        try {
            if (!exec.awaitTermination(2, TimeUnit.SECONDS)) { // TODO: change back to 10
                Assert.fail("Executor failed to terminate after 10 seconds.");
            }
        } catch (InterruptedException ie) {
            Assert.fail("Executor interrupted?");
        }
    }
}
