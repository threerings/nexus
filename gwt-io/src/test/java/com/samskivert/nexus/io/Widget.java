//
// $Id$
//
// Nexus JVMIO - I/O and network services for Nexus built on java.nio
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.io;

import com.samskivert.nexus.io.Streamable;

/**
 * A test streamable.
 */
public class Widget implements Streamable
{
    public static class Wangle implements Streamable {
        public final int size;

        public Wangle (int size) {
            this.size = size;
        }

        public boolean equals (Object other) {
            return (other instanceof Wangle) && size == ((Wangle)other).size;
        }
    }

    public final String name;

    public Widget (String name, Wangle wangle)
    {
        this.name = name;
        _wangle = wangle;
    }

    @Override
    public boolean equals (Object other)
    {
        if (!(other instanceof Widget)) {
            return false;
        }
        Widget ow = (Widget)other;
        return name.equals(ow.name) && _wangle.equals(ow._wangle);
    }

    protected Wangle _wangle;
}
