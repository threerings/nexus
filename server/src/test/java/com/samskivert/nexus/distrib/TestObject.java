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
    public final DValue<String> value = DValue.create("test");

    public final DService<TestService> testsvc;

    public TestObject (DService<TestService> testsvc)
    {
        this.testsvc = testsvc;
    }

    @Override
    protected DAttribute getAttribute (int index)
    {
        switch (index) {
        case 0: return value;
        case 1: return testsvc;
        default: throw new IndexOutOfBoundsException("Invalid attribute index " + index);
        }
    }

    @Override
    protected int getAttributeCount ()
    {
        return 2;
    }
}
