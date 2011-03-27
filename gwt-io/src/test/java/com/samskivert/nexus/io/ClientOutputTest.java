//
// $Id$

package com.samskivert.nexus.io;

import com.google.gwt.junit.client.GWTTestCase;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Tests our {@link ClientOutput} class.
 */
public class ClientOutputTest extends GWTTestCase
{
    public String getModuleName ()
    {
        return "com.samskivert.nexus.GWTIO";
    }

    @Test
    public void testBasicTypes ()
    {
        StringBuffer buf = new StringBuffer();
        Streamable.Output out = GWTIO.newOutput(new TestSerializer(), buf);
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
        // System.out.println(buf); // for regeneration
        assertEquals(BASIC_TYPES_PAYLOAD, buf.toString());
    }

    protected static final String BASIC_TYPES_PAYLOAD =
        "1|0|-128|0|127|-32768|0|32767|0|48|65535|-2147483648|0|2147483647|IAAAAAAAAAA|" +
        "A|H__________|1.401298464324817E-45|0.0|3.4028234663852886E38|4.9E-324|0.0|" +
        "1.7976931348623157E308|0|1|The quick brown fox jumped over the lazy dog.|";
}
