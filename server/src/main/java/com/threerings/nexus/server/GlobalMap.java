//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.server;

import com.google.common.collect.Maps;

import com.threerings.nexus.distrib.DistribUtil;

/**
 * A map whose contents are mirrored across all servers in the network.
 */
class GlobalMap<K,V> extends react.RMap<K,V>
{
    public GlobalMap (String id, ObjectManager omgr) {
        super(Maps.<K,V>newConcurrentMap());
        _id = id;
        _omgr = omgr;
    }

    public void applyPut (K key, V value, V oldValue) {
        if (oldValue == DistribUtil.<V>sentinelValue()) {
            // we came in over the network: update our underlying map
            oldValue = _impl.put(key, value);
        }
        notifyPut(key, value, oldValue);
    }

    public void applyRemove (K key, V oldValue) {
        if (oldValue == DistribUtil.<V>sentinelValue()) {
            // we came in over the network: update our underlying map
            oldValue = _impl.remove(key);
        }
        notifyRemove(key, oldValue);
    }

    @Override protected void emitPut (K key, V value, V oldValue) {
        // we don't call super, which would cause a change notification to be dispatched on the
        // caller's thread; instead we tell the object manager about this update and it queues the
        // update for distribution to other nodes and then invokes code on this map's execution
        // context to call applyPut, which will then notify listeners
        _omgr.emitMapPut(_id, key, value, oldValue);
    }

    @Override protected void emitRemove (K key, V oldValue) {
        // we don't call super, see emitPut for explanatory details
        _omgr.emitMapRemove(_id, key, oldValue);
    }

    protected final String _id;
    protected final ObjectManager _omgr;
}
