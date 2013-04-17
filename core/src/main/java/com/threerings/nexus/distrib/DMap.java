//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import java.util.HashMap;
import java.util.Map;

import com.threerings.nexus.io.Streamable;

/**
 * A map attribute for a Nexus object. Contains a mapping from keys to values.
 */
public class DMap<K,V> extends react.RMap<K,V>
    implements DAttribute
{
    /**
     * Creates a distributed map that uses a {@link HashMap} as its underlying implementation.
     */
    public static <K,V> DMap<K,V> create (NexusObject owner) {
        return create(owner, new HashMap<K,V>());
    }

    /**
     * Creates a distributed map with the supplied underlying map implementation.
     */
    public static <K,V> DMap<K,V> create (NexusObject owner, Map<K,V> impl) {
        return new DMap<K,V>(owner, impl);
    }

    @Override public void readContents (Streamable.Input in) {
        _impl = in.<Map<K,V>>readValue();
    }

    @Override public void writeContents (Streamable.Output out) {
        out.writeValue(_impl);
    }

    protected DMap (NexusObject owner, Map<K,V> impl) {
        super(impl);
        _owner = owner;
        _index = owner.registerAttr(this);
    }

    @Override protected void emitPut (K key, V value, V oldValue) {
        // we don't call super as we defer notification until the event is dispatched
        PutEvent<K,V> event = new PutEvent<K,V>(_owner.getId(), _index, key, value);
        event.oldValue = oldValue;
        _owner.postEvent(event);
    }

    protected void applyPut (K key, V value, V oldValue) {
        if (oldValue == DistribUtil.<V>sentinelValue()) {
            // we came in over the network: update our underlying map
            oldValue = _impl.put(key, value);
        }
        notifyPut(key, value, oldValue);
    }

    @Override protected void emitRemove (K key, V oldValue) {
        // we don't call super as we defer notification until the event is dispatched
        RemoveEvent<K,V> event = new RemoveEvent<K,V>(_owner.getId(), _index, key);
        event.oldValue = oldValue;
        _owner.postEvent(event);
    }

    protected void applyRemove (K key, V oldValue) {
        if (oldValue == DistribUtil.<V>sentinelValue()) {
            // we came in over the network: update our underlying map
            oldValue = _impl.remove(key);
        }
        notifyRemove(key, oldValue);
    }

    /** An event emitted when a mapping is added or updated. */
    protected static class PutEvent<K,V> extends DAttribute.Event {
        public V oldValue = DistribUtil.<V>sentinelValue();

        public PutEvent (int targetId, short index, K key, V value) {
            super(targetId, index);
            _key = key;
            _value = value;
        }

        @Override public void applyTo (NexusObject target) {
            target.<DMap<K,V>>getAttribute(this.index).applyPut(_key, _value, oldValue);
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
    protected static class RemoveEvent<K,V> extends DAttribute.Event {
        public V oldValue = DistribUtil.<V>sentinelValue();

        public RemoveEvent (int targetId, short index, K key) {
            super(targetId, index);
            _key = key;
        }

        @Override public void applyTo (NexusObject target) {
            target.<DMap<K,V>>getAttribute(this.index).applyRemove(_key, oldValue);
        }

        @Override protected void toString (StringBuilder buf) {
            super.toString(buf);
            buf.append(", key=").append(_key);
        }

        protected final K _key;
    }

    /** The object that owns this attribute. */
    protected final NexusObject _owner;

    /** The index of this attribute in its containing object. */
    protected final short _index;
}
