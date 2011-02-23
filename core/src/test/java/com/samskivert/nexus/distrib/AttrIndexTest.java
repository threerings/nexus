//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.distrib;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Tests the assignment of attribute indices for event dispatch.
 */
public class AttrIndexTest
{
    public class TestObject extends NexusObject
    {
        public DValue<Integer> monkeys = DValue.create(0);

        @Override protected DAttribute getAttribute (int index) {
            switch (index) {
            case 0: return monkeys;
            default: throw new IndexOutOfBoundsException("Invalid attribute index " + index);
            }
        }

        @Override protected int getAttributeCount () {
            return 1;
        }
    }

    @Test
    public void testAttrIndexAssignment ()
    {
        TestObject tobj = new TestObject();

        // manually initialize the object attributes since we're not registering with anything
        tobj.initAttributes();

        // make sure our attributes were wired up properly
        assertEquals(1, tobj.getAttributeCount());
        assertEquals(0, tobj.monkeys._index);
        assertEquals(tobj, tobj.monkeys._owner);
    }
}
