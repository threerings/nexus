//
// Nexus GWTServer - server-side support for Nexus GWT/WebSockets services
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.io;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests the server output and client input pair (in conjunction with ClientInputTest in gwt-io).
 */
public class ServerOutputTest
{
    @Test
    public void testBasicTypes () {
        GWTServerIO.PayloadBuffer buf = new GWTServerIO.PayloadBuffer();
        IOTester.writeBasicTypes(GWTServerIO.newOutput(new TestSerializer(), buf));
        // System.out.println(buf.getPayload()); // for regeneration
        assertEquals(IOTester.BT_IN_PAYLOAD, buf.getPayload());
    }

    @Test
    public void testValueOutput () {
        GWTServerIO.PayloadBuffer buf = new GWTServerIO.PayloadBuffer();
        IOTester.writeValue(GWTServerIO.newOutput(new TestSerializer(), buf));
        // System.out.println(buf.getPayload());
        assertEquals(IOTester.VALUE_IN_PAYLOAD, buf.getPayload());
    }

    @Test
    public void testValuesOutput () {
        GWTServerIO.PayloadBuffer buf = new GWTServerIO.PayloadBuffer();
        IOTester.writeValues(GWTServerIO.newOutput(new TestSerializer(), buf));
        // System.out.println(buf.getPayload());
        assertEquals(IOTester.VALUES_IN_PAYLOAD, buf.getPayload());
    }
}
