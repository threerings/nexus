//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.io;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Defines streamers for all of the basic types supported by Nexus.
 */
public class Streamers
{
    /** Creates a streamer for the supplied enum class. */
    public static <T extends Enum<T>> Streamer_Enum<T> create (Class<T> eclass) {
        return new Streamer_Enum<T>(eclass);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Streamer<?> forEnum (Class<?> eclass) {
        return new Streamers.Streamer_Enum(eclass);
    }

    /** A streamer for null instances. */
    public static class Streamer_Null implements Streamer<Void> {
        public Class<?> getObjectClass () {
            return Void.class;
        }
        public void writeObject (Streamable.Output out, Void value) {
            // nothing need be written
        }
        public Void readObject (Streamable.Input in) {
            return null;
        }
    }

    /** A streamer for {@link Boolean}. */
    public static class Streamer_Boolean implements Streamer<Boolean> {
        public Class<?> getObjectClass () {
            return Boolean.class;
        }
        public void writeObject (Streamable.Output out, Boolean value) {
            out.writeBoolean(value);
        }
        public Boolean readObject (Streamable.Input in) {
            return in.readBoolean();
        }
    }

    /** A streamer for {@link Byte}. */
    public static class Streamer_Byte implements Streamer<Byte> {
        public Class<?> getObjectClass () {
            return Byte.class;
        }
        public void writeObject (Streamable.Output out, Byte value) {
            out.writeByte(value);
        }
        public Byte readObject (Streamable.Input in) {
            return in.readByte();
        }
    }

    /** A streamer for {@link Character}. */
    public static class Streamer_Character implements Streamer<Character> {
        public Class<?> getObjectClass () {
            return Character.class;
        }
        public void writeObject (Streamable.Output out, Character value) {
            out.writeChar(value);
        }
        public Character readObject (Streamable.Input in) {
            return in.readChar();
        }
    }

    /** A streamer for {@link Short}. */
    public static class Streamer_Short implements Streamer<Short> {
        public Class<?> getObjectClass () {
            return Short.class;
        }
        public void writeObject (Streamable.Output out, Short value) {
            out.writeShort(value);
        }
        public Short readObject (Streamable.Input in) {
            return in.readShort();
        }
    }

    /** A streamer for {@link Integer}. */
    public static class Streamer_Integer implements Streamer<Integer> {
        public Class<?> getObjectClass () {
            return Integer.class;
        }
        public void writeObject (Streamable.Output out, Integer value) {
            out.writeInt(value);
        }
        public Integer readObject (Streamable.Input in) {
            return in.readInt();
        }
    }

    /** A streamer for {@link Long}. */
    public static class Streamer_Long implements Streamer<Long> {
        public Class<?> getObjectClass () {
            return Long.class;
        }
        public void writeObject (Streamable.Output out, Long value) {
            out.writeLong(value);
        }
        public Long readObject (Streamable.Input in) {
            return in.readLong();
        }
    }

    /** A streamer for {@link Float}. */
    public static class Streamer_Float implements Streamer<Float> {
        public Class<?> getObjectClass () {
            return Float.class;
        }
        public void writeObject (Streamable.Output out, Float value) {
            out.writeFloat(value);
        }
        public Float readObject (Streamable.Input in) {
            return in.readFloat();
        }
    }

    /** A streamer for {@link Double}. */
    public static class Streamer_Double implements Streamer<Double> {
        public Class<?> getObjectClass () {
            return Double.class;
        }
        public void writeObject (Streamable.Output out, Double value) {
            out.writeDouble(value);
        }
        public Double readObject (Streamable.Input in) {
            return in.readDouble();
        }
    }

    // TODO: boxed streamers?

    /** A streamer for {@link String}. */
    public static class Streamer_String implements Streamer<String> {
        public Class<?> getObjectClass () {
            return String.class;
        }
        public void writeObject (Streamable.Output out, String value) {
            out.writeString(value);
        }
        public String readObject (Streamable.Input in) {
            return in.readString();
        }
    }

    /** A streamer for {@code boolean[]}. */
    public static class Streamer_Booleans implements Streamer<boolean[]> {
        public static final Class<?> TYPE = (new boolean[0]).getClass();
        public Class<?> getObjectClass () {
            return TYPE;
        }
        public void writeObject (Streamable.Output out, boolean[] value) {
            out.writeBooleans(value);
        }
        public boolean[] readObject (Streamable.Input in) {
            return in.readBooleans();
        }
    }

    /** A streamer for {@code byte[]}. */
    public static class Streamer_Bytes implements Streamer<byte[]> {
        public static final Class<?> TYPE = (new byte[0]).getClass();
        public Class<?> getObjectClass () {
            return TYPE;
        }
        public void writeObject (Streamable.Output out, byte[] value) {
            out.writeBytes(value);
        }
        public byte[] readObject (Streamable.Input in) {
            return in.readBytes();
        }
    }

    /** A streamer for {@code char[]}. */
    public static class Streamer_Characters implements Streamer<char[]> {
        public static final Class<?> TYPE = (new char[0]).getClass();
        public Class<?> getObjectClass () {
            return TYPE;
        }
        public void writeObject (Streamable.Output out, char[] value) {
            out.writeChars(value);
        }
        public char[] readObject (Streamable.Input in) {
            return in.readChars();
        }
    }

    /** A streamer for {@code short[]}. */
    public static class Streamer_Shorts implements Streamer<short[]> {
        public static final Class<?> TYPE = (new short[0]).getClass();
        public Class<?> getObjectClass () {
            return TYPE;
        }
        public void writeObject (Streamable.Output out, short[] value) {
            out.writeShorts(value);
        }
        public short[] readObject (Streamable.Input in) {
            return in.readShorts();
        }
    }

    /** A streamer for {@code int[]}. */
    public static class Streamer_Integers implements Streamer<int[]> {
        public static final Class<?> TYPE = (new int[0]).getClass();
        public Class<?> getObjectClass () {
            return TYPE;
        }
        public void writeObject (Streamable.Output out, int[] value) {
            out.writeInts(value);
        }
        public int[] readObject (Streamable.Input in) {
            return in.readInts();
        }
    }

    /** A streamer for {@code long[]}. */
    public static class Streamer_Longs implements Streamer<long[]> {
        public static final Class<?> TYPE = (new long[0]).getClass();
        public Class<?> getObjectClass () {
            return TYPE;
        }
        public void writeObject (Streamable.Output out, long[] value) {
            out.writeLongs(value);
        }
        public long[] readObject (Streamable.Input in) {
            return in.readLongs();
        }
    }

    /** A streamer for {@code float[]}. */
    public static class Streamer_Floats implements Streamer<float[]> {
        public static final Class<?> TYPE = (new float[0]).getClass();
        public Class<?> getObjectClass () {
            return TYPE;
        }
        public void writeObject (Streamable.Output out, float[] value) {
            out.writeFloats(value);
        }
        public float[] readObject (Streamable.Input in) {
            return in.readFloats();
        }
    }

    /** A streamer for {@code double[]}. */
    public static class Streamer_Doubles implements Streamer<double[]> {
        public static final Class<?> TYPE = (new double[0]).getClass();
        public Class<?> getObjectClass () {
            return TYPE;
        }
        public void writeObject (Streamable.Output out, double[] value) {
            out.writeDoubles(value);
        }
        public double[] readObject (Streamable.Input in) {
            return in.readDoubles();
        }
    }

    /** A streamer for {@code String[]}. */
    public static class Streamer_Strings implements Streamer<String[]> {
        public static final Class<?> TYPE = (new String[0]).getClass();
        public Class<?> getObjectClass () {
            return TYPE;
        }
        public void writeObject (Streamable.Output out, String[] value) {
            out.writeStrings(value);
        }
        public String[] readObject (Streamable.Input in) {
            return in.readStrings();
        }
    }

    /** A streamer for {@link List}. */
    public static class Streamer_List<T> implements Streamer<List<T>> {
        public Class<?> getObjectClass () {
            return ArrayList.class; // special handling catches other List impls
        }
        public void writeObject (Streamable.Output out, List<T> values) {
            writeSequence(out, values);
        }
        public List<T> readObject (Streamable.Input in) {
            List<T> list = new ArrayList<T>();
            readSequence(in, list);
            return list;
        }
    }

    /** A streamer for {@link Set}. */
    public static class Streamer_Set<T> implements Streamer<Set<T>> {
        public Class<?> getObjectClass () {
            return HashSet.class; // special handling catches other Set impls
        }
        public void writeObject (Streamable.Output out, Set<T> values) {
            writeSequence(out, values);
        }
        public Set<T> readObject (Streamable.Input in) {
            Set<T> set = new HashSet<T>();
            readSequence(in, set);
            return set;
        }
    }

    /** A streamer for {@link Map}. */
    public static class Streamer_Map<K,V> implements Streamer<Map<K,V>> {
        public Class<?> getObjectClass () {
            return HashMap.class; // special handling catches other Map impls
        }
        public void writeObject (Streamable.Output out, Map<K,V> values) {
            writeSequence(out, values.keySet());
            writeSequence(out, values.values());
        }
        public Map<K,V> readObject (Streamable.Input in) {
            // first read in the keys
            List<K> keys = new ArrayList<K>();
            readSequence(in, keys);
            // then read the values into a collection "view" that adds them to the map
            final Iterator<K> kiter = keys.iterator();
            final Map<K,V> map = new HashMap<K,V>(keys.size());
            readSequence(in, new AbstractCollection<V>() {
                @Override public boolean add (V elem) {
                    map.put(kiter.next(), elem);
                    return true;
                }
                @Override public int size () {
                    return map.size();
                }
                @Override public Iterator<V> iterator () {
                    throw new UnsupportedOperationException();
                }
            });
            return map;
        }
    }

    /** A streamer for {@link Enum} classes. */
    public static class Streamer_Enum<T extends Enum<T>> implements Streamer<T> {
        public Streamer_Enum (Class<T> eclass) {
            _eclass = eclass;
        }
        public Class<?> getObjectClass () {
            return _eclass;
        }
        public void writeObject (Streamable.Output out, T value) {
            out.writeEnum(value);
        }
        public T readObject (Streamable.Input in) {
            return in.readEnum(_eclass);
        }
        protected Class<T> _eclass;
    }

    protected static <T> void writeSequence (Streamable.Output out, Iterable<T> values) {
        Iterator<T> seeker = values.iterator();
        Iterator<T> writer = values.iterator();
        int count = 0;
        Class<?> clazz = Streamers.class; // non-matching sentinel
        while (seeker.hasNext()) {
            T item = seeker.next();
            Class<?> iclazz = (item == null) ? null : item.getClass();
            if (iclazz != clazz) {
                if (count > 0) {
                    out.writeValues(count, writer);
                    count = 0;
                }
                clazz = iclazz;
            }
            count += 1;
        }
        if (count > 0) {
            out.writeValues(count, writer);
        }
        out.writeValues(0, null); // terminator
    }

    protected static <T> void readSequence (Streamable.Input in, Collection<T> into) {
        int size = into.size(), added;
        do {
            in.readValues(into);
            added = into.size() - size;
            size = into.size();
        } while (added > 0);
    }

    private Streamers () {} // no constructsky
}
