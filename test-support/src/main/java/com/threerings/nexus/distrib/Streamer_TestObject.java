//
// Nexus Test Support - shared test infrastructure for Nexus framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import com.threerings.nexus.io.Streamable;
import com.threerings.nexus.io.Streamer;

/**
 * Handles the streaming of {@link TestObject} and/or nested classes.
 */
public class Streamer_TestObject
    implements Streamer<TestObject>
{
    @Override
    public Class<?> getObjectClass () {
        return TestObject.class;
    }

    @Override
    public void writeObject (Streamable.Output out, TestObject obj) {
        writeObjectImpl(out, obj);
        obj.writeContents(out);
    }

    @Override
    public TestObject readObject (Streamable.Input in) {
        TestObject obj = new TestObject(
            in.<TestService>readService()
        );
        obj.readContents(in);
        return obj;
    }

    public static  void writeObjectImpl (Streamable.Output out, TestObject obj) {
        out.writeService(obj.testsvc);
    }
}
