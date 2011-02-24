//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.distrib;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import com.samskivert.nexus.io.Streamable;

/**
 * Handles the streaming of {@link DMap} instances and its internal events.
 */
public class Streamer_DMap
{
    public <K,V> void writeObject (Streamable.Output out, DMap<K,V> obj)
    {
        out.writeValues(obj.keySet());
        out.writeValues(obj.values());
    }

    public <K,V> DMap<K,V> readObject (Streamable.Input in)
    {
        Collection<K> keys = in.<K>readValues();
        Collection<V> vals = in.<V>readValues();
        Iterator<K> kiter = keys.iterator();
        Iterator<V> viter = vals.iterator();
        DMap<K,V> map = DMap.create(new HashMap<K,V>(keys.size()));
        while (kiter.hasNext()) {
            map.put(kiter.next(), viter.next());
        }
        return map;
    }

    public static class PutEvent {
        public <K,V> void writeObject (Streamable.Output out, DMap.PutEvent<K,V> obj)
        {
            out.writeShort(obj._index);
            out.writeValue(obj._key);
            out.writeValue(obj._value);
        }

        public <K,V> DMap.PutEvent<K,V> readObject (Streamable.Input in)
        {
            return new DMap.PutEvent<K,V>(in.readShort(), in.<K>readValue(), in.<V>readValue());
        }
    }

    public static class RemovedEvent {
        public <K,V> void writeObject (Streamable.Output out, DMap.RemovedEvent<K,V> obj)
        {
            out.writeShort(obj._index);
            out.writeValue(obj._key);
        }

        public <K,V> DMap.RemovedEvent<K,V> readObject (Streamable.Input in)
        {
            return new DMap.RemovedEvent<K,V>(in.readShort(), in.<K>readValue());
        }
    }
}
