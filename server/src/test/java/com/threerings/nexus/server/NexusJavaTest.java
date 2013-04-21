//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.server;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.*;
import static org.junit.Assert.*;

import com.threerings.nexus.distrib.Action;
import com.threerings.nexus.distrib.NexusException;
import com.threerings.nexus.distrib.Singleton;

public class NexusJavaTest
{
    @Before public void createServer () {
        System.setProperty("nexus.safety_checks", "true");
        _exec = Executors.newFixedThreadPool(3);
        Properties props = new Properties();
        props.setProperty("nexus.node", "test");
        props.setProperty("nexus.hostname", "localhost");
        props.setProperty("nexus.rpc_timeout", "1000");
        _server = new NexusServer(new NexusConfig(props), _exec);
    }

    @After public void shutdownServer () throws Exception {
        _server.shutdown();
        _exec.shutdown();
        if (!_exec.awaitTermination(2, TimeUnit.SECONDS)) {
            fail("Executor failed to terminate after 2 seconds.");
        }
        System.setProperty("nexus.safety_checks", "");
    }

    @Test public void testRuntimeChecksOnAction () {
        final boolean[] printed = new boolean[] { false };

        class EntityA implements Singleton {
            public int incr (int value) { return value + 1; }
        }

        class EntityB implements Singleton {
            public void incrAndDoubleAndPrint (final int value) {
                _server.invoke(EntityA.class, new Action<EntityA>() {
                    public void invoke (EntityA a) {
                        int ivalue = a.incr(value);
                        try {
                            // naughty naughty, we're using an outer-this pointer to jump contexts;
                            // this will fail because our runtime safety checks null out our
                            // outer-this pointers
                            print(ivalue*2);
                        } catch (NullPointerException npe) {
                            // this NPE is expected, but we can't set a flag to indicate that we
                            // got here, because that would require an outer this pointer and those
                            // have been nulled out
                        }
                    }
                });
            }

            protected void print (int value) {
                printed[0] = true;
                System.err.println(value);
            }
        }

        _server.registerSingleton(new EntityA());
        _server.registerSingleton(new EntityB());
        _server.invoke(EntityB.class, new Action<EntityB>() {
            public void invoke (EntityB b) {
                b.incrAndDoubleAndPrint(5);
            }
        });

        delay(500); // give things time to fail
        assertFalse(printed[0]); // ensure that the print call did not execute
    }

    protected void delay (long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException ie) {}
    }

    protected ExecutorService _exec;
    protected NexusServer _server;
}
