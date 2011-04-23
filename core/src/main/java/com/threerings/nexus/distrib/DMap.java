//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.threerings.nexus.io.Streamable;

import static com.threerings.nexus.util.Log.log;

/**
 * A map attribute for a Nexus object. Contains a mapping from keys to values.
 */
public class DMap<K,V> extends DAttribute
    implements Map<K,V>
{
    /** An interface for publishing map events to listeners. */
    public static interface Listener<K,V> extends DListener
    {
        /** Notifies listener of an added or updated mapping. */
        void entryPut (K key, V value, V oldValue);

        /** Notifies listener of a removed mapping. */
        void entryRemoved (K key, V oldValue);
    }

    /** An adaptor for listening to just put notifications. */
    public static abstract class PutListener<K,V> implements Listener<K,V>
    {
        public final void entryRemoved (K key, V oldValue) { /*NOOP*/ }
    }

    /** An adaptor for listening to just removed notifications. */
    public static abstract class RemovedListener<K,V> implements Listener<K,V>
    {
        public final void entryPut (K key, V value, V oldValue) { /*NOOP*/ }
    }

    /**
     * Creates a distributed map with the supplied underlying map implementation.
     */
    public static <K,V> DMap<K,V> create (Map<K,V> impl)
    {
        return new DMap<K,V>(impl);
    }

    /**
     * Creates a distributed map with the supplied underlying map implementation.
     */
    public DMap (Map<K,V> impl)
    {
        _impl = impl;
    }

    /**
     * Adds a listener for map changes.
     */
    public void addListener (Listener<K,V> listener)
    {
        _listeners = addListener(_listeners, listener);
    }

    /**
     * Removes a listener for map changes.
     */
    public void removeListener (Listener<K,V> listener)
    {
        removeListener(_listeners, listener);
    }

    // from interface Map<K,V>
    public int size ()
    {
        return _impl.size();
    }

    // from interface Map<K,V>
    public boolean isEmpty ()
    {
        return _impl.isEmpty();
    }

    // from interface Map<K,V>
    public boolean containsKey (Object key)
    {
        return _impl.containsKey(key);
    }

    // from interface Map<K,V>
    public boolean containsValue (Object value)
    {
        return _impl.containsValue(value);
    }

    // from interface Map<K,V>
    public V get (Object key)
    {
        return _impl.get(key);
    }

    // from interface Map<K,V>
    public V put (K key, V value)
    {
        checkMutate();
        V ovalue = _impl.get(key);
        postPut(key, value, ovalue);
        return ovalue;
    }

    // from interface Map<K,V>
    public V remove (Object rawKey)
    {
        checkMutate();

        // avoid generating an event if no mapping exists for the supplied key
        if (!_impl.containsKey(rawKey)) {
            return null;
        }

        @SuppressWarnings("unchecked") K key = (K)rawKey;
        V ovalue = _impl.remove(key);
        postRemoved(key, ovalue);

        return ovalue;
    }

    // from interface Map<K,V>
    public void putAll (Map<? extends K, ? extends V> map)
    {
        _impl.putAll(map); // this will call many put()s
    }

    // from interface Map<K,V>
    public void clear ()
    {
        checkMutate();
        // generate removed events for our keys (do so on a copy of our set so that we don't
        // trigger CME if the removed events are processed as they are generated)
        for (Map.Entry<K,V> entry : new HashSet<Map.Entry<K,V>>(_impl.entrySet())) {
            postRemoved(entry.getKey(), entry.getValue());
        }
        _impl.clear();
    }

    // from interface Map<K,V>
    public Set<K> keySet ()
    {
        final Set<K> iset = _impl.keySet();
        return new AbstractSet<K>() {
            public Iterator<K> iterator () {
                final Iterator<K> iiter = iset.iterator();
                return new Iterator<K>() {
                    public boolean hasNext () {
                        return iiter.hasNext();
                    }
                    public K next () {
                        return (_current = iiter.next());
                    }
                    public void remove () {
                        if (_current == null) {
                            throw new IllegalStateException();
                        }
                        checkMutate();
                        V ovalue = DMap.this.get(_current);
                        iiter.remove();
                        postRemoved(_current, ovalue);
                    }
                    protected K _current;
                };
            }
            public int size () {
                return iset.size();
            }
            public boolean remove (Object o) {
                checkMutate();
                @SuppressWarnings("unchecked") K key = (K)o;
                V ovalue = DMap.this.get(key);
                boolean modified = iset.remove(o);
                if (modified) {
                    postRemoved(key, ovalue);
                }
                return modified;
            }
            public void clear () {
                DMap.this.clear();
            }
        };
    }

    // from interface Map<K,V>
    public Collection<V> values ()
    {
        final Collection<Map.Entry<K,V>> iset = _impl.entrySet();
        return new AbstractCollection<V>() {
            public Iterator<V> iterator () {
                final Iterator<Map.Entry<K,V>> iiter = iset.iterator();
                return new Iterator<V>() {
                    public boolean hasNext () {
                        return iiter.hasNext();
                    }
                    public V next () {
                        return (_current = iiter.next()).getValue();
                    }
                    public void remove () {
                        if (_current == null) {
                            throw new IllegalStateException();
                        }
                        checkMutate();
                        iiter.remove();
                        postRemoved(_current.getKey(), _current.getValue());
                    }
                    protected Map.Entry<K,V> _current;
                };
            }
            public int size () {
                return iset.size();
            }
            public boolean contains (Object o) {
                return iset.contains(o);
            }
            public void clear () {
                DMap.this.clear();
            }
        };
    }

    // from interface Map<K,V>
    public Set<Map.Entry<K,V>> entrySet ()
    {
        final Set<Map.Entry<K,V>> iset = _impl.entrySet();
        return new AbstractSet<Map.Entry<K,V>>() {
            public Iterator<Map.Entry<K,V>> iterator () {
                final Iterator<Map.Entry<K,V>> iiter = iset.iterator();
                return new Iterator<Map.Entry<K,V>>() {
                    public boolean hasNext () {
                        return iiter.hasNext();
                    }
                    public Map.Entry<K,V> next () {
                        _current = iiter.next();
                        return new Map.Entry<K,V>() {
                            public K getKey () {
                                return _current.getKey();
                            }
                            public V getValue () {
                                return _current.getValue();
                            }
                            public V setValue (V value) {
                                checkMutate();
                                V ovalue = _current.setValue(value);
                                postPut(_current.getKey(), value, ovalue);
                                return ovalue;
                            }
                            // it's safe to pass these through because Map.Entry's
                            // implementations operate solely on getKey/getValue
                            public boolean equals (Object o) {
                                return _current.equals(o);
                            }
                            public int hashCode () {
                                return _current.hashCode();
                            }
                        };
                    }
                    public void remove () {
                        if (_current == null) {
                            throw new IllegalStateException();
                        }
                        checkMutate();
                        iiter.remove();
                        postRemoved(_current.getKey(), _current.getValue());
                    }
                    protected Map.Entry<K,V> _current;
                };
            }
            public boolean contains (Object o) {
                return iset.contains(o);
            }
            public boolean remove (Object o) {
                checkMutate();
                @SuppressWarnings("unchecked") Map.Entry<K,V> entry = (Map.Entry<K,V>)o;
                boolean modified = iset.remove(o);
                if (modified) {
                    postRemoved(entry.getKey(), entry.getValue());
                }
                return modified;
            }
            public int size () {
                return iset.size();
            }
            public void clear () {
                DMap.this.clear();
            }
        };
    }

    @Override // from DAttribute
    public void readContents (Streamable.Input in)
    {
        _impl = in.<Map<K,V>>readValue();
    }

    @Override // from DAttribute
    public void writeContents (Streamable.Output out)
    {
        out.writeValue(_impl);
    }

    protected void postPut (K key, V value, V ovalue)
    {
        PutEvent<K,V> event = new PutEvent<K,V>(_owner.getId(), _index, key, value);
        event.oldValue = ovalue;
        _owner.postEvent(event);
    }

    protected void applyPut (K key, V value, V oldValue)
    {
        V ovalue = DAttribute.chooseValue(_impl.put(key, value), oldValue);
        for (int ii = 0, ll = _listeners.length; ii < ll; ii++) {
            @SuppressWarnings("unchecked") Listener<K,V> listener = (Listener<K,V>)_listeners[ii];
            if (listener != null) {
                try {
                    listener.entryPut(key, value, ovalue);
                } catch (Throwable t) {
                    log.warning("Listener choked in entryPut", "key", key, "value", value,
                                "ovalue", oldValue, "listener", listener, t);
                }
            }
        }
    }

    protected void postRemoved (K key, V ovalue)
    {
        RemovedEvent<K,V> event = new RemovedEvent<K,V>(_owner.getId(), _index, key);
        event.oldValue = ovalue;
        _owner.postEvent(event);
    }

    protected void applyRemoved (K key, V oldValue)
    {
        V ovalue = DAttribute.chooseValue(_impl.remove(key), oldValue);
        for (int ii = 0, ll = _listeners.length; ii < ll; ii++) {
            @SuppressWarnings("unchecked") Listener<K,V> listener = (Listener<K,V>)_listeners[ii];
            if (listener != null) {
                try {
                    listener.entryRemoved(key, ovalue);
                } catch (Throwable t) {
                    log.warning("Listener choked in entryRemoved", "key", key, "ovalue", oldValue,
                                "listener", listener, t);
                }
            }
        }
    }

    /** An event emitted when a mapping is added or updated. */
    protected static class PutEvent<K,V> extends DAttribute.Event
    {
        public V oldValue = DAttribute.<V>sentinelValue();

        public PutEvent (int targetId, short index, K key, V value) {
            super(targetId, index);
            _key = key;
            _value = value;
        }

        @Override public void applyTo (NexusObject target) {
            @SuppressWarnings("unchecked") DMap<K,V> attr =
                (DMap<K,V>)target.getAttribute(this.index);
            attr.applyPut(_key, _value, oldValue);
        }

        @Override protected void toString (StringBuilder buf) {
            super.toString(buf);
            buf.append(", key=").append(_key);
            buf.append(", value=").append(_value);
        }

        protected final K _key;
        protected final V _value;
    }

    /** An event emitted when a mapping is removed. */
    protected static class RemovedEvent<K,V> extends DAttribute.Event
    {
        public V oldValue = DAttribute.<V>sentinelValue();

        public RemovedEvent (int targetId, short index, K key) {
            super(targetId, index);
            _key = key;
        }

        @Override public void applyTo (NexusObject target) {
            @SuppressWarnings("unchecked") DMap<K,V> attr =
                (DMap<K,V>)target.getAttribute(this.index);
            attr.applyRemoved(_key, oldValue);
        }

        @Override protected void toString (StringBuilder buf) {
            super.toString(buf);
            buf.append(", key=").append(_key);
        }

        protected final K _key;
    }

    /** Contains our underlying mappings. */
    protected Map<K, V> _impl;

    /** Our registered listeners. */
    protected transient DListener[] _listeners = NO_LISTENERS;
}
