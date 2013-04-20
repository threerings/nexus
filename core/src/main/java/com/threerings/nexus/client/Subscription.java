//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.client;

/**
 * Provides a handle on an object subscription that can be used to terminate it.
 */
public interface Subscription
{
    /** Terminates this object subscription. The object in question will no longer receive events
     * relating to distributed state change after the unsubscription is processed.
     *
     * <p>Any events in flight from the client to server or the server to client will be procesesd
     * before the subcription is finally terminated. Unsubscription is essentially another event in
     * the stream of events that are communicated between the client and server. Upon calling this
     * method, an unsubscribe event is sent to the server. No events from this client relating to
     * this objet will come after the unsubscribe event. Upon receipt of the unsubscribe event on
     * the server, an acknowledgement event is delivered back to the client. No events from the
     * server relating to this object will come after the acknowledgement event.</p>
     *
     * <p>If a subscription is canceled before the object in question has been successfully
     * delivered to the client, the client's subscription callback will never be notified.</p>
     */
    void unsubscribe ();
}
