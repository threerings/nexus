//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Tests the assignment of attribute indices for event dispatch.
 */
public class AttrIndexTest
{
    public class TestObject extends NexusObject
    {
        public DValue<Integer> monkeys = DValue.create(this, 0);
    }

    @Test
    public void testAttrIndexAssignment ()
    {
        TestObject tobj = new TestObject();
        // manually initialize the object attributes since we're not registering with anything
        tobj.init(0, null);

        // make sure our attributes were wired up properly
        assertEquals(0, tobj.monkeys._index);
        assertEquals(tobj, tobj.monkeys._owner);
    }
}
