//
// Nexus Chat Demo - demonstrates Nexus with some chattery
// http://github.com/threerings/nexus/blob/master/LICENSE

package nexus.chat.distrib;

import java.util.List;

import com.threerings.nexus.distrib.Address;
import com.threerings.nexus.distrib.DService;
import com.threerings.nexus.distrib.NexusObject;
import com.threerings.nexus.util.Callback;

/**
 * Creates {@link ChatService} marshaller instances.
 */
public class Factory_ChatService implements DService.Factory<ChatService>
{
    @Override
    public DService<ChatService> createService (NexusObject owner)
    {
        return new Marshaller(owner);
    }

    public static DService.Factory<ChatService> createDispatcher (final ChatService service)
    {
        return new DService.Factory<ChatService>() {
            public DService<ChatService> createService (NexusObject owner) {
                return new DService.Dispatcher<ChatService>(owner) {
                    @Override public ChatService get () {
                        return service;
                    }

                    @Override public Class<ChatService> getServiceClass () {
                        return ChatService.class;
                    }

                    @Override public void dispatchCall (short methodId, Object[] args) {
                        switch (methodId) {
                        case 1:
                            service.updateNick(
                                this.<String>cast(args[0]),
                                this.<Callback<Void>>cast(args[1]));
                            break;
                        case 2:
                            service.getRooms(
                                this.<Callback<List<String>>>cast(args[0]));
                            break;
                        case 3:
                            service.joinRoom(
                                this.<String>cast(args[0]),
                                this.<Callback<Address<RoomObject>>>cast(args[1]));
                            break;
                        case 4:
                            service.createRoom(
                                this.<String>cast(args[0]),
                                this.<Callback<Address<RoomObject>>>cast(args[1]));
                            break;
                        default:
                            super.dispatchCall(methodId, args);
                        }
                    }
                };
            }
        };
    }

    protected static class Marshaller extends DService<ChatService> implements ChatService
    {
        public Marshaller (NexusObject owner) {
            super(owner);
        }
        @Override public ChatService get () {
            return this;
        }
        @Override public Class<ChatService> getServiceClass () {
            return ChatService.class;
        }
        @Override public void updateNick (String nickname, Callback<Void> callback) {
            postCall((short)1, nickname, callback);
        }
        @Override public void getRooms (Callback<List<String>> callback) {
            postCall((short)2, callback);
        }
        @Override public void joinRoom (String name, Callback<Address<RoomObject>> callback) {
            postCall((short)3, name, callback);
        }
        @Override public void createRoom (String name, Callback<Address<RoomObject>> callback) {
            postCall((short)4, name, callback);
        }
    }
}
