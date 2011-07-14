//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import com.threerings.nexus.io.Streamable;

import static com.threerings.nexus.util.Log.log;

/**
 * A set attribute for a Nexus object. Contains an unordered set of distinct values.
 */
public class DSet<T> extends DAttribute
    implements Set<T>
{
    /** An interface for publishing set events to listeners. */
    public static interface Listener<T> extends DListener
    {
        /** Notifies listener of an added element. */
        void elementAdded (T element);

        /** Notifies listener of a removed element. */
        void elementRemoved (T element);
    }

    /** An adaptor for listening to just added notifications. */
    public static abstract class AddedListener<T> implements Listener<T>
    {
        public final void elementRemoved (T element) { /*NOOP*/ }
    }

    /** An adaptor for listening to just removed notifications. */
    public static abstract class RemovedListener<T> implements Listener<T>
    {
        public final void elementAdded (T element) { /*NOOP*/ }
    }

    /**
     * Creates a distributed set with the supplied underlying set implementation.
     */
    public static <T> DSet<T> create (Set<T> impl)
    {
        return new DSet<T>(impl);
    }

    /**
     * Creates a distributed set with the supplied underlying set implementation.
     */
    public DSet (Set<T> impl)
    {
        _impl = impl;
    }

    /**
     * Adds a listener for set changes.
     */
    public void addListener (Listener<T> listener)
    {
        _listeners = addListener(_listeners, listener);
    }

    /**
     * Removes a listener for set changes.
     */
    public void removeListener (Listener<T> listener)
    {
        removeListener(_listeners, listener);
    }

    // from interface Set<T>
    public int size ()
    {
        return _impl.size();
    }

    // from interface Set<T>
    public boolean isEmpty ()
    {
        return _impl.isEmpty();
    }

    // from interface Set<T>
    public boolean contains (Object key)
    {
        return _impl.contains(key);
    }

    // from interface Set<T>
    public boolean add (T elem)
    {
        checkMutate();
        if (!_impl.add(elem)) return false;
        postAdded(elem);
        return true;
    }

    // from interface Set<T>
    public boolean remove (Object rawElem)
    {
        checkMutate();
        @SuppressWarnings("unchecked") T elem = (T)rawElem;
        if (!_impl.remove(elem)) return false;
        postRemoved(elem);
        return true;
    }

    // from interface Set<T>
    public boolean containsAll (Collection<?> coll)
    {
        return _impl.containsAll(coll);
    }

    // from interface Set<T>
    public boolean addAll (Collection<? extends T> coll)
    {
        boolean modified = false;
        for (T elem : coll) {
            if (add(elem)) {
                modified = true;
            }
        }
        return modified;
    }

    // from interface Set<T>
    public boolean retainAll (Collection<?> coll)
    {
        boolean modified = false;
        for (Iterator<T> iter = iterator(); iter.hasNext(); ) {
            if (!coll.contains(iter.next())) {
                iter.remove();
                modified = true;
            }
        }
        return modified;
    }

    // from interface Set<T>
    public boolean removeAll (Collection<?> coll)
    {
        boolean modified = false;
        for (Iterator<T> iter = iterator(); iter.hasNext(); ) {
            if (remove(iter.next())) {
                modified = true;
            }
        }
        return modified;
    }

    // from interface Set<T>
    public void clear ()
    {
        checkMutate();
        // generate removed events for our elements (do so on a copy of our set so that we don't
        // trigger CME if the removed events are processed as they are generated)
        for (T elem : new ArrayList<T>(_impl)) {
            postRemoved(elem);
        }
        _impl.clear();
    }

    // from interface Set<T>
    public Iterator<T> iterator ()
    {
        final Iterator<T> iiter = _impl.iterator();
        return new Iterator<T>() {
            public boolean hasNext () {
                return iiter.hasNext();
            }
            public T next () {
                return (_current = iiter.next());
            }
            public void remove () {
                if (_current == null) {
                    throw new IllegalStateException();
                }
                checkMutate();
                iiter.remove();
                postRemoved(_current);
            }
            protected T _current;
        };
    }

    // from interface Set<T>
    public Object[] toArray ()
    {
        return _impl.toArray();
    }

    // from interface Set<T>
    public <T> T[] toArray (T[] array)
    {
        return _impl.toArray(array);
    }

    @Override // from DAttribute
    public void readContents (Streamable.Input in)
    {
        _impl = in.<Set<T>>readValue();
    }

    @Override // from DAttribute
    public void writeContents (Streamable.Output out)
    {
        out.writeValue(_impl);
    }

    protected void postAdded (T elem)
    {
        _owner.postEvent(new AddedEvent<T>(_owner.getId(), _index, elem));
    }

    protected void applyAdded (T elem)
    {
        _impl.add(elem);
        for (int ii = 0, ll = _listeners.length; ii < ll; ii++) {
            @SuppressWarnings("unchecked") Listener<T> listener = (Listener<T>)_listeners[ii];
            if (listener != null) {
                try {
                    listener.elementAdded(elem);
                } catch (Throwable t) {
                    log.warning("Listener choked in elementAdded", "elem", elem,
                                "listener", listener, t);
                }
            }
        }
    }

    protected void postRemoved (T elem)
    {
        _owner.postEvent(new RemovedEvent<T>(_owner.getId(), _index, elem));
    }

    protected void applyRemoved (T elem)
    {
        _impl.remove(elem);
        for (int ii = 0, ll = _listeners.length; ii < ll; ii++) {
            @SuppressWarnings("unchecked") Listener<T> listener = (Listener<T>)_listeners[ii];
            if (listener != null) {
                try {
                    listener.elementRemoved(elem);
                } catch (Throwable t) {
                    log.warning("Listener choked in elementRemoved", "elem", elem,
                                "listener", listener, t);
                }
            }
        }
    }

    /** An event emitted when an element is added. */
    protected static class AddedEvent<T> extends DAttribute.Event
    {
        public AddedEvent (int targetId, short index, T elem) {
            super(targetId, index);
            _elem = elem;
        }

        @Override public void applyTo (NexusObject target) {
            @SuppressWarnings("unchecked") DSet<T> attr = (DSet<T>)target.getAttribute(this.index);
            attr.applyAdded(_elem);
        }

        @Override protected void toString (StringBuilder buf) {
            super.toString(buf);
            buf.append(", elem=").append(_elem);
        }

        protected final T _elem;
    }

    /** An event emitted when an element is removed. */
    protected static class RemovedEvent<T> extends DAttribute.Event
    {
        public RemovedEvent (int targetId, short index, T elem) {
            super(targetId, index);
            _elem = elem;
        }

        @Override public void applyTo (NexusObject target) {
            @SuppressWarnings("unchecked") DSet<T> attr = (DSet<T>)target.getAttribute(this.index);
            attr.applyRemoved(_elem);
        }

        @Override protected void toString (StringBuilder buf) {
            super.toString(buf);
            buf.append(", elem=").append(_elem);
        }

        protected final T _elem;
    }

    /** Contains our underlying elements. */
    protected Set<T> _impl;

    /** Our registered listeners. */
    protected transient DListener[] _listeners = NO_LISTENERS;
}
