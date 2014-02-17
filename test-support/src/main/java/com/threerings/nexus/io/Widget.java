//
// Nexus Test Support - shared test infrastructure for Nexus framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.io;

import java.util.Arrays;
import java.util.List;

import com.threerings.nexus.io.Streamable;

/**
 * A test streamable.
 */
public class Widget implements Streamable
{
    public static final List<Widget> WS = Arrays.asList(
        new Widget(Color.RED, "foo", new Wangle(42)),
        new Widget(Color.GREEN, "bar", new Wangle(21)),
        new Widget(Color.BLUE, "baz", new Wangle(7)));

    public static class Wangle implements Streamable {
        public final int size;

        public Wangle (int size) {
            this.size = size;
        }

        @Override public boolean equals (Object other) {
            return (other instanceof Wangle) && size == ((Wangle)other).size;
        }

        @Override public int hashCode () {
            return size;
        }
    }

    public static enum Color { RED, GREEN, BLUE };

    public final Color color;
    public final String name;

    public Widget (Color color, String name, Wangle wangle) {
        this.color = color;
        this.name = name;
        _wangle = wangle;
    }

    @Override
    public boolean equals (Object other) {
        if (!(other instanceof Widget)) {
            return false;
        }
        Widget ow = (Widget)other;
        return name.equals(ow.name) && _wangle.equals(ow._wangle);
    }

    @Override public int hashCode () {
        return color.hashCode() ^ name.hashCode();
    }

    protected Wangle _wangle;
}
