//
// Nexus Chat Demo - demonstrates Nexus with some chattery
// http://github.com/threerings/nexus/blob/master/LICENSE

package nexus.chat.distrib;

import com.threerings.nexus.distrib.DAttribute;
import com.threerings.nexus.distrib.DService;
import com.threerings.nexus.distrib.DSignal;
import com.threerings.nexus.distrib.Keyed;
import com.threerings.nexus.distrib.NexusObject;
import com.threerings.nexus.io.Streamable;

/**
 * Models an instance of a chat room.
 */
public class RoomObject extends NexusObject
    implements Keyed
{
    /** An event emitted when a chat message is sent. */
    public static class ChatEvent implements Streamable {
        /** The nickname of the sender of this message. */
        public final String nickname;

        /** The text of the message. */
        public final String message;

        public ChatEvent (String nickname, String message) {
            this.nickname = nickname;
            this.message = message;
        }
    }

    /** The name of this chat room (unique among names). */
    public final String name;

    /** Provides services for this room. */
    public final DService<RoomService> roomSvc;

    /** A slot by which chat events can be listened for, and (on the server), emitted. */
    public final DSignal<ChatEvent> chatEvent = DSignal.create(this);

    // from interface Keyed
    public Comparable<?> getKey () {
        return name;
    }

    public RoomObject (String name, DService.Factory<RoomService> roomSvc) {
        this.name = name;
        this.roomSvc = roomSvc.createService(this);
    }
}
