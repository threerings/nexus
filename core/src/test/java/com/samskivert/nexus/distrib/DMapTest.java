//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.distrib;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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
        obj.init(sink);

        // test add
        prepPutCheck(obj.map, 1, "One", null);
        obj.map.put(1, "One");
        checkPutEvent(sink, 1, "One");
        assertEquals("One", obj.map.get(1));
        assertEquals(1, obj.map.size());

        // test update
        prepPutCheck(obj.map, 1, "Two", "One");
        obj.map.put(1, "Two");
        checkPutEvent(sink, 1, "Two");
        assertEquals(1, obj.map.size());

        // test remove
        prepRemovedCheck(obj.map, 1, "Two");
        obj.map.remove(1);
        checkRemovedEvent(sink, 1);
        assertEquals(0, obj.map.size());

        // test clear
        obj.map.put(1, "One");
        checkPutEvent(sink, 1, "One");
        assertEquals(1, obj.map.size());
        prepRemovedCheck(obj.map, 1, "One");
        obj.map.clear();
        checkRemovedEvent(sink, 1);
        assertEquals(0, obj.map.size());
    }

    @Test
    public void testKeySetView ()
    {
        TestSink sink = new TestSink();

        MapObject obj = new MapObject();
        obj.init(sink);

        populateMap(sink, obj.map);

        // test the key set view
        Set<Integer> keySet = obj.map.keySet();
        assertEquals(obj.map.size(), keySet.size());

        // remove via the key set
        prepRemovedCheck(obj.map, 1, "One");
        keySet.remove(1);
        checkRemovedEvent(sink, 1);

        // remove via the key set iterator
        Iterator<Integer> iter = keySet.iterator();
        Integer key = iter.next();
        prepRemovedCheck(obj.map, key, obj.map.get(key));
        iter.remove();
        checkRemovedEvent(sink, key);

        // clear via the key set (should get two more removed events)
        keySet.clear();
        sink.assertPostedCount(2);
        assertEquals(0, keySet.size());
        assertEquals(0, obj.map.size());

        // repopulate so that we can test removeAll and retainAll
        populateMap(sink, obj.map);
        assertEquals(obj.map.size(), keySet.size());

        keySet.removeAll(Arrays.asList(1, 2, 5));
        sink.assertPostedCount(2);
        assertEquals(2, keySet.size());
        assertEquals(2, obj.map.size());

        keySet.retainAll(Arrays.asList(3));
        sink.assertPostedCount(1);
        assertEquals(1, keySet.size());
        assertEquals(1, obj.map.size());
    }

    @Test
    public void testEntrySetView ()
    {
        TestSink sink = new TestSink();

        MapObject obj = new MapObject();
        obj.init(sink);

        populateMap(sink, obj.map);

        // test the entry set view
        Set<Map.Entry<Integer,String>> entrySet = obj.map.entrySet();
        assertEquals(obj.map.size(), entrySet.size());

        // remove via the entry set iterator
        Iterator<Map.Entry<Integer,String>> iter = entrySet.iterator();
        Map.Entry<Integer,String> entry = iter.next();
        prepRemovedCheck(obj.map, entry.getKey(), entry.getValue());
        iter.remove();
        checkRemovedEvent(sink, entry.getKey());

        // remove via the entry set
        entry = iter.next();
        prepRemovedCheck(obj.map, entry.getKey(), entry.getValue());
        entrySet.remove(entry);
        checkRemovedEvent(sink, entry.getKey());

        // clear via the entry set (should get two more removed events)
        entrySet.clear();
        sink.assertPostedCount(2);
        assertEquals(0, entrySet.size());
        assertEquals(0, obj.map.size());

        // we're going to punt on removeAll and retainAll, if those work for keySet, they should
        // work for entrySet (and values) and coming up with entries to remove or retain is a PITA

        // test Map.Entry.setValue
        populateMap(sink, obj.map);

        entry = entrySet.iterator().next();
        prepPutCheck(obj.map, entry.getKey(), "Bang!", entry.getValue());
        entry.setValue("Bang!");
        checkPutEvent(sink, entry.getKey(), "Bang!");
    }

    @Test
    public void testValuesView ()
    {
        TestSink sink = new TestSink();

        MapObject obj = new MapObject();
        obj.init(sink);

        populateMap(sink, obj.map);

        // test the values view
        Collection<String> values = obj.map.values();
        assertEquals(obj.map.size(), values.size());

        // remove via the values
        prepRemovedCheck(obj.map, 1, "One");
        values.remove("One");
        checkRemovedEvent(sink, 1);

        // remove via the values iterator
        Iterator<String> iter = values.iterator();
        String value = iter.next();
        prepRemovedCheck(obj.map, fromValue(value), value);
        iter.remove();
        checkRemovedEvent(sink, fromValue(value));

        // clear via the values (should get two more removed events)
        values.clear();
        sink.assertPostedCount(2);
        assertEquals(0, values.size());
        assertEquals(0, obj.map.size());

        // repopulate so that we can test removeAll and retainAll
        populateMap(sink, obj.map);
        assertEquals(obj.map.size(), values.size());

        values.removeAll(Arrays.asList("One", "Two", "Five"));
        sink.assertPostedCount(2);
        assertEquals(2, values.size());
        assertEquals(2, obj.map.size());

        values.retainAll(Arrays.asList("Three"));
        sink.assertPostedCount(1);
        assertEquals(1, values.size());
        assertEquals(1, obj.map.size());
    }

    protected void populateMap (TestSink sink, DMap<Integer, String> map)
    {
        map.put(1, "One");
        map.put(2, "Two");
        map.put(3, "Three");
        map.put(4, "Four");
        // we need to consume the put events from the sink
        checkPutEvent(sink, 1, "One");
        checkPutEvent(sink, 2, "Two");
        checkPutEvent(sink, 3, "Three");
        checkPutEvent(sink, 4, "Four");
    }

    protected Integer fromValue (String value)
    {
        if (value.equals("One")) return 1;
        else if (value.equals("Two")) return 2;
        else if (value.equals("Three")) return 3;
        else if (value.equals("Four")) return 4;
        else throw new IllegalArgumentException();
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
