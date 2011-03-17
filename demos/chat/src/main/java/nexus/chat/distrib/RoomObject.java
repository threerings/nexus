//
// $Id$

package nexus.chat.distrib;

import com.samskivert.nexus.distrib.Keyed;
import com.samskivert.nexus.distrib.NexusObject;

/**
 * Models an instance of a chat room.
 */
public class RoomObject extends NexusObject
    implements Keyed
{
    /** The name of this chat room (unique among names). */
    public String name;

    /** Provides room services for this room. */
    public RoomService roomService;

    // from interface Keyed
    public Comparable<?> getKey ()
    {
        return name;
    }
}
