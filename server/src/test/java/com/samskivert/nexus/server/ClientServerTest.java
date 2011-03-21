//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.server;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.samskivert.nexus.client.JVMClient;
import com.samskivert.nexus.client.NexusClient;
import com.samskivert.nexus.distrib.Action;
import com.samskivert.nexus.distrib.Address;
import com.samskivert.nexus.distrib.DValue;
import com.samskivert.nexus.distrib.TestObject;
import com.samskivert.nexus.util.Callback;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Tests simple client server communication.
 */
public class ClientServerTest
{
    @Test
    public void testClientServer ()
        throws IOException
    {
        // create a server with a thread pool
        NexusConfig config = TestUtil.createTestConfig();
        ExecutorService exec = Executors.newFixedThreadPool(3);
        final NexusServer server = new NexusServer(config, exec);

        // set up a connection manager and listen on a port
        final JVMConnectionManager conmgr = new JVMConnectionManager(server.getSessionManager());
        conmgr.listen("localhost", 1234);
        conmgr.start();

        // register a test object
        final TestObject test = new TestObject();
        server.registerSingleton(test);
        test.value.update("bob");

        // create a client connection to said server
        NexusClient client = JVMClient.create(Executors.newSingleThreadExecutor(), 1234);
        final boolean[] triggered = new boolean[1];

        // subscribe to the test object
        client.subscribe(Address.create("localhost", TestObject.class), new Callback<TestObject>() {
            public void onSuccess (TestObject ctest) {
                // make sure the value we got here is the same as the value from the server
                assertEquals(test.value.get(), ctest.value.get());

                // add a listener for changes to the test value
                final String ovalue = ctest.value.get();
                ctest.value.addListener(new DValue.Listener<String>() {
                    public void valueChanged (String value, String oldValue) {
                        assertEquals("updated", value);
                        assertEquals(ovalue, oldValue);
                        triggered[0] = true;
                    }
                });

                // update a test object value (over on the appropriate thread)
                server.invoke(TestObject.class, new Action<TestObject>() {
                    public void invoke (TestObject stest) {
                        stest.value.update("updated");
                    }
                });
            }
            public void onFailure (Throwable cause) {
                fail("Failed to subscribe to test object " + cause);
            }
        });

        // sleep for a bit to let everything process (we've got threads coming out the wazoo)
        try { Thread.sleep(1000); } catch (Throwable t) {}

        // make sure our innermost listener ran to completion
        assertTrue("Client value listener failed.", triggered[0]);

        // finally shut everything down
        exec.shutdown();
        TestUtil.awaitTermination(exec);
    }
}
