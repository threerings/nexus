//
// $Id$

package nexus.chat.distrib;

import com.samskivert.nexus.distrib.DCustom;
import com.samskivert.nexus.distrib.Keyed;
import com.samskivert.nexus.distrib.NexusObject;

/**
 * Models an instance of a chat room.
 */
public class RoomObject extends NexusObject
    implements Keyed
{
    /** An event emitted when a chat message is sent. */
    public static class ChatEvent extends DCustom.Event
    {
        /** The nickname of the sender of this message. */
        public final String nickname;

        /** The text of the message. */
        public final String message;

        public ChatEvent (int targetId, short index, String nickname, String message) {
            super(targetId, index);
            this.nickname = nickname;
            this.message = message;
        }
    }

    public static class ChatEventSlot extends DCustom<ChatEvent>
    {
        public void emit (String nickname, String message) {
            postEvent(new ChatEvent(_owner.getId(), _index, nickname, message));
        }
    }

    /** The name of this chat room (unique among names). */
    public String name;

    /** Provides room services for this room. */
    public RoomService roomService;

    /** A slot by which chat events can be listened for, and (on the server), emitted. */
    public ChatEventSlot chatEvent = new ChatEventSlot();

    // from interface Keyed
    public Comparable<?> getKey ()
    {
        return name;
    }
}
