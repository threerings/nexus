//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.distrib;

import com.samskivert.nexus.io.Streamable;

import static com.samskivert.nexus.util.Log.log;

/**
 * An attribute that has no associated data, but simply provides a means by which to emit, and
 * listen for, custom events. One uses it like so:
 * <pre>{@code
 * class MyObject extends NexusObject {
 *   public static class MyEvent extends DCustom.Event {
 *     public final String data;
 *     public MyEvent (int targetId, short index, String data) {
 *       super(targetId, index);
 *       this.data = data;
 *     }
 *   }
 *   public static class MyEventSlot extends DCustom<MyEvent> {
 *     public void emit (String data) {
 *       postEvent(new MyEvent(_owner.getId(), _index, data));
 *     }
 *   }
 *   public MyEventSlot myEvent = new MyEventSlot();
 * }
 * MyObject obj = ...;
 * obj.myEvent.addListener(new MyEventSlot.Listener<MyEvent>() {
 *   public void onEvent (MyEvent event) { ... }
 * });
 * </pre>
 * Note that {@code DCustom.One} (or {@code Two} or {@code Three}) must be used so that the {@code
 * emit} method is visible to the caller. One could also make their own custom subtype of {@code
 * DCustom} with 
 */
public abstract class DCustom<T extends DCustom.Event> extends DAttribute
{
    /** An interface for publishing event notifications to listeners. */
    public static interface Listener<T extends DCustom.Event> extends DListener
    {
        /** Notifies listener of an event arrival. */
        void onEvent (T event);
    }

    /** Custom events must extend this class. */
    public static abstract class Event extends DAttribute.Event
    {
        @Override public void applyTo (NexusObject target) {
            @SuppressWarnings("unchecked") DCustom<Event> attr =
                (DCustom<Event>)target.getAttribute(_index);
            attr.applyEvent(this);
        }

        protected Event (int targetId, short index) {
            super(targetId, index);
        }
    }

    /**
     * Adds a listener for value changes.
     */
    public void addListener (Listener<T> listener)
    {
        _listeners = addListener(_listeners, listener);
    }

    /**
     * Removes a listener for value changes.
     */
    public void removeListener (Listener<T> listener)
    {
        removeListener(_listeners, listener);
    }

    @Override // from DAttribute
    public void readContents (Streamable.Input in)
    {
        // nada
    }

    @Override // from DAttribute
    public void writeContents (Streamable.Output out)
    {
        // nada
    }

    protected void postEvent (T event)
    {
        _owner.postEvent(event);
    }

    protected void applyEvent (T event)
    {
        for (int ii = 0, ll = _listeners.length; ii < ll; ii++) {
            @SuppressWarnings("unchecked") Listener<T> listener = (Listener<T>)_listeners[ii];
            if (listener != null) {
                try {
                    listener.onEvent(event);
                } catch (Throwable t) {
                    log.warning("onEvent choked", "event", event, "listener", listener, t);
                }
            }
        }
    }

    /** Our registered listeners. */
    protected DListener[] _listeners = NO_LISTENERS;
}
