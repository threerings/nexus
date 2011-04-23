//
// $Id$

package nexus.chat.distrib;

import com.threerings.nexus.distrib.DService;
import com.threerings.nexus.io.ServiceFactory;
import com.threerings.nexus.util.Callback;

/**
 * Creates {@link RoomService} marshaller instances.
 */
public class Factory_RoomService implements ServiceFactory<RoomService>
{
    // from interface ServiceFactory<RoomService>
    public DService<RoomService> createService ()
    {
        return new Marshaller();
    }

    public static DService<RoomService> createDispatcher (final RoomService service)
    {
        return new DService.Dispatcher<RoomService>() {
            @Override public RoomService get () {
                return service;
            }

            @Override public void dispatchCall (short methodId, Object[] args) {
                switch (methodId) {
                case 1:
                    service.sendMessage(
                        (String)args[0],
                        this.<Callback<Void>>cast(args[1]));
                    break;
                default:
                    super.dispatchCall(methodId, args);
                }
            }
        };
    }

    protected static class Marshaller extends DService<RoomService> implements RoomService
    {
        @Override public RoomService get () {
            return this;
        }
        @Override public void sendMessage (String message, Callback<Void> callback) {
            postCall((short)1, message, callback);
        }
    }
}
