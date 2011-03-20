//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.distrib;

/**
 * An object used for simple tests.
 */
public class TestObject extends NexusObject
    implements Singleton
{
    public DValue<String> value = DValue.create("test");

    @Override
    protected DAttribute getAttribute (int index)
    {
        switch (index) {
        case 0: return value;
        default: throw new IndexOutOfBoundsException("Invalid attribute index " + index);
        }
    }

    @Override
    protected int getAttributeCount ()
    {
        return 1;
    }
}
