//
// Nexus GWTIO - I/O and network services for Nexus built on GWT and WebSockets
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.io;

import org.junit.Test;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests the client input and server output pair (in conjunction with ServerOutputTest in
 * gwt-server), and the client output and server input pair (in conjunction with ServerInputTest in
 * gwt-server). These are combined into one test to mitigate the massive 15-20 second overhead of
 * running a GWT test (yay!).
 */
public class GwtTestIO extends GWTTestCase
{
    @Override public String getModuleName () {
        return "com.threerings.nexus.GWTIO";
    }

    @Test
    public void testBasicTypesInput () {
        IOTester.checkBasicTypes(
            GWTIO.newInput(new TestSerializer(), IOTester.BT_IN_PAYLOAD), CHECKER);
    }

    @Test
    public void testBasicTypesOutput () {
        StringBuffer buf = new StringBuffer();
        IOTester.writeBasicTypes(GWTIO.newOutput(new TestSerializer(), buf));
        // System.out.println(buf); // for regeneration
        assertEquals(IOTester.BT_OUT_PAYLOAD, buf.toString());
    }

    @Test
    public void testValueInput () {
        IOTester.checkValue(
            GWTIO.newInput(new TestSerializer(), IOTester.VALUE_IN_PAYLOAD), CHECKER);
    }

    @Test
    public void testValueOutput () {
        StringBuffer buf = new StringBuffer();
        IOTester.writeValue(GWTIO.newOutput(new TestSerializer(), buf));
        // System.out.println(buf);
        assertEquals(IOTester.VALUE_OUT_PAYLOAD, buf.toString());
    }

    @Test
    public void testValuesInput () {
        IOTester.checkValues(
            GWTIO.newInput(new TestSerializer(), IOTester.VALUES_IN_PAYLOAD), CHECKER);
    }

    @Test
    public void testValuesOutput () {
        StringBuffer buf = new StringBuffer();
        IOTester.writeValues(GWTIO.newOutput(new TestSerializer(), buf));
        // System.out.println(buf);
        assertEquals(IOTester.VALUES_OUT_PAYLOAD, buf.toString());
    }

    protected final IOTester.Checker CHECKER = new IOTester.Checker() {
        public void assertEquals (Object expected, Object got) {
            GwtTestIO.assertEquals(expected, got);
        }
        public void assertEquals (double expected, double got, double epsilon) {
            GwtTestIO.assertEquals(expected, got, epsilon);
        }
    };
}
