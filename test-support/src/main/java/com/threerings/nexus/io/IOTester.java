//
// Nexus Test Support - shared test infrastructure for Nexus framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.io;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper functions for testing basic types.
 */
public class IOTester
{
    public interface Checker {
        void assertEquals (Object expected, Object got);
        void assertEquals (double expected, double got, double epsilon);
    }

    public static final String BT_OUT_PAYLOAD =
        "1|0|-128|0|127|-32768|0|32767|0|48|65535|-2147483648|0|2147483647|IAAAAAAAAAA|" +
        "A|H__________|1.401298464324817E-45|0.0|3.4028234663852886E38|4.9E-324|0.0|" +
        "1.7976931348623157E308|0|1|The quick brown fox jumped over the lazy dog.|";
    public static void writeBasicTypes (Streamable.Output out) {
        out.writeBoolean(true);
        out.writeBoolean(false);
        out.writeByte(Byte.MIN_VALUE);
        out.writeByte((byte)0);
        out.writeByte(Byte.MAX_VALUE);
        out.writeShort(Short.MIN_VALUE);
        out.writeShort((short)0);
        out.writeShort(Short.MAX_VALUE);
        out.writeChar(Character.MIN_VALUE);
        out.writeChar('0');
        out.writeChar(Character.MAX_VALUE);
        out.writeInt(Integer.MIN_VALUE);
        out.writeInt(0);
        out.writeInt(Integer.MAX_VALUE);
        out.writeLong(Long.MIN_VALUE);
        out.writeLong(0L);
        out.writeLong(Long.MAX_VALUE);
        out.writeFloat(Float.MIN_VALUE);
        out.writeFloat(0.0f);
        out.writeFloat(Float.MAX_VALUE);
        out.writeDouble(Double.MIN_VALUE);
        out.writeDouble(0.0);
        out.writeDouble(Double.MAX_VALUE);
        out.writeString(null);
        out.writeString("The quick brown fox jumped over the lazy dog.");
    }

    public static final String BT_IN_PAYLOAD =
        "[1,0,-128,0,127,-32768,0,32767,0,48,65535,-2147483648,0,2147483647,'IAAAAAAAAAA'," +
        "'A','H__________',1.401298464324817E-45,0.0,3.4028234663852886E38,4.9E-324,0.0," +
        "1.7976931348623157E308,null,\"The quick brown fox jumped over the lazy dog.\"]";
    public static void checkBasicTypes (Streamable.Input in, Checker checker) {
        checker.assertEquals(true, in.readBoolean());
        checker.assertEquals(false, in.readBoolean());
        checker.assertEquals(Byte.MIN_VALUE, in.readByte());
        checker.assertEquals((byte)0, in.readByte());
        checker.assertEquals(Byte.MAX_VALUE, in.readByte());
        checker.assertEquals(Short.MIN_VALUE, in.readShort());
        checker.assertEquals((short)0, in.readShort());
        checker.assertEquals(Short.MAX_VALUE, in.readShort());
        checker.assertEquals(Character.MIN_VALUE, in.readChar());
        checker.assertEquals('0', in.readChar());
        checker.assertEquals(Character.MAX_VALUE, in.readChar());
        checker.assertEquals(Integer.MIN_VALUE, in.readInt());
        checker.assertEquals(0, in.readInt());
        checker.assertEquals(Integer.MAX_VALUE, in.readInt());
        checker.assertEquals(Long.MIN_VALUE, in.readLong());
        checker.assertEquals(0L, in.readLong());
        checker.assertEquals(Long.MAX_VALUE, in.readLong());
        checker.assertEquals(Float.MIN_VALUE, in.readFloat(), 0f);
        checker.assertEquals(0.0f, in.readFloat(), 0f);
        checker.assertEquals(Float.MAX_VALUE, in.readFloat(), 0f);
        checker.assertEquals(Double.MIN_VALUE, in.readDouble(), 0.0);
        checker.assertEquals(0.0, in.readDouble(), 0.0);
        checker.assertEquals(Double.MAX_VALUE, in.readDouble(), 0.0);
        checker.assertEquals(null, in.readString());
        checker.assertEquals("The quick brown fox jumped over the lazy dog.", in.readString());
    }

    public static final String VALUE_OUT_PAYLOAD =
        "32|1|RED|1|foo|34|42|32|1|GREEN|1|bar|34|21|32|1|BLUE|1|baz|34|7|";
    public static void writeValue (Streamable.Output out) {
        for (Widget w : Widget.WS) {
            out.writeValue(w);
        }
    }

    public static final String VALUE_IN_PAYLOAD =
        "[32,\"RED\",\"foo\",34,42,32,\"GREEN\",\"bar\",34,21,32,\"BLUE\",\"baz\",34,7]";
    public static void checkValue (Streamable.Input in, Checker checker) {
        for (Widget w : Widget.WS) {
            checker.assertEquals(w, in.<Widget>readValue());
        }
    }

    public static final String VALUES_OUT_PAYLOAD =
        "3|32|1|RED|1|foo|34|42|1|GREEN|1|bar|34|21|1|BLUE|1|baz|34|7|";
    public static void writeValues (Streamable.Output out) {
        out.writeValues(Widget.WS.size(), Widget.WS.iterator());
    }

    public static final String VALUES_IN_PAYLOAD =
        "[3,32,\"RED\",\"foo\",34,42,\"GREEN\",\"bar\",34,21,\"BLUE\",\"baz\",34,7]";
    public static void checkValues (Streamable.Input in, Checker checker) {
        List<Widget> into = new ArrayList<Widget>();
        in.<Widget>readValues(into);
        checker.assertEquals(Widget.WS, into);
    }
}
