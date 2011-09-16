//
// Nexus GWTServer - server-side support for Nexus GWT/WebSockets services
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.io;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Tests the server output and client input pair (in conjunction with ClientInputTest in gwt-io).
 */
public class ServerOutputTest
{
    @Test
    public void testBasicTypes () {
        GWTServerIO.PayloadBuffer buf = new GWTServerIO.PayloadBuffer();
        Streamable.Output out = GWTServerIO.newOutput(new TestSerializer(), buf);
        // note: additions to this test should be mirrored in ServerInputTest in nexus-server
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
        // System.out.println(buf.getPayload()); // for regeneration

        final String PAYLOAD =
            "[1,0,-128,0,127,-32768,0,32767,0,48,65535,-2147483648,0,2147483647,'IAAAAAAAAAA'," +
            "'A','H__________',1.401298464324817E-45,0.0,3.4028234663852886E38,4.9E-324,0.0," +
            "1.7976931348623157E308,null,\"The quick brown fox jumped over the lazy dog.\"]";
        assertEquals(PAYLOAD, buf.getPayload());
    }

    @Test
    public void testValueOutput () {
        GWTServerIO.PayloadBuffer buf = new GWTServerIO.PayloadBuffer();
        Streamable.Output out = GWTServerIO.newOutput(new TestSerializer(), buf);
        for (Widget w : Widget.WS) {
            out.writeValue(w);
        }
        // System.out.println(buf.getPayload());

        final String PAYLOAD =
            "[31,\"RED\",\"foo\",33,42,31,\"GREEN\",\"bar\",33,21,31,\"BLUE\",\"baz\",33,7]";
        assertEquals(PAYLOAD, buf.getPayload());
    }

    @Test
    public void testValuesOutput () {
        GWTServerIO.PayloadBuffer buf = new GWTServerIO.PayloadBuffer();
        Streamable.Output out = GWTServerIO.newOutput(new TestSerializer(), buf);
        out.writeValues(Widget.WS.size(), Widget.WS.iterator());
        // System.out.println(buf.getPayload());

        final String PAYLOAD =
            "[3,31,\"RED\",\"foo\",33,42,\"GREEN\",\"bar\",33,21,\"BLUE\",\"baz\",33,7]";
        assertEquals(PAYLOAD, buf.getPayload());
    }
}
