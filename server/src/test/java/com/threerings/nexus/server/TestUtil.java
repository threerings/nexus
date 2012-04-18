//
// $Id$
//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.server;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.threerings.nexus.distrib.DService;
import com.threerings.nexus.distrib.Factory_TestService;
import com.threerings.nexus.distrib.Keyed;
import com.threerings.nexus.distrib.Singleton;
import com.threerings.nexus.distrib.TestService;
import com.threerings.nexus.util.Callback;

import org.junit.Assert;

/**
 * Test-related utility methods.
 */
public class TestUtil
{
    public static class TestSingleton implements Singleton {
        public int increment (int value) {
            return value+1;
        }
    }

    public static class ChildSingleton extends TestSingleton {
    }

    public static class TestKeyed implements Keyed {
        public TestKeyed (int key) {
            _key = key;
        }

        public Comparable<?> getKey () {
            return _key;
        }

        public int decrement (int value) {
            return value-1;
        }

        protected Integer _key; // box once instead of on every getKey call
    }

    public static class ChildKeyed extends TestKeyed {
        public ChildKeyed (int key) {
            super(key);
        }
    }

    public static NexusConfig createTestConfig () {
        Properties props = new Properties();
        props.setProperty("nexus.node", "test");
        props.setProperty("nexus.hostname", "localhost");
        props.setProperty("nexus.rpc_timeout", "1000");
        return new NexusConfig(props);
    }

    public static void awaitTermination (ExecutorService exec) {
        try {
            if (!exec.awaitTermination(2, TimeUnit.SECONDS)) { // TODO: change back to 10
                Assert.fail("Executor failed to terminate after 10 seconds.");
            }
        } catch (InterruptedException ie) {
            Assert.fail("Executor interrupted?");
        }
    }

    public static DService<TestService> createTestServiceAttr () {
        return Factory_TestService.createDispatcher(new TestService () {
            public void addOne (int value, Callback<Integer> callback) {
                callback.onSuccess(value+1);
            }
            public void launchMissiles () {
                System.err.println("Bang!");
            }
        });
    }
}
