//
// $Id$
//
// Nexus GWTIO - I/O and network services for Nexus built on GWT and WebSockets
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gwt.junit.client.GWTTestCase;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Tests the client input and server output pair (in conjunction with ServerOutputTest in
 * gwt-server), and the client output and server input pair (in conjunction with ServerInputTest in
 * gwt-server). These are combined into one test to mitigate the massive 15-20 second overhead of
 * running a GWT test (yay!).
 */
public class GWTIOTest extends GWTTestCase
{
    public String getModuleName ()
    {
        return "com.samskivert.nexus.GWTIO";
    }

    // NOTE: changes to these tests should be mirrored in Server{Input|Output}Test in gwt-server

    @Test
    public void testBasicTypesInput ()
    {
        final String PAYLOAD =
            "[1,0,-128,0,127,-32768,0,32767,0,48,65535,-2147483648,0,2147483647,'IAAAAAAAAAA'," +
            "'A','H__________',1.401298464324817E-45,0.0,3.4028234663852886E38,4.9E-324,0.0," +
            "1.7976931348623157E308,null,\"The quick brown fox jumped over the lazy dog.\"]";

        Streamable.Input in = GWTIO.newInput(new TestSerializer(), PAYLOAD);
        assertEquals(true, in.readBoolean());
        assertEquals(false, in.readBoolean());
        assertEquals(Byte.MIN_VALUE, in.readByte());
        assertEquals((byte)0, in.readByte());
        assertEquals(Byte.MAX_VALUE, in.readByte());
        assertEquals(Short.MIN_VALUE, in.readShort());
        assertEquals((short)0, in.readShort());
        assertEquals(Short.MAX_VALUE, in.readShort());
        assertEquals(Character.MIN_VALUE, in.readChar());
        assertEquals('0', in.readChar());
        assertEquals(Character.MAX_VALUE, in.readChar());
        assertEquals(Integer.MIN_VALUE, in.readInt());
        assertEquals(0, in.readInt());
        assertEquals(Integer.MAX_VALUE, in.readInt());
        assertEquals(Long.MIN_VALUE, in.readLong());
        assertEquals(0L, in.readLong());
        assertEquals(Long.MAX_VALUE, in.readLong());
        assertEquals(Float.MIN_VALUE, in.readFloat(), 0f);
        assertEquals(0.0f, in.readFloat(), 0f);
        assertEquals(Float.MAX_VALUE, in.readFloat(), 0f);
        assertEquals(Double.MIN_VALUE, in.readDouble(), 0.0);
        assertEquals(0.0, in.readDouble(), 0.0);
        assertEquals(Double.MAX_VALUE, in.readDouble(), 0.0);
        assertEquals(null, in.readString());
        assertEquals("The quick brown fox jumped over the lazy dog.", in.readString());
    }

    @Test
    public void testBasicTypesOutput ()
    {
        StringBuffer buf = new StringBuffer();
        Streamable.Output out = GWTIO.newOutput(new TestSerializer(), buf);
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
        // System.out.println(buf); // for regeneration

        final String PAYLOAD =
            "1|0|-128|0|127|-32768|0|32767|0|48|65535|-2147483648|0|2147483647|IAAAAAAAAAA|" +
            "A|H__________|1.401298464324817E-45|0.0|3.4028234663852886E38|4.9E-324|0.0|" +
            "1.7976931348623157E308|0|1|The quick brown fox jumped over the lazy dog.|";
        assertEquals(PAYLOAD, buf.toString());
    }

    @Test
    public void testValueInput ()
    {
        final String PAYLOAD = "[13,\"foo\",14,42,13,\"bar\",14,21,13,\"baz\",14,7]";
        Streamable.Input in = GWTIO.newInput(new TestSerializer(), PAYLOAD);
        for (Widget w : WS) {
            assertEquals(w, in.<Widget>readValue());
        }
    }

    @Test
    public void testValueOutput ()
    {
        StringBuffer buf = new StringBuffer();
        Streamable.Output out = GWTIO.newOutput(new TestSerializer(), buf);
        for (Widget w : WS) {
            out.writeValue(w);
        }
        // System.out.println(buf);

        final String PAYLOAD = "13|1|foo|14|42|13|1|bar|14|21|13|1|baz|14|7|";
        assertEquals(PAYLOAD, buf.toString());
    }

    @Test
    public void testValuesInput ()
    {
        final String PAYLOAD = "[3,13,\"foo\",14,42,\"bar\",14,21,\"baz\",14,7]";
        Streamable.Input in = GWTIO.newInput(new TestSerializer(), PAYLOAD);
        List<Widget> into = new ArrayList<Widget>();
        in.<Widget>readValues(into);
        assertEquals(WS, into);
    }

    @Test
    public void testValuesOutput ()
    {
        StringBuffer buf = new StringBuffer();
        Streamable.Output out = GWTIO.newOutput(new TestSerializer(), buf);
        out.writeValues(WS.size(), WS.iterator());
        // System.out.println(buf);

        final String PAYLOAD = "3|13|1|foo|14|42|1|bar|14|21|1|baz|14|7|";
        assertEquals(PAYLOAD, buf.toString());
    }

    protected static final List<Widget> WS = Arrays.asList(
        new Widget("foo", new Widget.Wangle(42)),
        new Widget("bar", new Widget.Wangle(21)),
        new Widget("baz", new Widget.Wangle(7)));
}
