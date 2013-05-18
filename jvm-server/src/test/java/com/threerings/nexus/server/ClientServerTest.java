//
// Nexus JVMServer - server-side support for Nexus java.nio-based services
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.server;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import react.Slot;

import com.threerings.nexus.client.JVMClient;
import com.threerings.nexus.client.NexusClient;
import com.threerings.nexus.distrib.Action;
import com.threerings.nexus.distrib.Address;
import com.threerings.nexus.distrib.DValue;
import com.threerings.nexus.distrib.TestObject;

import org.junit.*;
import static org.junit.Assert.*;
import static com.threerings.nexus.util.Log.log;

/**
 * Tests simple client server communication.
 */
public class ClientServerTest
{
    public abstract class TestAction {
        /** This method is run (on the test thread) after the server is started up. */
        public void onInit () {
            // nada
        }

        /** This method is run (on the client thread) in response to successful subscription. */
        public abstract void onSubscribe (TestObject test);

        /** Call this method when your testing is complete. This will trigger the shutdown of the
         * client and server and general cleanup. */
        protected void testComplete () {
            _latch.countDown();
        }

        protected void init (NexusServer server, NexusClient client, TestObject test) {
            _server = server;
            _client = client;
            _test = test;
        }

        protected boolean await () {
            try {
                return _latch.await(1, TimeUnit.SECONDS);
            } catch (InterruptedException ie) {
                fail("Test interrupted while awaiting completion?");
                return false;
            }
        }

        protected NexusServer _server;
        protected NexusClient _client;
        protected TestObject _test;
        protected CountDownLatch _latch = new CountDownLatch(1);
    }

    @Before
    public void suppressLogging () {
        log.setWarnOnly();
    }

    @Test
    public void testSubscribeAndAttrChange () throws IOException {
        runTest(new TestAction() {
            @Override public void onInit () {
                _test.value.update("bob");
            }
            @Override public void onSubscribe (TestObject test) {
                // make sure the value we got here is the same as the value from the server
                assertEquals(_test.value.get(), test.value.get());

                // add a listener for changes to the test value
                final String ovalue = test.value.get();
                test.value.connect(new DValue.Listener<String>() {
                    @Override public void onChange (String value, String oldValue) {
                        assertEquals("updated", value);
                        assertEquals(ovalue, oldValue);
                        testComplete();
                    }
                });

                // update a test object value (over on the appropriate thread)
                _server.invoke(TestObject.class, new Action<TestObject>() {
                    @Override public void invoke (TestObject stest) {
                        stest.value.update("updated");
                    }
                });
            }
        });
    }

    @Test
    public void testServiceCall () throws IOException {
        runTest(new TestAction() {
            @Override public void onSubscribe (TestObject test) {
                // call our test service
                test.testsvc.get().addOne(41).onSuccess(new Slot<Integer>() {
                    public void onEmit (Integer value) {
                        assertEquals(42, value.intValue());
                        testComplete();
                    }
                }).onFailure(new Slot<Throwable>() {
                    public void onEmit (Throwable cause) {
                        fail("Callback failed: " + cause.getMessage());
                    }
                });
            }
        });
    }

    protected void runTest (final TestAction action) throws IOException {
        // create a server with a thread pool
        NexusConfig config = TestUtil.createTestConfig();
        ExecutorService exec = Executors.newFixedThreadPool(3);
        final NexusServer server = new NexusServer(config, exec);

        // set up a connection manager and listen on a port
        final JVMConnectionManager conmgr = new JVMConnectionManager(server.getSessionManager());
        conmgr.listen("localhost", 1234);
        conmgr.start();

        // create a client connection to said server
        NexusClient client = JVMClient.create(Executors.newSingleThreadExecutor(), 1234);

        // register a test object
        TestObject test = new TestObject(TestUtil.createTestServiceAttr());
        server.registerSingleton(test);

        // initialize the action
        action.init(server, client, test);
        action.onInit();

        // subscribe to the test object
        client.<TestObject>subscriber().subscribe(Address.create("localhost", TestObject.class)).
            onSuccess(new Slot<TestObject>() {
                public void onEmit (TestObject test) {
                    action.onSubscribe(test);
                }
            }).onFailure(new Slot<Throwable>() {
                public void onEmit (Throwable cause) {
                    fail("Failed to subscribe to test object " + cause);
                }
            });

        // wait for the test to complete
        boolean completed = action.await();

        // finally shut everything down
        conmgr.disconnect();
        conmgr.shutdown();
        exec.shutdown();
        TestUtil.awaitTermination(exec);

        // now that we've cleaned everything up, we can freak out if necessary
        if (!completed) {
            fail("Timed out waiting for test to complete.");
        }
    }
}
