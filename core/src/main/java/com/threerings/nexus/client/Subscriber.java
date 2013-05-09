//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.client;

import com.threerings.nexus.distrib.Address;
import com.threerings.nexus.distrib.NexusObject;
import com.threerings.nexus.util.Callback;

/**
 * An object that will subscribe to an object of the specified address and pass the successfully
 * subscribed object through to the supplied callback. All failures will be propagated through to
 * the supplied callback as well.
 *
 * <p>This simplifies the handling of a common pattern, which is to make a service request, receive
 * an object address in response and immediately subscribe to the object in question. One can write
 * code like so:</p>
 *
 * <pre>{@code
 * // assume RoomService.joinRoom (String roomId, Callback<Address<RoomObject>> callback);
 * Subscriber<RoomObject> sub = Subscriber.create(new Callback<RoomObject>() {
 *   public void onSuccess (RoomObject obj) { ... }
 *   public void onFailure (Throwable cause) { ... }
 * });
 * obj.joinRoom(roomId, sub);
 * // then later when you're done with the room object
 * sub.unsubscribe();
 * }</pre>
 *
 * <p>Note that if you call {@link Subscriber#unsubscribe} before the initial service call has
 * returned the address to your object. It will simply not subscribe to the object in question. If
 * you call {@code unsubscribe} after the address has been received and the subscription has been
 * initiated, the normal "preempted subscription" process will take place and the NexusClient will
 * automatically unsubscribe once the subscription succeeds. </p>
 */
public abstract class Subscriber<T extends NexusObject> implements Callback<Address<T>>, Subscription
{
}
