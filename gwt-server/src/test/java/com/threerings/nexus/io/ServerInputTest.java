//
// Nexus GWTServer - server-side support for Nexus GWT/WebSockets services
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.io;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the client output and server input pair (in conjunction with GwtTestIO in gwt-io).
 */
public class ServerInputTest
{
    @Test
    public void testBasicTypes () {
        IOTester.checkBasicTypes(
            GWTServerIO.newInput(new TestSerializer(), IOTester.BT_OUT_PAYLOAD), CHECKER);
    }

    @Test
    public void testValueInput () {
        IOTester.checkValue(
            GWTServerIO.newInput(new TestSerializer(), IOTester.VALUE_OUT_PAYLOAD), CHECKER);
    }

    @Test
    public void testValuesInput () {
        IOTester.checkValues(
            GWTServerIO.newInput(new TestSerializer(), IOTester.VALUES_OUT_PAYLOAD), CHECKER);
    }

    protected final IOTester.Checker CHECKER = new IOTester.Checker() {
        public void assertEquals (Object expected, Object got) {
            Assert.assertEquals(expected, got);
        }
        public void assertEquals (double expected, double got, double epsilon) {
            Assert.assertEquals(expected, got, epsilon);
        }
    };
}
