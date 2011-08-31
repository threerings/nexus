//
// Nexus Chat Demo - demonstrates Nexus with some chattery
// http://github.com/threerings/nexus/blob/master/LICENSE

package nexus.chat.distrib;

import com.threerings.nexus.distrib.NexusService;
import com.threerings.nexus.util.Callback;

/**
 * Defines distributed services available in a room.
 */
public interface RoomService extends NexusService
{
    /**
     * Requests that the supplied chat message be sent to the room.
     */
    void sendMessage (String message, Callback<Void> callback); // TODO: mode (emote, shout, etc.)?
}
