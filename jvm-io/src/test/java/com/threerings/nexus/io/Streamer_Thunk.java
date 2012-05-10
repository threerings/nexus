//
// Nexus JVMIO - I/O and network services for Nexus built on java.nio
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.io;

/**
 * Handles the streaming of {@link Thunk} and/or nested classes.
 */
public class Streamer_Thunk
    implements Streamer<Thunk>
{
    @Override
    public Class<?> getObjectClass () {
        return Thunk.class;
    }

    @Override
    public void writeObject (Streamable.Output out, Thunk obj) {
        writeObjectImpl(out, obj);
    }

    @Override
    public Thunk readObject (Streamable.Input in) {
        return new Thunk(
            in.readInt()
        );
    }

    public static  void writeObjectImpl (Streamable.Output out, Thunk obj) {
        out.writeInt(obj.value);
    }
}
