//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.server;

import java.util.concurrent.Executor;

import com.threerings.nexus.distrib.Action;
import com.threerings.nexus.distrib.DValue;
import com.threerings.nexus.distrib.Nexus;
import com.threerings.nexus.distrib.NexusException;
import com.threerings.nexus.distrib.Request;
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
    public void testRegister () {
        ObjectManager omgr = createObjectManager();
        TestObject test = new TestObject(createTestServiceAttr());
        omgr.register(test);

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

        // update the value and make sure the listener was triggered (tests use the direct
        // executor, so it will process events directly inline with the update)
        test.value.update(nvalue);
        assertTrue(triggered[0]);

        // ensure that after clear, we no longer have an id
        omgr.clear(test);
        assertFalse(test.getId() > 0);
    }

    @Test
    public void testSingleton () {
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
    public void testMissingSingletonInvoke () {
        ObjectManager omgr = createObjectManager();
        try {
            omgr.invoke(TestSingleton.class, FAIL_SINGLE);
            fail();
        } catch (NexusException ne) {
            // expected
        }
    }

    @Test
    public void testRegisterAndClearKeyed () {
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
        omgr.invoke(TestKeyed.class, test.getKey(), MISSING_KEYED);

        // ensure that we no longer report that we currently host this entity
        assertFalse(omgr.hostsKeyed(TestKeyed.class, test.getKey()));
    }

    @Test
    public void testMissingKeyedInvoke () {
        ObjectManager omgr = createObjectManager();

        // test when there are no registrations at all
        omgr.invoke(TestKeyed.class, 3, MISSING_KEYED);

        // now test just a key mismatch
        omgr.registerKeyed(new TestKeyed(1));
        omgr.invoke(TestKeyed.class, 3, MISSING_KEYED);
    }

    @Test
    public void testKeyedSubclass () {
        ObjectManager omgr = createObjectManager();
        omgr.registerKeyed(new ChildKeyed(1));
        final boolean[] invoked = new boolean[1];
        omgr.invoke(TestKeyed.class, 1, new Action<TestKeyed>() {
            public void invoke (TestKeyed obj) {
                assertTrue(obj instanceof ChildKeyed);
                invoked[0] = true;
            }
        });
        assertTrue(invoked[0]);
    }

    @Test
    public void testSingletonSubclass () {
        ObjectManager omgr = createObjectManager();
        omgr.registerSingleton(new ChildSingleton());
        final boolean[] invoked = new boolean[1];
        omgr.invoke(TestSingleton.class, new Action<TestSingleton>() {
            public void invoke (TestSingleton obj) {
                assertTrue(obj instanceof ChildSingleton);
                invoked[0] = true;
            }
        });
        assertTrue(invoked[0]);
    }

    @Test
    public void testRequireContext () {
        final ObjectManager omgr = createObjectManager();
        omgr.registerSingleton(new TestSingleton());
        omgr.registerKeyed(new TestKeyed(5));
        final int[] checks = new int[] { 0 };

        omgr.invoke(TestSingleton.class, new Action<TestSingleton>() {
            public void invoke (TestSingleton obj) {
                omgr.assertContext(TestSingleton.class);
                checks[0]++;
                try {
                    omgr.assertContext(TestKeyed.class, 5);
                } catch (AssertionError ae) {
                    checks[0]++;
                }
            }
        });
        assertEquals(2, checks[0]);

        omgr.invoke(TestKeyed.class, 5, new Action<TestKeyed>() {
            public void invoke (TestKeyed obj) {
                omgr.assertContext(TestKeyed.class, 5);
                checks[0]++;
                try {
                    omgr.assertContext(TestSingleton.class);
                } catch (AssertionError ae) {
                    checks[0]++;
                }
            }
        });
        assertEquals(4, checks[0]);
    }

    protected ObjectManager createObjectManager () {
        return new ObjectManager(createTestConfig(), null, createDirectExec());
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

    protected static final Action<TestKeyed> MISSING_KEYED = new Action<TestKeyed>() {
        public void invoke (TestKeyed obj) {
            fail();
        }
        public void onDropped (Nexus nexus, Class<?> eclass, Comparable<?> key) {
            // expected
        }
    };
}
