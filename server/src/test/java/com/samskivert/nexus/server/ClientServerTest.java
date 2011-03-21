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
import com.samskivert.nexus.distrib.Address;
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
        NexusServer server = new NexusServer(config, exec);

        // set up a connection manager and listen on a port
        final JVMConnectionManager conmgr = new JVMConnectionManager(server.getSessionManager());
        conmgr.listen("localhost", 1234);
        conmgr.start();

        // register a test object
        TestObject test = new TestObject();
        server.registerSingleton(test);
        test.value.update("bob");

        // create a client connection to said server
        NexusClient client = JVMClient.create(Executors.newSingleThreadExecutor(), 1234);

        // subscribe to the test object
        client.subscribe(Address.create("localhost", TestObject.class), new Callback<TestObject>() {
            public void onSuccess (TestObject test) {
                System.err.println("Got test object, value = " + test.value.get());
            }
            public void onFailure (Throwable cause) {
                fail("Failed to subscribe to test object " + cause);
            }
        });

        TestUtil.awaitTermination(exec);
    }
}
