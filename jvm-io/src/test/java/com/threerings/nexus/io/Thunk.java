//
// Nexus JVMIO - I/O and network services for Nexus built on java.nio
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.io;

/**
 * A non-streamable class for which we manually provide a streamer.
 */
public class Thunk
{
    public final int value;

    public Thunk (int value) {
        this.value = value;
    }

    @Override public boolean equals (Object other) {
        return ((other instanceof Thunk) && ((Thunk)other).value == value);
    }

    @Override public int hashCode () {
        return value;
    }
}
