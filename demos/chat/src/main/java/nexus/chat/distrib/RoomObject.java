//
// $Id$

package nexus.chat.distrib;

import com.threerings.nexus.distrib.DAttribute;
import com.threerings.nexus.distrib.DCustom;
import com.threerings.nexus.distrib.DService;
import com.threerings.nexus.distrib.Keyed;
import com.threerings.nexus.distrib.NexusObject;

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

    /** Used to emit and listen for chat events. */
    public static class ChatEventSlot extends DCustom<ChatEvent>
    {
        public void emit (String nickname, String message) {
            postEvent(new ChatEvent(_owner.getId(), _index, nickname, message));
        }
    }

    /** The name of this chat room (unique among names). */
    public final String name;

    /** Provides services for this room. */
    public final DService<RoomService> roomSvc;

    /** A slot by which chat events can be listened for, and (on the server), emitted. */
    public final ChatEventSlot chatEvent = new ChatEventSlot();

    // from interface Keyed
    public Comparable<?> getKey ()
    {
        return name;
    }

    public RoomObject (String name, DService<RoomService> roomSvc)
    {
        this.name = name;
        this.roomSvc = roomSvc;
    }

    @Override
    protected DAttribute getAttribute (int index)
    {
        switch (index) {
        case 0: return roomSvc;
        case 1: return chatEvent;
        default: throw new IndexOutOfBoundsException("Invalid attribute index " + index);
        }
    }

    @Override
    protected int getAttributeCount ()
    {
        return 2;
    }
}
