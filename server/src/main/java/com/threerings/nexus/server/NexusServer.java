//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.server;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.google.common.collect.Maps;

import react.RMap;

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
    public <N extends NexusObject> Context<N> register (N object) {
        return _omgr.register(object);
    }

    @Override // from interface Nexus
    public void register (NexusObject child, Context<?> parent) {
        _omgr.register(child, parent);
    }

    @Override // from interface Nexus
    public <S extends Singleton> Context<S> register (Class<? super S> sclass, S entity) {
        return _omgr.register(sclass, entity);
    }

    @Override // from interface Nexus
    public <S extends Singleton> void register (Class<? super S> sclass, S child,
                                                Context<?> parent) {
        _omgr.register(sclass, child, parent);
    }

    @Override // from interface Nexus
    public <K extends Keyed> Context<K> registerKeyed (Class<? super K> kclass, K entity) {
        return _omgr.registerKeyed(kclass, entity);
    }

    @Override // from interface Nexus
    public <K extends Keyed> void registerKeyed (Class<? super K> kclass, K child,
                                                 Context<?> parent) {
        _omgr.registerKeyed(kclass, child, parent);
    }

    @Override // from interface Nexus
    public <K extends Keyed> void registerKeyedFactory (Class<? super K> kclass,
                                                        KeyedFactory<K> kf) {
        _omgr.registerKeyedFactory(kclass, kf);
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
    public <S extends Singleton> void clear (Class<? super S> sclass, S entity) {
        _omgr.clear(sclass, entity);
    }

    @Override // from interface Nexus
    public <K extends Keyed> void clearKeyed (Class<? super K> kclass, K entity) {
        _omgr.clearKeyed(kclass, entity);
    }

    @Override // from interface Nexus
    public <K extends Keyed> Set<Comparable<?>> hostedKeys (Class<K> kclass) {
        return _omgr.hostedKeys(kclass);
    }

    @Override // from interface Nexus
    public <S extends Singleton> void assertContext (Class<S> sclass) {
        _omgr.assertContext(sclass);
    }

    @Override // from interface Nexus
    public <K extends Keyed> void assertContext (Class<K> kclass, Comparable<?> key) {
        _omgr.assertContext(kclass, key);
    }

    @Override // from interface Nexus
    public <S extends Singleton> void invoke (Class<S> sclass, Action<? super S> action) {
        _omgr.invoke(sclass, action);
    }

    @Override // from interface Nexus
    public <K extends Keyed> void invoke (Class<K> kclass, Comparable<?> key,
                                          Action<? super K> action) {
        // TODO: determine whether the entity is local or remote
        _omgr.invoke(kclass, key, action);
    }

    @Override @Deprecated
    public <T extends Singleton,R> R invoke (Class<T> sclass, Request<? super T,R> request) {
        return request(sclass, request);
    }

    @Override @Deprecated
    public <T extends Keyed,R> R invoke (Class<T> kclass, Comparable<?> key,
                                         Request<? super T,R> request) {
        return request(kclass, key, request);
    }

    @Override // from interface Nexus
    public <S extends Singleton,R> R request (Class<S> sclass, Request<? super S,R> request) {
        return get(request, requestF(sclass, request));
    }

    @Override // from interface Nexus
    public <S extends Singleton,R> Future<R> requestF (Class<S> sclass,
                                                       Request<? super S,R> request) {
        return _omgr.invoke(sclass, request);
    }

    @Override // from interface Nexus
    public <K extends Keyed,R> R request (Class<K> kclass, Comparable<?> key,
                                         Request<? super K,R> request) {
        // TODO: determine whether the entity is local or remote
        return get(request, requestF(kclass, key, request));
    }

    @Override // from interface Nexus
    public <K extends Keyed,R> Future<R> requestF (Class<K> kclass, Comparable<?> key,
                                                   Request<? super K,R> request) {
        // TODO: determine whether the entity is local or remote
        return _omgr.invoke(kclass, key, request);
    }

    @Override
    public <K extends Keyed> int nextId (Class<K> kclass) {
        // TODO: get our real server id from the peer manager
        return _omgr.nextId(0, MAX_SERVERS, kclass);
    }

    @Override // from interface Nexus
    public <K extends Keyed> Map<Integer,Integer> census (Class<K> kclass) {
        // TODO: proper distributed stuffs
        Map<Integer,Integer> results = Maps.newHashMap();
        results.put(0, _omgr.census(kclass));
        return results;
    }

    @Override // from interface Nexus
    public <K extends Keyed> void invoke (Class<K> kclass, Set<? extends Comparable<?>> keys,
                                          Action<? super K> action) {
        // TODO: partition keys based on the server that hosts the entities in question; then send
        // one message to each server with the action and the key subset to execute thereon
        for (Comparable<?> key : keys) _omgr.invoke(kclass, key, action);
    }

    @Override // from interface Nexus
    public <K extends Keyed,R> Map<Comparable<?>,R> gather (
        Class<K> kclass, Set<? extends Comparable<?>> keys, Request<? super K,R> request) {
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
    public <K extends Keyed,R> Map<Comparable<?>,Future<R>> gatherF (
        Class<K> kclass, Set<? extends Comparable<?>> keys, Request<? super K,R> request) {
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
    public <S extends Singleton> void invokeOn (
        Class<S> sclass, int serverId, Action<? super S> action) {
        // TODO: proper distributed stuffs
        if (serverId != 0) throw new ServerNotFoundException(serverId);
        invoke(sclass, action);
    }

    @Override // from interface Nexus
    public <S extends Singleton,R> R requestFrom (
        Class<S> sclass, int serverId, Request<? super S,R> request) {
        // TODO: proper distributed stuffs
        if (serverId != 0) throw new ServerNotFoundException(serverId);
        return request(sclass, request);
    }

    @Override // from interface Nexus
    public <S extends Singleton,R> Future<R> requestFromF (
        Class<S> sclass, int serverId, Request<? super S,R> request) {
        // TODO: proper distributed stuffs
        if (serverId != 0) throw new ServerNotFoundException(serverId);
        return requestF(sclass, request);
    }

    @Override
    public <S extends Singleton> void broadcast (Class<S> sclass, Action<? super S> action) {
        // TODO: proper distributed stuffs
        invoke(sclass, action);
    }

    @Override // from interface Nexus
    public <S extends Singleton,R> Map<Integer,R> survey (
        Class<S> sclass, Request<? super S,R> request) {
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
    public <S extends Singleton,R> Map<Integer,Future<R>> surveyF (
        Class<S> sclass, Request<? super S,R> request) {
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
            Throwable cause = ee.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException)cause;
            else throw new NexusException("Request failure " + request, cause);
        } catch (InterruptedException ie) {
            throw new NexusException("Interrupted while waiting for request " + request);
        }
    }

    protected final NexusConfig _config;
    protected final ObjectManager _omgr;
    protected final SessionManager _smgr;

    protected static final int MAX_SERVERS = 1000; // see nextId()
}
