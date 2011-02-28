//
// $Id$

package com.samskivert.nexus.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import com.samskivert.nexus.distrib.DValue;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Tests the JVM (reflection) based Streamable I/O.
 */
public class JVMIOTest
{
    @Test
    public void testPrimitives ()
    {
        final boolean aBoolean = false;
        final byte aByte = 7;
        final char aChar = 'z';
        final short aShort = 42;
        final int anInt = 0xDEADBEEF;
        final long aLong = 0xCAFEBABEDEADBEEFL;
        final float aFloat = 3.14179f;
        final double aDouble = Math.E;
        final String aString = "Hello world!";

        testStreaming(new StreamTester() {
            public void writeTest (Streamable.Output out) {
                out.writeBoolean(aBoolean);
                out.writeByte(aByte);
                out.writeChar(aChar);
                out.writeShort(aShort);
                out.writeInt(anInt);
                out.writeLong(aLong);
                out.writeFloat(aFloat);
                out.writeDouble(aDouble);
                out.writeString(aString);
            }
            public void readTest (Streamable.Input in) {
                assertEquals(aBoolean, in.readBoolean());
                assertEquals(aByte, in.readByte());
                assertEquals(aChar, in.readChar());
                assertEquals(aShort, in.readShort());
                assertEquals(anInt, in.readInt());
                assertEquals(aLong, in.readLong());
                assertEquals(aFloat, in.readFloat(), 0.0);
                assertEquals(aDouble, in.readDouble(), 0.0);
                assertEquals(aString, in.readString());
            }
        });
    }

    @Test
    public void testValues ()
    {
        final Object aNull = null;
        final boolean aBoolean = false;
        final byte aByte = 7;
        final char aChar = 'z';
        final short aShort = 42;
        final int anInt = 0xDEADBEEF;
        final long aLong = 0xCAFEBABEDEADBEEFL;
        final float aFloat = 3.14179f;
        final double aDouble = Math.E;
        final String aString = "Hello world!";

        testStreaming(new StreamTester() {
            public void writeTest (Streamable.Output out) {
                out.writeValue(aNull);
                out.writeValue(aBoolean);
                out.writeValue(aByte);
                out.writeValue(aChar);
                out.writeValue(aShort);
                out.writeValue(anInt);
                out.writeValue(aLong);
                out.writeValue(aFloat);
                out.writeValue(aDouble);
                out.writeValue(aString);
            }
            public void readTest (Streamable.Input in) {
                assertEquals(aNull, in.readValue());
                assertEquals(aBoolean, in.<Boolean>readValue());
                assertEquals(aByte, in.<Byte>readValue().byteValue());
                assertEquals(aChar, in.<Character>readValue().charValue());
                assertEquals(aShort, in.<Short>readValue().shortValue());
                assertEquals(anInt, in.<Integer>readValue().intValue());
                assertEquals(aLong, in.<Long>readValue().longValue());
                assertEquals(aFloat, in.<Float>readValue().floatValue(), 0.0);
                assertEquals(aDouble, in.<Double>readValue().doubleValue(), 0.0);
                assertEquals(aString, in.<String>readValue());
            }
        });
    }

    @Test
    public void testClass ()
    {
        testStreaming(new StreamTester() {
            public void writeTest (Streamable.Output out) {
                out.writeClass(Widget.class);
                out.writeClass(Widget.Wangle.class);
            }
            public void readTest (Streamable.Input in) {
                assertEquals(Widget.class, in.readClass());
                assertEquals(Widget.Wangle.class, in.readClass());
            }
        });
    }

    @Test
    public void testStreaming ()
    {
        final Widget w = new Widget("foo", new Widget.Wangle(42));
        testStreaming(new StreamTester() {
            public void writeTest (Streamable.Output out) {
                out.writeStreamable(w);
            }
            public void readTest (Streamable.Input in) {
                assertEquals(w, in.<Widget>readStreamable());
            }
        });
    }

    protected void testStreaming (StreamTester tester)
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Streamable.Output sout = JVMIO.newOutput(out);
        tester.writeTest(sout);
        tester.readTest(JVMIO.newInput(new ByteArrayInputStream(out.toByteArray())));
    }

    protected interface StreamTester {
        void writeTest (Streamable.Output out);
        void readTest (Streamable.Input in);
    }
}
