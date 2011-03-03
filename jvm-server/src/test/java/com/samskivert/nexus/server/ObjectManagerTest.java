//
// $Id$

package com.samskivert.nexus.server;

import java.util.concurrent.Executor;

import com.samskivert.nexus.distrib.Action;
import com.samskivert.nexus.distrib.Keyed;
import com.samskivert.nexus.distrib.NexusException;
import com.samskivert.nexus.distrib.Singleton;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Unit tests for the {@link ObjectManager}.
 */
public class ObjectManagerTest
{
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
    public void testClearSingleton ()
    {
        ObjectManager omgr = createObjectManager();

        TestSingleton test = new TestSingleton();
        omgr.registerSingleton(test);
        final boolean[] invoked = new boolean[1];
        omgr.invoke(TestSingleton.class, new Action<TestSingleton>() {
            public void invoke (TestSingleton obj) {
                invoked[0] = true;
            }
        });
        assertTrue(invoked[0]);

        omgr.clearSingleton(test);
        try {
            omgr.invoke(TestSingleton.class, FAIL_SINGLE);
            fail();
        } catch (NexusException ne) {
            // expected
        }
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

    @Test
    public void testClearKeyed ()
    {
        ObjectManager omgr = createObjectManager();

        TestKeyed test = new TestKeyed(5);
        omgr.registerKeyed(test);
        final boolean[] invoked = new boolean[1];
        omgr.invoke(TestKeyed.class, test.getKey(), new Action<TestKeyed>() {
            public void invoke (TestKeyed obj) {
                invoked[0] = true;
            }
        });
        assertTrue(invoked[0]);

        omgr.clearKeyed(test);
        try {
            omgr.invoke(TestKeyed.class, test.getKey(), FAIL_KEYED);
            fail();
        } catch (NexusException ne) {
            // expected
        }
    }

    protected ObjectManager createObjectManager ()
    {
        return new ObjectManager(TestUtil.createTestConfig(), createDirectExec());
    }

    protected Executor createDirectExec () {
        return new Executor() {
            public void execute (Runnable op) {
                op.run();
            }
        };
    }
            
    protected static class TestSingleton implements Singleton
    {
        public int increment (int value) {
            return value+1;
        }
    }

    protected static final Action<TestSingleton> FAIL_SINGLE = new Action<TestSingleton>() {
        public void invoke (TestSingleton obj) {
            fail();
        }
    };

    protected static class TestKeyed implements Keyed
    {
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

    protected static final Action<TestKeyed> FAIL_KEYED = new Action<TestKeyed>() {
        public void invoke (TestKeyed obj) {
            fail();
        }
    };
}
