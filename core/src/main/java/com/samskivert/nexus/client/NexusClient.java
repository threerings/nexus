//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.client;

import java.util.HashMap;
import java.util.Map;

import com.samskivert.nexus.distrib.Address;
import com.samskivert.nexus.distrib.NexusObject;
import com.samskivert.nexus.net.Connection;
import com.samskivert.nexus.util.Callback;

/**
 * Manages connections to Nexus servers. Provides access to distributed objects and services.
 */
public class NexusClient
{
    /**
     * Requests that the client connects to the specified server. The callback will be notified
     * asynchronously on connection success or failure.
     */
    public void connect (String serverHost, Callback<Void> callback)
    {
        callback.onFailure(new Throwable("TODO: implement connect"));
    }

    /**
     * Requests to subscribe to the object identified by the supplied address.
     */
    public <T extends NexusObject> void subscribe (Address<T> addr, Callback<T> callback)
    {
        callback.onFailure(new Throwable("TODO: implement subscribe"));
    }

    /**
     * Creates a callback that will subscribe to an object of the specified address and pass the
     * successfully subscribed object through to the supplied callback. All failures will be
     * propagated through to the supplied callback as well. This simplifies the handling of a
     * common pattern, which is to make a service request, receive an object address in response
     * and immediately subscribe to the object in question. One can write code like so:
     * <pre>{@code
     * // assume RoomService.joinRoom (String roomId, Callback<Address<RoomObject>> callback);
     * obj.joinRoom(roomId, client.subscriber(new Callback<RoomObject>() {
     *     public void onSuccess (RoomObject obj) { ... }
     *     public void onFailure (Throwable cause) { ... }
     * }));
     * }</pre>
     */
    public <T extends NexusObject> Callback<Address<T>> subscriber (final Callback<T> callback)
    {
        return new Callback<Address<T>>() {
            public void onSuccess (Address<T> address) {
                subscribe(address, callback);
            }
            public void onFailure (Throwable cause) {
                callback.onFailure(cause);
            }
        };
    }

    /** A mapping from hostname to connection instance for all active connections. */
    protected Map<String, Connection> _connections = new HashMap<String, Connection>();
}
