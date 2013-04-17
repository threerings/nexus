//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import java.util.HashSet;
import java.util.Set;

import com.threerings.nexus.io.Streamable;

/**
 * A set attribute for a Nexus object. Contains an unordered set of distinct values.
 */
public class DSet<T> extends react.RSet<T>
    implements DAttribute
{
    /**
     * Creates a distributed set backed by a {@link HashSet}, with the specified owner.
     */
    public static <E> DSet<E> create (NexusObject owner) {
        return create(owner, new HashSet<E>());
    }

    /**
     * Creates a distributed set with the specified owner and underlying set implementation.
     */
    public static <T> DSet<T> create (NexusObject owner, Set<T> impl) {
        return new DSet<T>(owner, impl);
    }

    @Override public void readContents (Streamable.Input in) {
        _impl = in.<Set<T>>readValue();
    }

    @Override public void writeContents (Streamable.Output out) {
        out.writeValue(_impl);
    }

    protected DSet (NexusObject owner, Set<T> impl) {
        super(impl);
        _owner = owner;
        _index = owner.registerAttr(this);
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
            target.<DSet<T>>getAttribute(this.index).applyAdd(_elem);
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
            target.<DSet<T>>getAttribute(this.index).applyRemove(_elem);
        }

        @Override protected void toString (StringBuilder buf) {
            super.toString(buf);
            buf.append(", elem=").append(_elem);
        }

        protected final T _elem;
    }

    /** The object that owns this attribute. */
    protected final NexusObject _owner;

    /** The index of this attribute in its containing object. */
    protected final short _index;
}
