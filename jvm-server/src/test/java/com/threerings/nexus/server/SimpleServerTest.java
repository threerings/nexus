//
// Nexus JVMServer - server-side support for Nexus java.nio-based services
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.server;

import java.io.IOException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.threerings.nexus.distrib.Action;
import com.threerings.nexus.distrib.DValue;
import com.threerings.nexus.distrib.TestObject;

import org.junit.*;
import static org.junit.Assert.*;
import static com.threerings.nexus.util.Log.log;

/**
 * Tests basic Nexus server functionality.
 */
public class SimpleServerTest
{
    @Before
    public void suppressLogging () {
        log.setWarnOnly();
    }

    @Test
    public void testStartupShutdown () {
        // create a server with a thread pool
        NexusConfig config = TestUtil.createTestConfig();
        final ExecutorService exec = Executors.newFixedThreadPool(3);
        NexusServer server = new NexusServer(config, exec);

        // run a simple event dispatch through
        testEventDispatchAndShutdown(server, new Runnable() {
            public void run () {
                exec.shutdown();
            }
        });

        // now wait for everything to run to completion
        TestUtil.awaitTermination(exec);
    }

    @Test
    public void testStartupShutdownWithConMgr () throws IOException {
        // create a server with a thread pool
        NexusConfig config = TestUtil.createTestConfig();
        final ExecutorService exec = Executors.newFixedThreadPool(3);
        NexusServer server = new NexusServer(config, exec);

        // set up a connection manager and listen on a port
        final JVMConnectionManager conmgr = new JVMConnectionManager(server.getSessionManager());
        conmgr.listen("localhost", 1234);
        conmgr.start();

        // run a simple event dispatch through
        testEventDispatchAndShutdown(server, new Runnable() {
            public void run () {
                // since JUnit forcibly terminates the JVM on test completion, wait a few
                // milliseconds to give the reader and writer threads time to go away
                conmgr.disconnect();
                try { Thread.sleep(100); } catch (Throwable t) {}
                conmgr.shutdown();
                try { Thread.sleep(100); } catch (Throwable t) {}
                assert(conmgr.isTerminated());
                exec.shutdown();
            }
        });

        // now wait for everything to run to completion
        TestUtil.awaitTermination(exec);
    }

    protected void testEventDispatchAndShutdown (NexusServer server, final Runnable onComplete) {
        TestObject test = new TestObject(TestUtil.createTestServiceAttr());
        server.registerSingleton(test);

        // ensure that we've been assigned an id
        assertTrue(test.getId() > 0);

        // ensure that event dispatch is wired up
        final String ovalue = test.value.get();
        final String nvalue = "newValue";
        final boolean[] triggered = new boolean[1];
        test.value.connect(new DValue.Listener<String>() {
            @Override public void onChange (String value, String oldValue) {
                assertEquals(ovalue, oldValue);
                assertEquals(nvalue, value);
                triggered[0] = true;
            }
        });

        // update the value
        test.value.update(nvalue);

        // make sure the listener was triggered (we need to queue this check up on the object's
        // action queue so that we can be sure the attribute change goes through first)
        server.invoke(TestObject.class, new Action<TestObject>() {
            @Override public void invoke (TestObject entity) {
                assertTrue(triggered[0]);
                onComplete.run(); // let the caller know that we're done
            }
        });
    }
}
