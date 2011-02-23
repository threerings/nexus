//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.distrib;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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
        V ovalue = _impl.get(key);
        postPut(key, value, ovalue);
        return ovalue;
    }

    // from interface Map<K,V>
    public V remove (Object rawKey)
    {
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
        // generate removed events for our keys
        for (Map.Entry<K,V> entry : entrySet()) {
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
                        // TODO: generate removed event with _current
                        iiter.remove();
                    }
                    protected K _current;
                };
            }
            public int size () {
                return iset.size();
            }
            public boolean remove (Object o) {
                // TODO: machinery!
                return iset.remove(o);
            }
            public void clear () {
                DMap.this.clear();
            }
        };
    }

    // from interface Map<K,V>
    public Collection<V> values ()
    {
        final Collection<V> ivals = _impl.values();
        return new AbstractCollection<V>() {
            public Iterator<V> iterator () {
                final Iterator<V> iiter = ivals.iterator();
                return new Iterator<V>() {
                    public boolean hasNext () {
                        return iiter.hasNext();
                    }
                    public V next () {
                        return (_current = iiter.next());
                    }
                    public void remove () {
                        if (_current == null) {
                            throw new IllegalStateException();
                        }
                        // TODO: generate removed event with _current
                        iiter.remove();
                    }
                    protected V _current;
                };
            }
            public int size () {
                return ivals.size();
            }
            public boolean contains (Object o) {
                return ivals.contains(o);
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
                        return (_current = iiter.next());
                    }
                    public void remove () {
                        if (_current == null) {
                            throw new IllegalStateException();
                        }
                        // TODO: generate removed event with _current
                        iiter.remove();
                    }
                    protected Map.Entry<K,V> _current;
                };
            }
            public boolean contains (Object o) {
                return iset.contains(o);
            }
            public boolean remove (Object o) {
                // TODO: machinery!
                return iset.remove(o);
            }
            public int size () {
                return iset.size();
            }
            public void clear () {
                DMap.this.clear();
            }
        };
    }

    @Override // from Object
    public boolean equals (Object o)
    {
        return (o instanceof DMap<?,?>) && _impl.equals(((DMap<?,?>)o)._impl);
    }

    @Override // from Object
    public int hashCode()
    {
        return _impl.hashCode();
    }

    protected void postPut (K key, V value, V ovalue)
    {
        PutEvent<K,V> event = new PutEvent<K,V>(_index, key, value);
        event.oldValue = ovalue;
        _owner.postEvent(event);
    }

    protected void applyPut (K key, V value, V oldValue)
    {
        V ovalue = DAttribute.chooseValue(_impl.put(key, value), oldValue);
        for (int ii = 0, ll = _listeners.length; ii < ll; ii++) {
            @SuppressWarnings("unchecked") Listener<K,V> listener = (Listener<K,V>)_listeners[ii];
            if (listener != null) {
                listener.entryPut(key, value, ovalue);
            }
        }
    }

    protected void postRemoved (K key, V ovalue)
    {
        RemovedEvent<K,V> event = new RemovedEvent<K,V>(_index, key);
        event.oldValue = ovalue;
        _owner.postEvent(event);
    }

    protected void applyRemoved (K key, V oldValue)
    {
        V ovalue = DAttribute.chooseValue(_impl.remove(key), oldValue);
        for (int ii = 0, ll = _listeners.length; ii < ll; ii++) {
            @SuppressWarnings("unchecked") Listener<K,V> listener = (Listener<K,V>)_listeners[ii];
            if (listener != null) {
                listener.entryRemoved(key, ovalue);
            }
        }
    }

    /** An event emitted when a mapping is added or updated. */
    protected static class PutEvent<K,V> extends DAttribute.Event
    {
        public V oldValue = DAttribute.<V>sentinelValue();

        public PutEvent (short index, K key, V value) {
            super(index);
            _key = key;
            _value = value;
        }

        @Override public void applyTo (NexusObject target) {
            @SuppressWarnings("unchecked") DMap<K,V> attr = (DMap<K,V>)target.getAttribute(_index);
            attr.applyPut(_key, _value, oldValue);
        }

        protected final K _key;
        protected final V _value;
    }

    /** An event emitted when a mapping is removed. */
    protected static class RemovedEvent<K,V> extends DAttribute.Event
    {
        public V oldValue = DAttribute.<V>sentinelValue();

        public RemovedEvent (short index, K key) {
            super(index);
            _key = key;
        }

        @Override public void applyTo (NexusObject target) {
            @SuppressWarnings("unchecked") DMap<K,V> attr = (DMap<K,V>)target.getAttribute(_index);
            attr.applyRemoved(_key, oldValue);
        }

        protected final K _key;
    }

    /** Contains our underlying mappings. */
    protected Map<K, V> _impl;

    /** Our registered listeners. */
    protected transient DListener[] _listeners = NO_LISTENERS;
}
