//
// Nexus Chat Demo - demonstrates Nexus with some chattery
// http://github.com/threerings/nexus/blob/master/LICENSE

package nexus.chat.distrib;

import com.threerings.nexus.io.Streamable;
import com.threerings.nexus.io.Streamer;

/**
 * Handles the streaming of {@link RoomObject} and/or nested classes.
 */
public class Streamer_RoomObject
    implements Streamer<RoomObject>
{
    /**
     * Handles the streaming of {@link RoomObject.ChatEvent} instances.
     */
    public static class ChatEvent
        implements Streamer<RoomObject.ChatEvent>
    {
        @Override
        public Class<?> getObjectClass () {
            return RoomObject.ChatEvent.class;
        }

        @Override
        public void writeObject (Streamable.Output out, RoomObject.ChatEvent obj) {
            writeObjectImpl(out, obj);
        }

        @Override
        public RoomObject.ChatEvent readObject (Streamable.Input in) {
            return new RoomObject.ChatEvent(
                in.readString(),
                in.readString()
            );
        }

        public static  void writeObjectImpl (Streamable.Output out, RoomObject.ChatEvent obj) {
            out.writeString(obj.nickname);
            out.writeString(obj.message);
        }
    }

    @Override
    public Class<?> getObjectClass () {
        return RoomObject.class;
    }

    @Override
    public void writeObject (Streamable.Output out, RoomObject obj) {
        writeObjectImpl(out, obj);
        obj.writeContents(out);
    }

    @Override
    public RoomObject readObject (Streamable.Input in) {
        RoomObject obj = new RoomObject(
            in.readString(),
            in.<RoomService>readService()
        );
        obj.readContents(in);
        return obj;
    }

    public static  void writeObjectImpl (Streamable.Output out, RoomObject obj) {
        out.writeString(obj.name);
        out.writeService(obj.roomSvc);
    }
}
