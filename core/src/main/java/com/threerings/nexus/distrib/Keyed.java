//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

/**
 * An interface that must be implemented by keyed entities. Keyed entities are registered in tables
 * shared across all server nodes in a Nexus network, and can be addressed from any server node in
 * the network.
 */
public interface Keyed
{
    /** Returns the globally unique key for this instance (must be a primitive or String). */
    Comparable<?> getKey ();
}
