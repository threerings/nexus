//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import react.Signal;

import com.threerings.nexus.io.Streamable;

import static com.threerings.nexus.util.Log.log;

/**
 * The basis for all distributed information sharing in Nexus.
 */
public abstract class NexusObject
    implements Streamable, NexusService.ObjectResponse
{
    /**
     * A signal that is emitted if the subscription to this object is lost due to the object being
     * destroyed or due to the connection to the server that hosts this object being lost. The
     * exception delivered to the signal will be null if the object was destroyed or the cause of
     * the networking failure if an object is lost due to loss of network connection. This signal
     * will never be emitted on the hosting server, only on a subscribing client.
     */
    public final Signal<Throwable> onLost = Signal.create();

    /**
     * Returns this object's Nexus id. Only valid after the object has been registered with Nexus.
     */
    public int getId () {
        return _id;
    }

    /**
     * Returns the address of this object.
     */
    public Address<?> getAddress () {
        if (this instanceof Keyed) {
            @SuppressWarnings("unchecked") Class<DummyKeyed> clz = (Class<DummyKeyed>)getClass();
            return Address.create(_sink.getHost(), clz, ((Keyed)this).getKey());
        } else if (this instanceof Singleton) {
            @SuppressWarnings("unchecked") Class<DummySingle> clz = (Class<DummySingle>)getClass();
            return Address.create(_sink.getHost(), clz);
        } else {
            return Address.create(_sink.getHost(), getId());
        }
    }

    /**
     * Reads the contents of this object from the supplied input.
     */
    public void readContents (Streamable.Input in) {
        _id = in.readInt();
        for (int ii = 0, ll = getAttributeCount(); ii < ll; ii++) {
            getAttribute(ii).readContents(in);
        }
    }

    /**
     * Writes the contents of this object to the supplied output.
     */
    public void writeContents (Streamable.Output out) {
        out.writeInt(_id);
        for (int ii = 0, ll = getAttributeCount(); ii < ll; ii++) {
            getAttribute(ii).writeContents(out);
        }
    }

    @Override // from NexusService.ObjectResponse
    public NexusObject[] getObjects () {
        return new NexusObject[] { this };
    }

    /**
     * Initializes this object with its id and event sink, which also triggers the initialization
     * of its distributed attributes. This takes place when the object is registered with
     * dispatcher on its originating server, and when it is read off the network on a subscribing
     * client (though in this latter case the id is not changed).
     */
    protected void init (int id, EventSink sink) {
        _id = id;
        _sink = sink;
        for (int ii = 0, ll = getAttributeCount(); ii < ll; ii++) {
            getAttribute(ii).init(this, (short)ii);
        }
    }

    /**
     * Clears out this object's distributed id and event sink. Called by the object manager when
     * this object is unregistered.
     */
    protected void clear () {
        _id = 0;
        _sink = null;
    }

    /**
     * Returns the distributed attribute at the specified index.
     *
     * @exception IndexOutOfBoundsException if an attribute at illegal index is requested.
     */
    protected DAttribute getAttribute (int index) {
        throw new IndexOutOfBoundsException("Invalid attribute index " + index);
    }

    /**
     * Returns the number of attributes owned by this object. Values from 0 to {@link
     * #getAttributeCount}-1 may be legally passed to {@link #getAttribute}.
     */
    protected int getAttributeCount () {
        return 0;
    }

    /**
     * Requests that the supplied event be posted to this object.
     */
    protected void postEvent (NexusEvent event) {
        if (_id > 0) {
            assert(event.targetId == getId());
            _sink.postEvent(this, event);
        } else {
            log.warning("Requested to post event to unregistered object",
                        "event", event, new Exception());
        }
    }

    /**
     * Requests that a service call be posted to this object.
     */
    protected void postCall (short attrIndex, short methodId, Object[] args) {
        _sink.postCall(this, attrIndex, methodId, args);
    }

    /** Used by {@link #getAddress} for type jockeying. */
    private static class DummyKeyed extends NexusObject implements Keyed {
        public Comparable<?> getKey () { return null; }
    }
    /** Used by {@link #getAddress} for type jockeying. */
    private static class DummySingle extends NexusObject implements Singleton {}

    /** The unique identifier for this object. This value is not available until the object has
     * been registered with the Nexus Manager. The id is unique with respect to the peer on which
     * this object was created and registered. */
    protected int _id;

    /** Handles the dispatch of events on this object. */
    protected EventSink _sink;
}
