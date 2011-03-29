//
// $Id$
//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.distrib;

import com.samskivert.nexus.io.Streamable;
import com.samskivert.nexus.io.Streamer;

/**
 * Handles streaming of {@link TestObject} instances.
 */
public class Streamer_TestObject implements Streamer<TestObject>
{
    public Class<?> getObjectClass ()
    {
        return TestObject.class;
    }

    public void writeObject (Streamable.Output out, TestObject obj)
    {
        out.writeService(TestService.class);
        obj.writeContents(out);
    }

    public TestObject readObject (Streamable.Input in)
    {
        TestObject obj = new TestObject(in.<TestService>readService());
        obj.readContents(in);
        return obj;
    }
}
