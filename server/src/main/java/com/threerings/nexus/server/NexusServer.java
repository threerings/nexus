//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.server;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import react.RMap;
import react.Slot;

import com.threerings.nexus.distrib.Action;
import com.threerings.nexus.distrib.Keyed;
import com.threerings.nexus.distrib.KeyedFactory;
import com.threerings.nexus.distrib.Nexus;
import com.threerings.nexus.distrib.NexusException;
import com.threerings.nexus.distrib.NexusObject;
import com.threerings.nexus.distrib.Request;
import com.threerings.nexus.distrib.ServerNotFoundException;
import com.threerings.nexus.distrib.Singleton;
import static com.threerings.nexus.util.Log.log;

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
    public NexusServer (NexusConfig config, ExecutorService exec) {
        _config = config;
        _omgr = new ObjectManager(config, this, exec);
        _smgr = new SessionManager(_omgr);
    }

    /**
     * Returns the session manager used by this server.
     */
    public SessionManager getSessionManager () {
        return _smgr;
    }

    /**
     * Shuts down this server and cleans up any resources it is using.
     */
    public void shutdown () {
        // nothing for now; TODO: should we shutdown the executor?
    }

    @Override // from interface Nexus
    public void register (NexusObject object) {
        _omgr.register(object);
    }

    @Override // from interface Nexus
    public void register (NexusObject child, Singleton parent) {
        _omgr.register(child, parent);
    }

    @Override // from interface Nexus
    public void register (NexusObject child, Keyed parent) {
        _omgr.register(child, parent);
    }

    @Override // from interface Nexus
    public void registerSingleton (Singleton entity) {
        _omgr.registerSingleton(entity);
    }

    @Override // from interface Nexus
    public void registerSingleton (Singleton child, Singleton parent) {
        _omgr.registerSingleton(child, parent);
    }

    @Override // from interface Nexus
    public void registerKeyed (Keyed entity) {
        _omgr.registerKeyed(entity);
    }

    @Override // from interface Nexus
    public void registerKeyed (Keyed child, Keyed parent) {
        _omgr.registerKeyed(child, parent);
    }

    @Override // from interface Nexus
    public <T extends Keyed> void registerKeyedFactory (Class<T> kclass, KeyedFactory<T> factory) {
        _omgr.registerKeyedFactory(kclass, factory);
    }

    @Override // from interface Nexus
    public <K,V> RMap<K,V> registerMap (String id) {
        return _omgr.registerMap(id);
    }

    @Override // from interface Nexus
    public void clear (NexusObject object) {
        _omgr.clear(object);
    }

    @Override // from interface Nexus
    public void clearSingleton (Singleton entity) {
        _omgr.clearSingleton(entity);
    }

    @Override // from interface Nexus
    public void clearKeyed (Keyed entity) {
        _omgr.clearKeyed(entity);
    }

    @Override // from interface Nexus
    public <T extends Singleton> void assertContext (Class<T> eclass) {
        _omgr.assertContext(eclass);
    }

    @Override // from interface Nexus
    public <T extends Keyed> void assertContext (Class<T> kclass, Comparable<?> key) {
        _omgr.assertContext(kclass, key);
    }

    @Override // from interface Nexus
    public <T extends Singleton> void invoke (Class<T> eclass, Action<? super T> action) {
        _omgr.invoke(eclass, action);
    }

    @Override // from interface Nexus
    public <T extends Keyed> void invoke (Class<T> kclass, Comparable<?> key,
                                          Action<? super T> action) {
        // TODO: determine whether the entity is local or remote
        _omgr.invoke(kclass, key, action);
    }

    @Override @Deprecated
    public <T extends Singleton,R> R invoke (Class<T> eclass, Request<? super T,R> request) {
        return request(eclass, request);
    }

    @Override @Deprecated
    public <T extends Keyed,R> R invoke (Class<T> kclass, Comparable<?> key,
                                         Request<? super T,R> request) {
        return request(kclass, key, request);
    }

    @Override // from interface Nexus
    public <T extends Keyed> void invoke (Class<T> kclass, Set<Comparable<?>> keys,
                                          Action<? super T> action) {
        // TODO: partition keys based on the server that hosts the entities in question; then send
        // one message to each server with the action and the key subset to execute thereon
        for (Comparable<?> key : keys) _omgr.invoke(kclass, key, action);
    }

    @Override // from interface Nexus
    public <T extends Singleton,R> R request (Class<T> eclass, Request<? super T,R> request) {
        return get(request, requestF(eclass, request));
    }

    @Override // from interface Nexus
    public <T extends Singleton,R> Future<R> requestF (Class<T> eclass,
                                                       Request<? super T,R> request) {
        return _omgr.invoke(eclass, request);
    }

    @Override // from interface Nexus
    public <T extends Keyed,R> R request (Class<T> kclass, Comparable<?> key,
                                         Request<? super T,R> request) {
        // TODO: determine whether the entity is local or remote
        return get(request, requestF(kclass, key, request));
    }

    @Override // from interface Nexus
    public <T extends Keyed,R> Future<R> requestF (Class<T> kclass, Comparable<?> key,
                                                   Request<? super T,R> request) {
        // TODO: determine whether the entity is local or remote
        return _omgr.invoke(kclass, key, request);
    }

    @Override // from interface Nexus
    public <T extends Keyed,R> Map<Comparable<?>,R> gather (
        Class<T> kclass, Set<Comparable<?>> keys, Request<? super T,R> request) {
        Map<Comparable<?>,Future<R>> resultFs = gatherF(kclass, keys, request);
        Map<Comparable<?>,R> results = Maps.newHashMap();
        for (Map.Entry<Comparable<?>,Future<R>> entry : resultFs.entrySet()) {
            try {
                results.put(entry.getKey(), get(request, entry.getValue()));
            } catch (Exception e) {
                log.warning("Gather failure", "kclass", kclass.getName(), "key", entry.getKey(), e);
            }
        }
        return results;
    }

    @Override // from interface Nexus
    public <T extends Keyed,R> Map<Comparable<?>,Future<R>> gatherF (
        Class<T> kclass, Set<Comparable<?>> keys, Request<? super T,R> request) {
        // TODO: partition keys based on the server that hosts the entities in question; then send
        // out batched requests to invoke our request on the entities hosted by each server
        Map<Comparable<?>,Future<R>> results = Maps.newHashMap();
        for (Comparable<?> key : keys) {
            if (_omgr.hostsKeyed(kclass, key)) {
                results.put(key, _omgr.invoke(kclass, key, request));
            }
        }
        return results;
    }

    @Override // from interface Nexus
    public <T extends Keyed> Map<Integer,Integer> census (Class<T> kclass) {
        // TODO: proper distributed stuffs
        Map<Integer,Integer> results = Maps.newHashMap();
        results.put(0, _omgr.census(kclass));
        return results;
    }

    @Override // from interface Nexus
    public <T extends Singleton> void invokeOn (
        Class<T> sclass, int serverId, Action<? super T> action) {
        // TODO: proper distributed stuffs
        if (serverId != 0) throw new ServerNotFoundException(serverId);
        invoke(sclass, action);
    }

    @Override // from interface Nexus
    public <T extends Singleton,R> R requestFrom (
        Class<T> sclass, int serverId, Request<? super T,R> request) {
        // TODO: proper distributed stuffs
        if (serverId != 0) throw new ServerNotFoundException(serverId);
        return request(sclass, request);
    }

    @Override // from interface Nexus
    public <T extends Singleton,R> Future<R> requestFromF (
        Class<T> sclass, int serverId, Request<? super T,R> request) {
        // TODO: proper distributed stuffs
        if (serverId != 0) throw new ServerNotFoundException(serverId);
        return requestF(sclass, request);
    }

    @Override // from interface Nexus
    public <T extends Singleton,R> Map<Integer,R> survey (
        Class<T> sclass, Request<? super T,R> request) {
        Map<Integer,Future<R>> resultFs = surveyF(sclass, request);
        Map<Integer,R> results = Maps.newHashMap();
        for (Map.Entry<Integer,Future<R>> entry : resultFs.entrySet()) {
            try {
                results.put(entry.getKey(), get(request, entry.getValue()));
            } catch (Exception e) {
                log.warning("Survey failure", "sclass", sclass.getName(), "id", entry.getKey(), e);
            }
        }
        return results;
    }

    @Override // from interface Nexus
    public <T extends Singleton,R> Map<Integer,Future<R>> surveyF (
        Class<T> sclass, Request<? super T,R> request) {
        // TODO: proper distributed stuffs
        Map<Integer,Future<R>> results = Maps.newHashMap();
        results.put(0, requestF(sclass, request));
        return results;
    }

    protected <R> R get (Request<?,?> request, Future<R> future) {
        try {
            // TODO: should we configure a default timeout?
            return future.get();
        } catch (ExecutionException ee) {
            throw new NexusException("Request failure " + request, ee.getCause());
        } catch (InterruptedException ie) {
            throw new NexusException("Interrupted while waiting for request " + request);
        }
    }

    protected final NexusConfig _config;
    protected final ObjectManager _omgr;
    protected final SessionManager _smgr;
}
