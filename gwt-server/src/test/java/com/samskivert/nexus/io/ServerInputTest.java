//
// $Id$
//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Tests the client output and server input pair (in conjunction with ClientOutputTest in gwt-io).
 */
public class ServerInputTest
{
    @Test
    public void testBasicTypes ()
    {
        final String PAYLOAD =
            "1|0|-128|0|127|-32768|0|32767|0|48|65535|-2147483648|0|2147483647|IAAAAAAAAAA|" +
            "A|H__________|1.401298464324817E-45|0.0|3.4028234663852886E38|4.9E-324|0.0|" +
            "1.7976931348623157E308|0|1|The quick brown fox jumped over the lazy dog.|";
        Streamable.Input in = GWTServerIO.newInput(new TestSerializer(), PAYLOAD);
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
    public void testValueInput ()
    {
        // final String PAYLOAD = "13|1|foo|14|42|13|1|bar|14|21|13|1|baz|14|7|";
        final String PAYLOAD = "28|1|foo|29|42|28|1|bar|29|21|28|1|baz|29|7|";
        Streamable.Input in = GWTServerIO.newInput(new TestSerializer(), PAYLOAD);
        for (Widget w : WS) {
            assertEquals(w, in.<Widget>readValue());
        }
    }

    @Test
    public void testValuesInput ()
    {
        // final String PAYLOAD = "3|13|1|foo|14|42|1|bar|14|21|1|baz|14|7|";
        final String PAYLOAD = "3|28|1|foo|29|42|1|bar|29|21|1|baz|29|7|";
        Streamable.Input in = GWTServerIO.newInput(new TestSerializer(), PAYLOAD);
        List<Widget> into = new ArrayList<Widget>();
        in.<Widget>readValues(into);
        assertEquals(WS, into);
    }

    protected static final List<Widget> WS = Arrays.asList(
        new Widget("foo", new Widget.Wangle(42)),
        new Widget("bar", new Widget.Wangle(21)),
        new Widget("baz", new Widget.Wangle(7)));
}
