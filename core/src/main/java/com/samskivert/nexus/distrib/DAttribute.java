//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.distrib;

import com.samskivert.nexus.io.Streamable;

/**
 * The base type for all Nexus object attributes.
 */
public abstract class DAttribute
    implements Streamable, NexusObject.Attribute
{
    // from interface NexusObject.Attribute
    public void init (NexusObject owner, short index)
    {
        _owner = owner;
        _index = index;
    }

    /** The object that owns this attribute. */
    protected NexusObject _owner;

    /** The index of this attribute in its containing object. */
    protected short _index;
}
