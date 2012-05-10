//
// Nexus JVMIO - I/O and network services for Nexus built on java.nio
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.threerings.nexus.distrib.DValue;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Tests the JVM (reflection) based Streamable I/O.
 */
public class JVMIOTest
{
    public static enum TestEnum { FOO, BAR, BAZ };

    @Test
    public void testPrimitives () {
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
    public void testValues () {
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
        final TestEnum anEnum = TestEnum.BAR;

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
                out.writeValue(anEnum);
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
                assertEquals(anEnum, in.<TestEnum>readValue());
            }
        });
    }

    @Test
    public void testWriteValue () {
        testStreaming(new StreamTester() {
            public void writeTest (Streamable.Output out) {
                for (Widget w : Widget.WS) {
                    out.writeValue(w);
                }
            }
            public void readTest (Streamable.Input in) {
                for (Widget w : Widget.WS) {
                    assertEquals(w, in.<Widget>readValue());
                }
            }
        });
    }

    @Test
    public void testWriteValues () {
        testStreaming(new StreamTester() {
            public void writeTest (Streamable.Output out) {
                out.writeValues(Widget.WS.size(), Widget.WS.iterator());
            }
            public void readTest (Streamable.Input in) {
                List<Widget> into = new ArrayList<Widget>();
                in.<Widget>readValues(into);
                assertEquals(Widget.WS, into);
            }
        });
    }

    @Test
    public void testManualStreamer () {
        final Thunk t1 = new Thunk(42), t2 = new Thunk(0xDEADBEEF);
        testStreaming(new StreamTester() {
            public void writeTest (Streamable.Output out) {
                out.writeValue(t1);
                out.writeValue(t2);
            }
            public void readTest (Streamable.Input in) {
                assertEquals(t1, in.<Thunk>readValue());
                assertEquals(t2, in.<Thunk>readValue());
            }
        });
    }

    protected void testStreaming (StreamTester tester) {
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
