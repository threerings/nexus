//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.server;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.*;
import static org.junit.Assert.*;

import com.threerings.nexus.distrib.Action;
import com.threerings.nexus.distrib.Nexus;
import com.threerings.nexus.distrib.Request;

import static com.threerings.nexus.server.TestUtil.*;

/**
 * Tests the deferred action services of the Nexus server.
 */
public class DeferredActionTest
{
    @Before
    public void createServer () {
        _exec = Executors.newFixedThreadPool(3);
        _server = new NexusServer(createTestConfig(), _exec);
        TestSingleton test = new TestSingleton();
        _server.registerSingleton(test);
    }

    @After
    public void shutdownServer () {
        _server.shutdown();
        _exec.shutdown();
        TestUtil.awaitTermination(_exec);
    }

    @Test
    public void testNonDeferredAction () {
        // ensure that non-deferred actions are dispatched "quickly"
        final long start = System.currentTimeMillis();
        final long[] invokedAt = new long[1];
        _server.invoke(TestSingleton.class, new Action<TestSingleton>() {
            public void invoke (TestSingleton obj) {
                invokedAt[0] = System.currentTimeMillis();
            }
        });

        _exec.execute(new Runnable() {
            public void run () {
                long elapsed = invokedAt[0] - start;
                assertTrue(elapsed < 20);
            }
        });
    }

    @Test
    public void testDeferredAction () {
        final long start = System.currentTimeMillis();
        final long[] invokedAt = new long[1];
        _server.invokeAfter(TestSingleton.class, 20, new Action<TestSingleton>() {
            public void invoke (TestSingleton obj) {
                invokedAt[0] = System.currentTimeMillis();
            }
        });

        delay(30);
        _exec.execute(new Runnable() {
            public void run () {
                long elapsed = invokedAt[0] - start;
                assertFalse(elapsed < 20);
            }
        });
    }

    @Test
    public void testCanceledAction () {
        final long[] invokedAt = new long[1];
        Nexus.Deferred def = _server.invokeAfter(
            TestSingleton.class, 20, new Action<TestSingleton>() {
                public void invoke (TestSingleton obj) {
                    invokedAt[0] = System.currentTimeMillis();
                }
            });
        def.cancel();

        delay(30);
        _exec.execute(new Runnable() {
            public void run () {
                assertEquals(0, invokedAt[0]);
            }
        });
    }

    @Test
    public void testRepeatedAction () {
        final int[] invoked = new int[1];
        Nexus.Deferred def = _server.invokeAfter(
            TestSingleton.class, 20, new Action<TestSingleton>() {
                public void invoke (TestSingleton obj) {
                    invoked[0]++;
                }
            });
        def.repeatEvery(20);

        delay(90);
        def.cancel();

        delay(30);
        _exec.execute(new Runnable() {
            public void run () {
                assertEquals(4, invoked[0]);
            }
        });
    }

    protected void delay (long millis) {
        try { Thread.sleep(millis); }
        catch (InterruptedException ie) {}
    }

    protected ExecutorService _exec;
    protected NexusServer _server;
}
