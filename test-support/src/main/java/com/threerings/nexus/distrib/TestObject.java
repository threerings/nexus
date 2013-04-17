//
// Nexus Test Support - shared test infrastructure for Nexus framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

/**
 * An object used for simple tests.
 */
public class TestObject extends NexusObject
    implements Singleton
{
    public final DValue<String> value = DValue.create(this, "test");

    public final DService<TestService> testsvc;

    public TestObject (DService.Factory<TestService> testsvc) {
        this.testsvc = testsvc.createService(this);
    }
}
