//
// Nexus Test Support - shared test infrastructure for Nexus framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.io;

/**
 * Handles the streaming of {@link Widget} and/or nested classes.
 */
public class Streamer_Widget
    implements Streamer<Widget>
{
    /**
     * Handles the streaming of {@link Widget.Wangle} instances.
     */
    public static class Wangle
        implements Streamer<Widget.Wangle>
    {
        @Override
        public Class<?> getObjectClass () {
            return Widget.Wangle.class;
        }

        @Override
        public void writeObject (Streamable.Output out, Widget.Wangle obj) {
            writeObjectImpl(out, obj);
        }

        @Override
        public Widget.Wangle readObject (Streamable.Input in) {
            return new Widget.Wangle(
                in.readInt()
            );
        }

        public static  void writeObjectImpl (Streamable.Output out, Widget.Wangle obj) {
            out.writeInt(obj.size);
        }
    }

    @Override
    public Class<?> getObjectClass () {
        return Widget.class;
    }

    @Override
    public void writeObject (Streamable.Output out, Widget obj) {
        writeObjectImpl(out, obj);
    }

    @Override
    public Widget readObject (Streamable.Input in) {
        return new Widget(
            in.readString(),
            in.<Widget.Wangle>readValue()
        );
    }

    public static  void writeObjectImpl (Streamable.Output out, Widget obj) {
        out.writeString(obj.name);
        out.writeValue(obj._wangle);
    }
}
