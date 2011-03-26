//
// $Id$

package nexus.chat.server;

import com.samskivert.nexus.distrib.Keyed;
import com.samskivert.nexus.distrib.Nexus;
import com.samskivert.nexus.distrib.NexusException;
import com.samskivert.nexus.server.SessionLocal;
import com.samskivert.nexus.util.Callback;

import nexus.chat.distrib.Factory_RoomService;
import nexus.chat.distrib.RoomObject;
import nexus.chat.distrib.RoomService;

/**
 * Manages a single chat room.
 */
public class RoomManager implements RoomService, Keyed
{
    /** The room object we manage. */
    public final RoomObject roomObj;

    /**
     * Creates a room manager for the chat room with the specified name.
     */
    public RoomManager (Nexus nexus, String name)
    {
        // create our room object before we register ourselves as it will hold our key
        roomObj = new RoomObject(name, Factory_RoomService.createDispatcher(this));

        // register ourselves as a keyed entity
        nexus.registerKeyed(this);

        // create and register our RoomObject as a keyed child in our same context
        nexus.registerKeyed(roomObj, this);
    }

    public void chatterEntered (String nickname)
    {
        roomObj.chatEvent.emit(null, nickname + " entered.");
    }

    public void chatterLeft (String nickname)
    {
        roomObj.chatEvent.emit(null, nickname + " left.");
    }

    public void chatterChangedNick (String oldname, String newname)
    {
        roomObj.chatEvent.emit(null, oldname + " is now known as <" + newname + ">.");
    }

    // from interface RoomService
    public void sendMessage (String message, Callback<Void> callback)
    {
        // here we might do things like access control, etc.

        // send the chat event to all subscribers to the room
        roomObj.chatEvent.emit(SessionLocal.get(Chatter.class).nickname, message);

        // tell the caller their chat message was sent
        callback.onSuccess(null);
    }

    // from interface Keyed
    public Comparable<?> getKey ()
    {
        return roomObj.name;
    }
}
