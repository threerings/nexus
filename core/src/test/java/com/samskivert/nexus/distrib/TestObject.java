//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.distrib;

/**
 * An object with a bunch of attributes, for testing low-level Nexus bits.
 */
public class TestObject extends NexusObject
{
    /** The number of monkeys in our barrel. */
    public DIntValue monkeys = DIntValue.create(0);

    @Override
    public void readObject (Input in)
    {
        super.readObject(in);
        monkeys = in.<DIntValue>readStreamable();
    }

    @Override
    public void writeObject (Output out)
    {
        super.writeObject(out);
        out.writeStreamable(monkeys);
    }

    @Override
    protected DAttribute getAttribute (int index)
    {
        // TODO: optimize the generation of objects that extend NexusObject directly, to avoid
        // needless call to super.getAttributeCount()
        int pcount = super.getAttributeCount();
        if (index < pcount) {
            return super.getAttribute(index);
        }

        switch (index-pcount) {
        case 0: return monkeys;
        default: throw new IndexOutOfBoundsException("Invalid attribute index " + index);
        }
    }

    @Override
    protected int getAttributeCount ()
    {
        // TODO: optimize the generation of objects that extend NexusObject directly, to avoid
        // needless call to super.getAttributeCount()
        return super.getAttributeCount() + 1;
    }
}
