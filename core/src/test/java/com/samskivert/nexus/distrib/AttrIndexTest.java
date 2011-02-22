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
    @Test
    public void testAttrIndexAssignment ()
    {
        final TestObject tobj = new TestObject();
        final int omonkeys = tobj.monkeys.get();
        final int nmonkeys = 15;

        // manually initialize the object attributes since we're not registering with anything
        tobj.initAttributes();

        // make sure our attributes were wired up properly
        assertEquals(1, tobj.getAttributeCount());
        assertEquals(0, tobj.monkeys._index);
        assertEquals(tobj, tobj.monkeys._owner);

        // manually configure our event sink for same reason as above
        tobj._sink = new EventSink() {
            public void postEvent (NexusObject source, NexusEvent event) {
                assertTrue(event instanceof DValue.ChangedEvent<?>);
                DValue.ChangedEvent<?> ice = (DValue.ChangedEvent<?>)event;
                assertEquals(omonkeys, ice.getOldValue());
                assertEquals(nmonkeys, ice.getValue());

                // try applying the event to the object, make sure it works, and that our value
                // gets updated (which is a noop since it gets applied locally anyway)
                event.applyTo(source);
                assertEquals(nmonkeys, tobj.monkeys.get());
            }
        };

        // update the monkeys attribute, which will trigger a changed event (handled above)
        tobj.monkeys.update(nmonkeys);
    }
}
