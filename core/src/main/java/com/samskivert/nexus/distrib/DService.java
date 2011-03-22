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
    /** Used on the client to post service calls to the server. */
    public abstract static class Poster {
        public void postCall (short methodId, Object... args) {
            _attr.postCall(methodId, args);
        }
        protected Poster (DService<?> attr) {
            _attr = attr;
        }
        protected DService<?> _attr;
    }

    /** Used on the server to dispatch service requests. */
    public interface Dispatcher {
        /** Dispatches the specified service method using the supplied arguments. */
        void dispatchCall (short methodId, Object[] args);
    }

    /**
     * Returns the service, which can be called.
     */
    public T get ()
    {
        return _service;
    }

    @Override // from DAttribute
    public void readContents (Streamable.Input in)
    {
        // TODO: _service = in.<T>readService();
    }

    @Override // from DAttribute
    public void writeContents (Streamable.Output out)
    {
        // TODO: out.writeService(_service);
    }

    protected DService (T service)
    {
        _service = service;
    }

    protected void postCall (short methodId, Object[] args)
    {
        _owner.postCall(_index, methodId, args);
    }

    protected void dispatchCall (short methodId, Object[] args)
    {
        _dispatcher.dispatchCall(methodId, args);
    }

    protected T _service;
    protected Dispatcher _dispatcher;
}
