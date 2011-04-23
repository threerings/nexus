//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import com.threerings.nexus.io.Streamable;

/**
 * A set attribute for a Nexus object. Contains an unordered set of distinct values.
 */
public class DSet<T> extends DAttribute // TODO: implements Set<T>
{
    @Override // from DAttribute
    public void readContents (Streamable.Input in)
    {
        // TODO: _impl = in.<Set<T>>readValue();
    }

    @Override // from DAttribute
    public void writeContents (Streamable.Output out)
    {
        // TODO: out.writeValue(_impl);
    }
}
