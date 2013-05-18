//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.client;

import react.Function;
import react.RFuture;

import com.threerings.nexus.distrib.Address;
import com.threerings.nexus.distrib.NexusObject;

/**
 * An object that will subscribe to a single object and provide you a handle by which you can
 * unsubscribe from the object when you are done with it. This structure makes the API for
 * subscribing to objects much simpler and allows it to follow the standard pattern of returning a
 * {@link RFuture} via which the caller can react to success or failure.
 *
 * <p>There are two common patterns for subscribing to objects. One is when you want to subscribe
 * to singleton object for which you already have the address:</p>
 *
 * <pre>{@code
 * Subscriber<FooObject> sub = client.subscriber();
 * sub.subscribe(Address.create(host, FooObject.class)).
 *   onSuccess(new Slot<FooObject>() { ... }).
 *   onFailure(new Slot<Throwable>() { ... });
 * // call sub.unsubscribe() when you're done with the object
 * }</pre>
 *
 * <p>The other pattern is when you receive the address of an object as the result of a distributed
 * service call:</p>
 *
 * <pre>{@code
 * Subscriber<FooObject> sub = client.subscriber();
 * obj.svc.get().getFoo().flatMap(sub).
 *   onSuccess(new Slot<FooObject>() { ... }).
 *   onFailure(new Slot<Throwable>() { ... });
 * }</pre>
 *
 * <p>In this latter pattern, if either the service call fails or the subsequent subscription
 * request fail, the onFailure slot will be notified.</p>
 *
 * <p>Note that you can {@link Subscriber#unsubscribe} at any point during this asynchronous
 * process and the object subscription will be successfully canceled. This may mean that the client
 * waits for the subscription to complete and then terminates it, but you don't have to wait around
 * yourself, nor does such an action result in a leaked subscription.</p>
 */
public interface Subscriber<T extends NexusObject> extends Function<Address<T>,RFuture<T>>
{
    /** Initiates a subscription to the object with the specified address. */
    RFuture<T> subscribe (Address<T> addr);

    /** Terminates this object subscription. The object in question will no longer receive events
     * relating to distributed state change after the unsubscription is processed.
     *
     * <p>Any events in flight from the client to server or the server to client will be procesesd
     * before the subcription is finally terminated. Unsubscription is essentially another event in
     * the stream of events that are communicated between the client and server. Upon calling this
     * method, an unsubscribe event is sent to the server. No events from this client relating to
     * this object will come after the unsubscribe event. Upon receipt of the unsubscribe event on
     * the server, an acknowledgement event is delivered back to the client. No events from the
     * server relating to this object will come after the acknowledgement event.</p>
     *
     * <p>If a subscription is canceled before the object in question has been successfully
     * delivered to the client, the object will not be delivered to the future's listeners.</p>
     */
    void unsubscribe ();
}
