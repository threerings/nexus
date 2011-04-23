//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import org.junit.*;
import static org.junit.Assert.*;

import com.threerings.nexus.distrib.DAttribute.DListener;

/**
 * Test attribute machinery.
 */
public class DAttributeTest
{
    @Test
    public void testListenerUtils ()
    {
        DListener[] listeners = DAttribute.NO_LISTENERS;
        DListener one = new DListener() {};
        DListener two = new DListener() {};
        DListener three = new DListener() {};

        // add and remove a listener
        DListener[] l1 = DAttribute.addListener(listeners, one);
        assertEquals(1, countListeners(l1));
        assertTrue(haveListener(l1, one));

        DAttribute.removeListener(l1, one);
        assertEquals(0, countListeners(l1));
        assertFalse(haveListener(l1, one));

        DListener[] l1p = DAttribute.addListener(l1, one);
        assertEquals(1, countListeners(l1p));
        assertTrue(haveListener(l1p, one));

        // add multiple listeners, remove some, add again
        DListener[] l3 = DAttribute.addListener(
            DAttribute.addListener(
                DAttribute.addListener(listeners, one), two), three);
        assertEquals(3, countListeners(l3));
        assertTrue(haveListener(l3, one));
        assertTrue(haveListener(l3, two));
        assertTrue(haveListener(l3, three));

        DAttribute.removeListener(l3, two);
        assertEquals(2, countListeners(l3));
        assertTrue(haveListener(l3, one));
        assertFalse(haveListener(l3, two));
        assertTrue(haveListener(l3, three));

        DAttribute.removeListener(l3, one);
        assertEquals(1, countListeners(l3));
        assertFalse(haveListener(l3, one));
        assertFalse(haveListener(l3, two));
        assertTrue(haveListener(l3, three));

        DListener[] l3p = DAttribute.addListener(l3, two);
        assertEquals(2, countListeners(l3p));
        assertFalse(haveListener(l3p, one));
        assertTrue(haveListener(l3p, two));
        assertTrue(haveListener(l3p, three));
    }

    protected int countListeners (DListener[] ls)
    {
        int count = 0;
        for (int ii = 0; ii < ls.length; ii++) {
            if (ls[ii] != null) {
                count++;
            }
        }
        return count;
    }

    protected boolean haveListener (DListener[] ls, DListener l)
    {
        for (DListener ll : ls) {
            if (ll == l) {
                return true;
            }
        }
        return false;
    }
}
