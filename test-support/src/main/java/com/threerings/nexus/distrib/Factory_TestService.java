//
// Nexus Test Support - shared test infrastructure for Nexus framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import com.threerings.nexus.util.Callback;

/**
 * Creates {@link TestService} marshaller instances.
 */
public class Factory_TestService implements DService.Factory<TestService>
{
    @Override
    public DService<TestService> createService (NexusObject owner)
    {
        return new Marshaller(owner);
    }

    public static DService.Factory<TestService> createDispatcher (final TestService service)
    {
        return new DService.Factory<TestService>() {
            public DService<TestService> createService (NexusObject owner) {
                return new DService.Dispatcher<TestService>(owner) {
                    @Override public TestService get () {
                        return service;
                    }

                    @Override public Class<TestService> getServiceClass () {
                        return TestService.class;
                    }

                    @Override public void dispatchCall (short methodId, Object[] args) {
                        switch (methodId) {
                        case 1:
                            service.addOne(
                                this.<Integer>cast(args[0]),
                                this.<Callback<Integer>>cast(args[1]));
                            break;
                        case 2:
                            service.launchMissiles();
                            break;
                        default:
                            super.dispatchCall(methodId, args);
                        }
                    }
                };
            }
        };
    }

    protected static class Marshaller extends DService<TestService> implements TestService
    {
        public Marshaller (NexusObject owner) {
            super(owner);
        }
        @Override public TestService get () {
            return this;
        }
        @Override public Class<TestService> getServiceClass () {
            return TestService.class;
        }
        @Override public void addOne (int value, Callback<Integer> callback) {
            postCall((short)1, value, callback);
        }
        @Override public void launchMissiles () {
            postCall((short)2);
        }
    }
}
