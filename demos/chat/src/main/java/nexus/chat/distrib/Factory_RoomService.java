//
// Nexus Chat Demo - demonstrates Nexus with some chattery
// http://github.com/threerings/nexus/blob/master/LICENSE

package nexus.chat.distrib;

import com.threerings.nexus.distrib.DService;
import com.threerings.nexus.distrib.NexusObject;
import com.threerings.nexus.util.Callback;

/**
 * Creates {@link RoomService} marshaller instances.
 */
public class Factory_RoomService implements DService.Factory<RoomService>
{
    @Override
    public DService<RoomService> createService (NexusObject owner)
    {
        return new Marshaller(owner);
    }

    public static DService.Factory<RoomService> createDispatcher (final RoomService service)
    {
        return new DService.Factory<RoomService>() {
            public DService<RoomService> createService (NexusObject owner) {
                return new DService.Dispatcher<RoomService>(owner) {
                    @Override public RoomService get () {
                        return service;
                    }

                    @Override public Class<RoomService> getServiceClass () {
                        return RoomService.class;
                    }

                    @Override public void dispatchCall (short methodId, Object[] args) {
                        switch (methodId) {
                        case 1:
                            service.sendMessage(
                                this.<String>cast(args[0]),
                                this.<Callback<Void>>cast(args[1]));
                            break;
                        default:
                            super.dispatchCall(methodId, args);
                        }
                    }
                };
            }
        };
    }

    protected static class Marshaller extends DService<RoomService> implements RoomService
    {
        public Marshaller (NexusObject owner) {
            super(owner);
        }
        @Override public RoomService get () {
            return this;
        }
        @Override public Class<RoomService> getServiceClass () {
            return RoomService.class;
        }
        @Override public void sendMessage (String message, Callback<Void> callback) {
            postCall((short)1, message, callback);
        }
    }
}
