//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.distrib;

import com.samskivert.nexus.io.ServiceFactory;
import com.samskivert.nexus.util.Callback;

/**
 * Creates {@link TestService} marshaller instances.
 */
public class Factory_TestService implements ServiceFactory<TestService>
{
    // from interface ServiceFactory<TestService>
    public DService<TestService> createService ()
    {
        return DService.<TestService>create(new Marshaller());
    }

    protected static class Marshaller extends DService.Marshaller implements TestService
    {
        public void addOne (int value, Callback<Integer> callback) {
            postCall((short)1, value, callback);
        }
        public void launchMissiles () {
            postCall((short)2);
        }
    }
}
