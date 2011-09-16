//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import com.threerings.nexus.io.Streamable;
import static com.threerings.nexus.util.Log.log;

/**
 * A signal attribute which retains no data, but serves as a means by which to emit and listen for
 * custom events. One uses it like so:
 * <pre>{@code
 * class MyObject extends NexusObject {
 *   public static class MyEvent implements Streamable {
 *     public final String name;
 *     public final int value;
 *     public MyEvent (String name, int value) {
 *       this.name = name;
 *       this.value = value;
 *     }
 *   }
 *   public DSignal<MyEvent> myEvent = DSignal.create();
 * }
 * MyObject obj = ...;
 * obj.myEvent.connect(new Slot<MyEvent>() {
 *   public void onEmit (MyEvent event) { ... }
 * });
 * obj.myEvent.emit(new MyEvent("answer", 42));
 * </pre>
 */
public class DSignal<T> extends react.AbstractSignal<T>
    implements DAttribute
{
    /**
     * Convenience method for creating a signal without repeating the type parameter.
     */
    public static <T> DSignal<T> create () {
        return new DSignal<T>();
    }

    /**
     * Causes this signal to emit the supplied event.
     */
    public void emit (T event) {
        _owner.postEvent(new EmitEvent<T>(_owner.getId(), _index, event));
    }

    @Override public void init (NexusObject owner, short index) {
        _owner = owner;
        _index = index;
    }

    @Override public void readContents (Streamable.Input in) {
        // nada
    }

    @Override public void writeContents (Streamable.Output out) {
        // nada
    }

    protected void applyEmit (T event) {
        notifyEmit(event);
    }

    /** An event emitted when an event is emitted. How meta! */
    protected static class EmitEvent<T> extends DAttribute.Event {
        public EmitEvent (int targetId, short index, T event) {
            super(targetId, index);
            _event = event;
        }

        @Override public void applyTo (NexusObject target) {
            @SuppressWarnings("unchecked") DSignal<T> attr =
                (DSignal<T>)target.getAttribute(this.index);
            attr.applyEmit(_event);
        }

        protected T _event;
    }

    /** The object that owns this attribute. */
    protected NexusObject _owner;

    /** The index of this attribute in its containing object. */
    protected short _index;
}
