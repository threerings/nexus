//
// Nexus Test Support - shared test infrastructure for Nexus framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import com.threerings.nexus.io.ServiceFactory;
import com.threerings.nexus.util.Callback;

/**
 * Creates {@link TestService} marshaller instances.
 */
public class Factory_TestService implements ServiceFactory<TestService>
{
    // from interface ServiceFactory<TestService>
    public DService<TestService> createService ()
    {
        return new Marshaller();
    }

    public static DService<TestService> createDispatcher (final TestService service)
    {
        return new DService.Dispatcher<TestService>() {
            @Override public TestService get () {
                return service;
            }

            @Override public Class<TestService> getServiceClass () {
                return TestService.class;
            }

            @Override public void dispatchCall (short methodId, Object[] args) {
                switch (methodId) {
                case 1:
                    service.addOne((Integer)args[0], this.<Callback<Integer>>cast(args[1]));
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

    protected static class Marshaller extends DService<TestService> implements TestService
    {
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
