//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.distrib;

import java.util.HashMap;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Tests the myriad wirings of the distributed map.
 */
public class DMapTest
{
    public static class MapObject extends NexusObject {
        public DMap<Integer,String> map = DMap.create(new HashMap<Integer,String>());

        @Override protected DAttribute getAttribute (int index) {
            switch (index) {
            case 0: return map;
            default: throw new IndexOutOfBoundsException("Invalid attribute index " + index);
            }
        }

        @Override protected int getAttributeCount () {
            return 1;
        }
    }

    @Test
    public void testBasicMapActions ()
    {
        TestSink sink = new TestSink();

        MapObject obj = new MapObject();
        obj._sink = sink;
        obj.initAttributes();

        prepPutCheck(obj.map, 1, "One", null);
        obj.map.put(1, "One");
        checkPutEvent(sink, 1, "One");
        assertEquals("One", obj.map.get(1));

        prepPutCheck(obj.map, 1, "Two", "One");
        obj.map.put(1, "Two");
        checkPutEvent(sink, 1, "Two");

        prepRemovedCheck(obj.map, 1, "Two");
        obj.map.remove(1);
        checkRemovedEvent(sink, 1);
    }

    protected <K, V> void prepPutCheck (
        final DMap<K, V> map, final K ekey, final V evalue, final V eoldValue)
    {
        map.addListener(new DMap.PutListener<K, V>() {
            public void entryPut (K key, V value, V oldValue) {
                assertEquals(ekey, key);
                assertEquals(evalue, value);
                assertEquals(eoldValue, oldValue);
                map.removeListener(this);
            }
        });
    }

    protected <K, V> void prepRemovedCheck (
        final DMap<K, V> map, final K ekey, final V eoldValue)
    {
        map.addListener(new DMap.RemovedListener<K, V>() {
            public void entryRemoved (K key, V oldValue) {
                assertEquals(ekey, key);
                assertEquals(eoldValue, oldValue);
                map.removeListener(this);
            }
        });
    }

    protected <K, V> void checkPutEvent (TestSink sink, K key, V value)
    {
        @SuppressWarnings("unchecked") DMap.PutEvent<K, V> event =
            (DMap.PutEvent<K, V>)sink.assertPosted(DMap.PutEvent.class);
        assertEquals(key, event._key);
        assertEquals(value, event._value);
    }

    protected <K, V> void checkRemovedEvent (TestSink sink, K key)
    {
        @SuppressWarnings("unchecked") DMap.RemovedEvent<K, V> event =
            (DMap.RemovedEvent<K, V>)sink.assertPosted(DMap.RemovedEvent.class);
        assertEquals(key, event._key);
    }
}
