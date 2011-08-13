//
// $Id$

package nexus.chat.distrib;

import com.threerings.nexus.io.Streamable;
import com.threerings.nexus.io.Streamer;

/**
 * Streams {@link RoomObject} and its internal classes.
 */
public class Streamer_RoomObject implements Streamer<RoomObject>
{
    public Class<?> getObjectClass ()
    {
        return RoomObject.class;
    }

    public void writeObject (Streamable.Output out, RoomObject obj)
    {
        out.writeString(obj.name);
        out.writeService(RoomService.class);
        obj.writeContents(out);
    }

    public RoomObject readObject (Streamable.Input in)
    {
        RoomObject obj = new RoomObject(in.readString(), in.<RoomService>readService());
        obj.readContents(in);
        return obj;
    }

    /** Handles the streaming of {@link RoomObject.ChatEvent} instances. */
    public static class ChatEvent implements Streamer<RoomObject.ChatEvent> {
        public Class<?> getObjectClass () {
            return RoomObject.ChatEvent.class;
        }
        public void writeObject (Streamable.Output out, RoomObject.ChatEvent obj) {
            out.writeString(obj.nickname);
            out.writeString(obj.message);
        }
        public RoomObject.ChatEvent readObject (Streamable.Input in) {
            return new RoomObject.ChatEvent(in.readString(), in.readString());
        }
    }
}
