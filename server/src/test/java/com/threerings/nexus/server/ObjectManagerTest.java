//
// $Id$
//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.server;

import java.util.concurrent.Executor;

import com.threerings.nexus.distrib.Action;
import com.threerings.nexus.distrib.DAttribute;
import com.threerings.nexus.distrib.DValue;
import com.threerings.nexus.distrib.Keyed;
import com.threerings.nexus.distrib.NexusException;
import com.threerings.nexus.distrib.NexusObject;
import com.threerings.nexus.distrib.Request;
import com.threerings.nexus.distrib.Singleton;

import com.threerings.nexus.distrib.TestObject;

import org.junit.*;
import static org.junit.Assert.*;

import static com.threerings.nexus.server.TestUtil.*;

/**
 * Unit tests for the {@link ObjectManager}.
 */
public class ObjectManagerTest
{
    @Test
    public void testRegister ()
    {
        ObjectManager omgr = createObjectManager();
        TestObject test = new TestObject(createTestServiceAttr());
        omgr.register(test);

        // ensure that we've been assigned an id
        assertTrue(test.getId() > 0);

        // ensure that event dispatch is wired up
        final String ovalue = test.value.get();
        final String nvalue = "newValue";
        final boolean[] triggered = new boolean[1];
        test.value.listen(new DValue.Listener<String>() {
            public void onChange (String value, String oldValue) {
                assertEquals(ovalue, oldValue);
                assertEquals(nvalue, value);
                triggered[0] = true;
            }
        });

        // update the value and make sure the listener was triggered (tests use the direct
        // executor, so it will process events directly inline with the update)
        test.value.update(nvalue);
        assertTrue(triggered[0]);

        // ensure that after clear, we no longer have an id
        omgr.clear(test);
        assertFalse(test.getId() > 0);
    }

    @Test
    public void testSingleton ()
    {
        ObjectManager omgr = createObjectManager();
        TestSingleton test = new TestSingleton();
        omgr.registerSingleton(test);

        // ensure that actions are dispatched on our registered entity
        final boolean[] invoked = new boolean[1];
        omgr.invoke(TestSingleton.class, new Action<TestSingleton>() {
            public void invoke (TestSingleton obj) {
                invoked[0] = true;
            }
        });
        assertTrue(invoked[0]);

        // ensure that requests are dispatched on our registered entity
        int result = omgr.invoke(TestSingleton.class, new Request<TestSingleton,Integer>() {
            public Integer invoke (TestSingleton obj) {
                return obj.increment(0);
            }
        });
        assertEquals(1, result);

        // ensure that actions are not dispatched once the entity is cleared
        omgr.clearSingleton(test);
        try {
            omgr.invoke(TestSingleton.class, FAIL_SINGLE);
            fail();
        } catch (NexusException ne) {
            // expected
        }
    }

    @Test
    public void testMissingSingletonInvoke ()
    {
        ObjectManager omgr = createObjectManager();
        try {
            omgr.invoke(TestSingleton.class, FAIL_SINGLE);
            fail();
        } catch (NexusException ne) {
            // expected
        }
    }

    @Test
    public void testRegisterAndClearKeyed ()
    {
        ObjectManager omgr = createObjectManager();
        TestKeyed test = new TestKeyed(5);
        omgr.registerKeyed(test);

        // ensure that we report that we currently host this entity
        assertTrue(omgr.hostsKeyed(TestKeyed.class, test.getKey()));

        // ensure that actions are dispatched on our registered entity
        final boolean[] invoked = new boolean[1];
        omgr.invoke(TestKeyed.class, test.getKey(), new Action<TestKeyed>() {
            public void invoke (TestKeyed obj) {
                invoked[0] = true;
            }
        });
        assertTrue(invoked[0]);

        // ensure that requests are dispatched on our registered entity
        int result = omgr.invoke(TestKeyed.class, test.getKey(), new Request<TestKeyed,Integer>() {
            public Integer invoke (TestKeyed obj) {
                return obj.decrement(0);
            }
        });
        assertEquals(-1, result);

        // ensure that actions are not dispatched once the entity is cleared
        omgr.clearKeyed(test);
        try {
            omgr.invoke(TestKeyed.class, test.getKey(), FAIL_KEYED);
            fail();
        } catch (NexusException ne) {
            // expected
        }

        // ensure that we no longer report that we currently host this entity
        assertFalse(omgr.hostsKeyed(TestKeyed.class, test.getKey()));
    }

    @Test
    public void testMissingKeyedInvoke ()
    {
        ObjectManager omgr = createObjectManager();

        // test when there are no registrations at all
        try {
            omgr.invoke(TestKeyed.class, 3, FAIL_KEYED);
            fail();
        } catch (NexusException ne) {
            // expected
        }

        // now test just a key mismatch
        omgr.registerKeyed(new TestKeyed(1));
        try {
            omgr.invoke(TestKeyed.class, 3, FAIL_KEYED);
            fail();
        } catch (NexusException ne) {
            // expected
        }
    }

    protected ObjectManager createObjectManager ()
    {
        return new ObjectManager(createTestConfig(), createDirectExec());
    }

    protected Executor createDirectExec () {
        return new Executor() {
            public void execute (Runnable op) {
                op.run();
            }
        };
    }

    protected static final Action<TestSingleton> FAIL_SINGLE = new Action<TestSingleton>() {
        public void invoke (TestSingleton obj) {
            fail();
        }
    };

    protected static final Action<TestKeyed> FAIL_KEYED = new Action<TestKeyed>() {
        public void invoke (TestKeyed obj) {
            fail();
        }
    };
}
