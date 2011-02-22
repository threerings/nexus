//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.distrib;

/**
 * A map attribute for a Nexus object. Contains a mapping from keys to values.
 */
public class DMap<K,V> extends DAttribute // TODO: implements Map<K,V>
{
    // from interface Streamable
    public void readObject (Input in)
    {
        // TODO
    }

    // from interface Streamable
    public void writeObject (Output out)
    {
        // TODO
    }
}
