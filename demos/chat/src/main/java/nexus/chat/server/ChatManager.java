//
// $Id$

package nexus.chat.server;

import java.util.List;

import com.samskivert.nexus.distrib.Address;
import com.samskivert.nexus.util.Callback;

import nexus.chat.distrib.ChatService;
import nexus.chat.distrib.RoomObject;

/**
 * Manages the global chat services.
 */
public class ChatManager implements ChatService
{
    // from interface ChatService
    public void updateNick (String nickname, Callback<Void> callback)
    {
        callback.onFailure(new Exception("TODO"));
    }

    // from interface ChatService
    public void getRooms (Callback<List<String>> callback)
    {
        callback.onFailure(new Exception("TODO"));
    }

    // from interface ChatService
    public void joinRoom (String name, Callback<Address<RoomObject>> callback)
    {
        callback.onFailure(new Exception("TODO"));
    }

    // from interface ChatService
    public void createRoom (String name, Callback<Address<RoomObject>> callback)
    {
        callback.onFailure(new Exception("TODO"));
    }
}
