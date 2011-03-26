//
// $Id$

package nexus.chat.server;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.nexus.distrib.Address;
import com.samskivert.nexus.distrib.Nexus;
import com.samskivert.nexus.distrib.NexusException;
import com.samskivert.nexus.distrib.Singleton;
import com.samskivert.nexus.util.Callback;

import nexus.chat.distrib.ChatObject;
import nexus.chat.distrib.ChatService;
import nexus.chat.distrib.Factory_ChatService;
import nexus.chat.distrib.RoomObject;

/**
 * Manages the global chat services.
 */
public class ChatManager implements ChatService, Singleton
{
    public ChatManager (Nexus nexus)
    {
        _nexus = nexus;

        // register ourselves as a singleton
        nexus.registerSingleton(this);

        // create and register our chat object as a child singleton in our same context
        ChatObject chatobj = new ChatObject(Factory_ChatService.createDispatcher(this));
        nexus.registerSingleton(chatobj, this);

        // TEMP: create a chat room
        _rooms.put("Test", new RoomManager(_nexus, "Test"));
    }

    // from interface ChatService
    public void updateNick (String nickname, Callback<Void> callback)
    {
        callback.onFailure(new Exception("TODO"));
    }

    // from interface ChatService
    public void getRooms (Callback<List<String>> callback)
    {
        callback.onSuccess(Lists.newArrayList(_rooms.keySet()));
    }

    // from interface ChatService
    public void joinRoom (String name, Callback<Address<RoomObject>> callback)
    {
        RoomManager mgr = _rooms.get(name);
        if (mgr == null) {
            throw new NexusException("No room named '" + name + "'.");
        }

        // here we might check invitation lists or whatnot
        callback.onSuccess(Address.of(mgr.roomObj));
    }

    // from interface ChatService
    public void createRoom (String name, Callback<Address<RoomObject>> callback)
    {
        if (_rooms.containsKey(name)) {
            throw new NexusException("Room with name '" + name + "' already exists.");
        }

        // here we might check privileges or whether the room name contains swear words, etc.
        RoomManager mgr = new RoomManager(_nexus, name);
        _rooms.put(name, mgr);
        callback.onSuccess(Address.of(mgr.roomObj));
    }

    protected Nexus _nexus;
    protected Map<String, RoomManager> _rooms = Maps.newHashMap();
}
