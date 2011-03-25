//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.distrib;

import com.samskivert.nexus.io.Streamable;

/**
 * An attribute that contains a Nexus service reference.
 */
public class DService<T extends NexusService> extends DAttribute
{
    /** An implementation detail used by service marshallers. */
    public static class Marshaller {
        /** The attribute that holds this service marshaller reference. */
        public DService<?> attr;

        protected void postCall (short methodId, Object... args) {
            attr._owner.postCall(attr._index, methodId, args);
        }
    }

    /** The service encapsulated by this attribute. */
    public final T svc;

    /**
     * Creates a service attribute with the supplied underlying service.
     */
    public static <T extends NexusService> DService<T> create (T service)
    {
        return new DService<T>(service);
    }

    @Override // from DAttribute
    public void readContents (Streamable.Input in)
    {
        // NOOP
    }

    @Override // from DAttribute
    public void writeContents (Streamable.Output out)
    {
        // NOOP
    }

    protected DService (T service)
    {
        this.svc = service;

        // tie the gordian knot
        if (service instanceof Marshaller) {
            ((Marshaller)service).attr = this;
        }
    }
}
