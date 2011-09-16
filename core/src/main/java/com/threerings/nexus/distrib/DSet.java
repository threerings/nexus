//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import java.util.Set;

import com.threerings.nexus.io.Streamable;
import static com.threerings.nexus.util.Log.log;

/**
 * A set attribute for a Nexus object. Contains an unordered set of distinct values.
 */
public class DSet<T> extends react.RSet<T>
    implements DAttribute
{
    /**
     * Creates a distributed set with the supplied underlying set implementation.
     */
    public static <T> DSet<T> create (Set<T> impl) {
        return new DSet<T>(impl);
    }

    @Override public void init (NexusObject owner, short index) {
        _owner = owner;
        _index = index;
    }

    @Override public void readContents (Streamable.Input in) {
        _impl = in.<Set<T>>readValue();
    }

    @Override public void writeContents (Streamable.Output out) {
        out.writeValue(_impl);
    }

    protected DSet (Set<T> impl) {
        super(impl);
    }

    protected void applyAdd (T elem) {
        _impl.add(elem);
        notifyAdd(elem);
    }

    protected void applyRemove (T elem) {
        _impl.remove(elem);
        notifyRemove(elem);
    }

    @Override protected void emitAdd (T elem) {
        _owner.postEvent(new AddEvent<T>(_owner.getId(), _index, elem));
    }

    @Override protected void emitRemove (T elem) {
        _owner.postEvent(new RemoveEvent<T>(_owner.getId(), _index, elem));
    }

    /** An event emitted when an element is added. */
    protected static class AddEvent<T> extends DAttribute.Event {
        public AddEvent (int targetId, short index, T elem) {
            super(targetId, index);
            _elem = elem;
        }

        @Override public void applyTo (NexusObject target) {
            @SuppressWarnings("unchecked") DSet<T> attr = (DSet<T>)target.getAttribute(this.index);
            attr.applyAdd(_elem);
        }

        @Override protected void toString (StringBuilder buf) {
            super.toString(buf);
            buf.append(", elem=").append(_elem);
        }

        protected final T _elem;
    }

    /** An event emitted when an element is removed. */
    protected static class RemoveEvent<T> extends DAttribute.Event {
        public RemoveEvent (int targetId, short index, T elem) {
            super(targetId, index);
            _elem = elem;
        }

        @Override public void applyTo (NexusObject target) {
            @SuppressWarnings("unchecked") DSet<T> attr = (DSet<T>)target.getAttribute(this.index);
            attr.applyRemove(_elem);
        }

        @Override protected void toString (StringBuilder buf) {
            super.toString(buf);
            buf.append(", elem=").append(_elem);
        }

        protected final T _elem;
    }

    /** The object that owns this attribute. */
    protected NexusObject _owner;

    /** The index of this attribute in its containing object. */
    protected short _index;
}
