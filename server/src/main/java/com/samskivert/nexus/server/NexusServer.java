//
// $Id$
//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.server;

import java.util.concurrent.ExecutorService;

import com.samskivert.nexus.distrib.Action;
import com.samskivert.nexus.distrib.Keyed;
import com.samskivert.nexus.distrib.Nexus;
import com.samskivert.nexus.distrib.NexusObject;
import com.samskivert.nexus.distrib.Request;
import com.samskivert.nexus.distrib.Singleton;

/**
 * Implements the Nexus services and coordinates communication between nodes.
 */
public class NexusServer implements Nexus
{
    /**
     * Creates a server with the supplied configuration.
     *
     * @param exec the executor to use for dispatching events and actions. An executor that uses a
     * pool of threads is appropriate.
     */
    public NexusServer (NexusConfig config, ExecutorService exec)
    {
        _config = config;
        _omgr = new ObjectManager(config, exec);
        _smgr = new SessionManager(_omgr);
    }

    /**
     * Returns the session manager used by this server.
     */
    public SessionManager getSessionManager ()
    {
        return _smgr;
    }

    // from interface Nexus
    public void register (NexusObject object)
    {
        _omgr.register(object);
    }

    // from interface Nexus
    public void register (NexusObject child, Singleton parent)
    {
        _omgr.register(child, parent);
    }

    // from interface Nexus
    public void register (NexusObject child, Keyed parent)
    {
        _omgr.register(child, parent);
    }

    // from interface Nexus
    public void registerSingleton (Singleton entity)
    {
        _omgr.registerSingleton(entity);
    }

    // from interface Nexus
    public void registerSingleton (Singleton child, Singleton parent)
    {
        _omgr.registerSingleton(child, parent);
    }

    // from interface Nexus
    public void registerKeyed (Keyed entity)
    {
        _omgr.registerKeyed(entity);
    }

    // from interface Nexus
    public void registerKeyed (Keyed child, Keyed parent)
    {
        _omgr.registerKeyed(child, parent);
    }

    // from interface Nexus
    public void clear (NexusObject object)
    {
        _omgr.clear(object);
    }

    // from interface Nexus
    public void clearSingleton (Singleton entity)
    {
        _omgr.clearSingleton(entity);
    }

    // from interface Nexus
    public void clearKeyed (Keyed entity)
    {
        _omgr.clearKeyed(entity);
    }

    // from interface Nexus
    public <T extends Singleton> void invoke (Class<T> eclass, Action<T> action)
    {
        _omgr.invoke(eclass, action);
    }

    // from interface Nexus
    public <T extends Keyed> void invoke (Class<T> kclass, Comparable<?> key, Action<T> action)
    {
        // TODO: determine whether the entity is local or remote
        _omgr.invoke(kclass, key, action);
    }

    // from interface Nexus
    public <T extends Singleton,R> R invoke (Class<T> eclass, Request<T,R> request)
    {
        return _omgr.invoke(eclass, request);
    }

    // from interface Nexus
    public <T extends Keyed,R> R invoke (Class<T> kclass, Comparable<?> key, Request<T,R> request)
    {
        // TODO: determine whether the entity is local or remote
        return _omgr.invoke(kclass, key, request);
    }

    protected final NexusConfig _config;
    protected final ObjectManager _omgr;
    protected final SessionManager _smgr;
}
