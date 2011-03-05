//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
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
    // from interface Nexus
    public void register (NexusObject object)
    {
        _omgr.register(object);
    }

    // from interface Nexus
    public void registerChild (Singleton parent, NexusObject child)
    {
        _omgr.registerChild(parent, child);
    }

    // from interface Nexus
    public void registerChild (Keyed parent, NexusObject child)
    {
        _omgr.registerChild(parent, child);
    }

    // from interface Nexus
    public void registerSingleton (Singleton entity)
    {
        _omgr.registerSingleton(entity);
    }

    // from interface Nexus
    public void registerKeyed (Keyed entity)
    {
        _omgr.registerKeyed(entity);
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

    public NexusServer (NexusConfig config, ExecutorService exec)
    {
        _config = config;
        _omgr = new ObjectManager(config, exec);
    }

    protected final NexusConfig _config;
    protected final ObjectManager _omgr;
}
