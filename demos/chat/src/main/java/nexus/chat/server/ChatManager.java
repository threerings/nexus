//
// Nexus Chat Demo - demonstrates Nexus with some chattery
// http://github.com/threerings/nexus/blob/master/LICENSE

package nexus.chat.server;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import react.UnitSlot;

import com.threerings.nexus.distrib.Address;
import com.threerings.nexus.distrib.Nexus;
import com.threerings.nexus.distrib.Singleton;
import com.threerings.nexus.server.Session;
import com.threerings.nexus.server.SessionLocal;
import com.threerings.nexus.util.Callback;

import nexus.chat.distrib.ChatObject;
import nexus.chat.distrib.ChatService;
import nexus.chat.distrib.Factory_ChatService;
import nexus.chat.distrib.RoomObject;

import static com.threerings.nexus.distrib.NexusException.require;
import static com.threerings.nexus.util.Log.log;

/**
 * Manages the global chat services.
 */
public class ChatManager implements ChatService, Singleton
{
    public ChatManager (Nexus nexus) {
        _nexus = nexus;

        // register ourselves as a singleton
        nexus.registerSingleton(this);

        // create and register our chat object as a child singleton in our same context
        ChatObject chatobj = new ChatObject(Factory_ChatService.createDispatcher(this));
        nexus.registerSingleton(chatobj, this);
    }

    // from interface ChatService
    public void updateNick (String nickname, Callback<Void> callback) {
        require(!_activeNicks.contains(nickname), "The nickname '" + nickname + "' is in use.");
        require(!nickname.startsWith("{") && !nickname.toLowerCase().contains("anonymous"),
                "Invalid nickname."); // no spoofing

        getChatter().updateNick(nickname);
        _activeNicks.add(nickname);
        callback.onSuccess(null);
    }

    // from interface ChatService
    public void getRooms (Callback<List<String>> callback) {
        callback.onSuccess(Lists.newArrayList(_rooms.keySet()));
    }

    // from interface ChatService
    public void joinRoom (String name, Callback<Address<RoomObject>> callback) {
        Address<RoomObject> addr = _rooms.get(name);
        require(addr != null, "No room named '" + name + "'.");
        // here we might check invitation lists or whatnot

        // note that this chatter has entered this room
        getChatter().enterRoom(name);
        callback.onSuccess(addr);
    }

    // from interface ChatService
    public void createRoom (String name, Callback<Address<RoomObject>> callback) {
        require(!_rooms.containsKey(name), "Room already exists.");
        // here we might check privileges or whether the room name contains swear words, etc.

        // create a manager for the new room
        RoomManager mgr = new RoomManager(_nexus, name);
        Address<RoomObject> addr = Address.of(mgr.roomObj);
        _rooms.put(name, addr);

        // note that this chatter has entered this room
        getChatter().enterRoom(name);
        callback.onSuccess(addr);
    }

    protected Chatter getChatter () {
        Chatter chatter = SessionLocal.get(Chatter.class);
        if (chatter == null) {
            log.info("New chatter " + SessionLocal.getSession().getIPAddress());
            String nickname = "{anonymous@" + SessionLocal.getSession().getIPAddress() + "}";
            SessionLocal.set(Chatter.class, chatter = new Chatter(_nexus, nickname));

            // register a listener on this chatter's session to learn when they go away
            SessionLocal.getSession().onDisconnect().connect(new UnitSlot() {
                @Override public void onEmit () {
                    Chatter chatter = SessionLocal.get(Chatter.class);
                    _activeNicks.remove(chatter.nickname); // clear out this chatter's nickname
                    chatter.leaveRoom(); // leave any occupied room
                }
            });
        }
        return chatter;
    }

    protected Nexus _nexus;
    protected Map<String, Address<RoomObject>> _rooms = Maps.newHashMap();
    protected Set<String> _activeNicks = Sets.newHashSet();
}
