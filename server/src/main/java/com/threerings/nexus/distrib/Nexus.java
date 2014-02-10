//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import react.RMap;

/**
 * The main source for Nexus services. See
 * <a href="https://github.com/threerings/nexus/wiki/ServerConcepts">this documentation</a> for a
 * detailed explanation of entities and execution contexts.
 */
public interface Nexus
{
    /**
     * Provides direct access to the execution context of an entity that resides on this node. One
     * can use this to invoke actions and requests on an entity's context without having to look
     * the entity up using its class token (and key in the case of keyed entities). A context is
     * also used when registering an entity as a child of another entity, such that the child
     * entity's actions and requests are processed in the parent entity's context.
     */
    interface Context<E> {
        /** Invokes an action in this entity context. */
        void invoke (Action<? super E> action);

        /** Invokes a request in this entity context.
         * @return a future which will eventually contain the result, or failure. */
        <R> Future<R> requestF (Request<? super E,R> request);

        /** Invokes a request in this entity context. */
        <R> R request (Request<? super E,R> request);
    }

    /**
     * Registers an anonymous object with the Nexus in its own execution context.
     *
     * @return the exection context created for the registered object.
     */
    <N extends NexusObject> Context<N> register (N object);

    /**
     * Registers an object as a child of the specified parent entity. The child will share the
     * parent's execution context rather than having an execution context of its own.
     *
     * @param parent the context of the parent entity, obtained via {@link #resolve(Class)} or
     * {@link #resolve(Class,Comparable)}.
     */
    void register (NexusObject child, Context<?> parent);

    /**
     * Registers a singleton (object or non-object) entity with the Nexus. This entity will only be
     * accessible on the server node on which it was created. Code may be executed in this entity's
     * context via, for example, {@link #invoke(Class,Action)}. If the singleton is also a {@link
     * NexusObject}, clients can subscribe to the object using its class.
     *
     * @param sclass the class token used to identify this singleton. This will usually be the
     * singleton's class, but could be a superclass if the caller desires. This <em>same</em> class
     * token must be used when calling {@link #invoke(Class,Action)}.
     * @param entity the singleton entity to be registered.
     *
     * @return the execution context created for the registered singleton.
     * @throws NexusException if an entity is already mapped for this singleton type.
     */
    <S extends Singleton> Context<S> register (Class<? super S> sclass, S entity);

    /**
     * Registers a singleton as a child of the specified parent entity. The child will share the
     * parent's execution context rather than having an execution context of its own.
     *
     * @param sclass the class token used to identify this singleton. This will usually be the
     * singleton's class, but could be a superclass if the caller desires. This <em>same</em> class
     * token must be used when calling {@link #invoke(Class,Action)}.
     * @param child the singleton entity to be registered.
     * @param parent the context of the parent entity, obtained via {@link #resolve(Class)} or
     * {@link #resolve(Class,Comparable)}.
     */
    <S extends Singleton> void register (Class<? super S> cclass, S child, Context<?> parent);

    /**
     * Registers a keyed (object or non-object) entity with the Nexus. This entity will be
     * accessible to all server nodes in the Nexus. Code may be executed in this entity's context
     * (server+thread) via, for example, {@link #invoke(Class,Comparable,Action)}. If the keyed is
     * also a {@link NexusObject}, clients can subscribe to the object using its class and key.
     *
     * @param kclass the class token used to identify this keyed entity. This will usually be the
     * entity's class, but could be a superclass if the caller desires. This <em>same</em> class
     * token must be used when calling {@link #invoke(Class,Comparable,Action)}.
     * @param entity the keyed entity to be registered.
     *
     * @return the execution context created for the registered keyed entity.
     */
    <K extends Keyed> Context<K> registerKeyed (Class<? super K> kclass, K entity);

    /**
     * Registers a keyed entity as a child of the specified parent entity. The child will share the
     * parent's execution context rather than having an execution context of its own.
     *
     * @param kclass the class token used to identify this keyed entity. This will usually be the
     * entity's class, but could be a superclass if the caller desires. This <em>same</em> class
     * token must be used when calling {@link #invoke(Class,Comparable,Action)}.
     * @param child the keyed entity to be registered.
     * @param parent the context of the parent entity, obtained via {@link #resolve(Class)} or
     * {@link #resolve(Class,Comparable)}.
     */
    <K extends Keyed> void registerKeyed (Class<? super K> kclass, K child, Context<?> parent);

    /**
     * Registers a factory which will be used to create keyed entities on demand. If an action or
     * request is dispatched to a non-existent entity of type {@code kclass}, the entity will be
     * created and registered with the Nexus, and then the action or request will be dispatched to
     * it as normal.
     */
    <K extends Keyed> void registerKeyedFactory (Class<? super K> kclass, KeyedFactory<K> factory);

    /**
     * Registers a global map, whose contents will be mirrored to all nodes in the Nexus. Maps are
     * expensive (computationally and with regard to network bandwidth) and should be used
     * sparingly. Global maps are for situations where some mapping must be known across all nodes
     * in the network at all times, but where the use of a database is infeasable for performance
     * reasons. In general, a global map works best when entries are added infrequently and queried
     * frequently. If entries are updated too frequently, a map can become a serious burden on CPU
     * and network bandwidth.
     *
     * <p>Note: like an entity, each map defines its own execution context. Listeners added to the
     * map will execute in the map's context. However, it is <em>not</em> necessary that the map
     * only be read or updated from within its own context. In this way it differs from other
     * entities. Internally, global maps use a concurrent hash map, which allows reads and updates
     * to take place on any thread.</p>
     *
     * <p>Note: a map must be registered across all nodes during its initialization process.
     * Because of the way map syncing works, another node might send a newly started node updates
     * to any map at any time, thus the newly started node should establish the existence of all of
     * its maps before it joins the network. A map will also exist for the lifetime of the network.
     * There is no way to remove a map. If a map is no longer useful, one can simply remove all
     * entries from it and cease updates. This will reduce its overhead to essentially nil.</p>
     *
     * <p>Beware: updates to a global mapping (key/value pair) should not be made frequently by
     * different nodes, as no mechanism is in place to ensure that updates to a global map from two
     * different nodes will be applied in a consistent order. Global maps are best suited for
     * situations where one particular node "controls" a given mapping, and is the only node
     * responsible for updating it. If two nodes update the same mapping, one node's updates may be
     * applied last (and therefore "win") on some subset of the nodes, and another node's updates
     * may win on the remaining nodes.</p>
     *
     * @param id a unique string that identifies this map.
     * @return a reference to the newly registered map.
     * @throws NexusException if a map is already registered for {@code id}.
     */
    <K,V> RMap<K,V> registerMap (String id);

    /**
     * Clears a NexusObject registration. Note, this will <em>not</em> clear any associated
     * singleton or keyed entity registration.
     */
    void clear (NexusObject object);

    /**
     * Clears a singleton entity registration. This will <em>also</em> clear any NexusObject
     * registration for the singleton.
     */
    <S extends Singleton> void clear (Class<? super S> sclass, S entity);

    /**
     * Clears a keyed entity registration. This will <em>also</em> clear any NexusObject
     * registration for the keyed entity. The entity may have been manually registered or
     * automatically registered via a keyed entity factory. Note that in the latter case, the
     * entity can be reregistered if it is resolved again.
     */
    <K extends Keyed> void clearKeyed (Class<? super K> kclass, K entity);

    /**
     * Returns the keys of all instances of the supplied entity that are hosted on <em>this</em>
     * server. The returned set is immutable.
     */
    <T extends Keyed> Set<Comparable<?>> hostedKeys (Class<T> kclass);

    /**
     * Asserts that one is currently executing in the context of the specified singleton entity.
     * This check is only made if assertions are enabled in the executing JVM.
     * @throws AssertionError if one is not executing in the required context.
     */
    <T extends Singleton> void assertContext (Class<T> sclass);

    /**
     * Asserts that one is currently executing in the context of the specified keyed entity. This
     * check is only made if assertions are enabled in the executing JVM.
     * @throws AssertionError if one is not executing in the required context.
     */
    <T extends Keyed> void assertContext (Class<T> kclass, Comparable<?> key);

    /**
     * Executes an action in the context of the specified singleton entity (either object or
     * non-object entity). This call returns immediately, and executes the action at a later time,
     * regardless of whether the caller is already in the target context.
     *
     * @throws NexusException if no singleton instance is registered for {@code sclass}.
     */
    <T extends Singleton> void invoke (Class<T> sclass, Action<? super T> action);

    /**
     * Executes an action in the context (server+thread) of the specified keyed (object or
     * non-object) entity. This call returns immediately, and executes the action at a later time,
     * regardless of whether the caller is already in the target context. The supplied action may
     * be streamed to another server node if the context for the specified keyed entity is hosted
     * outside the local server node.
     */
    <T extends Keyed> void invoke (Class<T> kclass, Comparable<?> key, Action<? super T> action);

    @Deprecated /** Deprecated use {@code request}. */
    <T extends Singleton,R> R invoke (Class<T> sclass, Request<? super T,R> request);

    @Deprecated /** Deprecated use {@code request}. */
    <T extends Keyed,R> R invoke (Class<T> kclass, Comparable<?> key, Request<? super T,R> request);

    /**
     * Executes a request in the context of the specified singleton entity (either object or
     * non-object entity) and returns the result. The caller will remain blocked until the response
     * is received from the target context.
     *
     * @throws EntityNotFoundException if no singleton instance is registered for {@code sclass}.
     * @throws NexusException if an exception occurs while processing the request. The triggering
     * exception wil be available via {@link Exception#getCause}.
     */
    <T extends Singleton,R> R request (Class<T> sclass, Request<? super T,R> request);

    /**
     * Executes a request in the context of the specified singleton entity (either object or
     * non-object entity) and returns a future that can be used to obtain the result when the
     * caller is ready to block.
     *
     * @throws EntityNotFoundException if no singleton instance is registered for {@code sclass}
     */
    <T extends Singleton,R> Future<R> requestF (Class<T> sclass, Request<? super T,R> request);

    /**
     * Executes a request in the context (server+thread) of the specified keyed (object or
     * non-object) entity and returns the result. The caller will remain blocked until the response
     * is received from the target context, or the request times out (timeouts are configured on
     * the concrete implementation being used). The supplied request may be streamed to another
     * server node if the context for the specified keyed entity is hosted outside the local server
     * node.
     *
     * @throws EntityNotFoundException if {@code kclass} + {@code key} refer to an unknown entity.
     * @throws NexusException if an exception occurs while processing the request. The triggering
     * exception wil be available via {@link Exception#getCause}.
     */
    <T extends Keyed,R> R request (Class<T> kclass, Comparable<?> key, Request<? super T,R> request);

    /**
     * Executes a request in the context (server+thread) of the specified keyed (object or
     * non-object) entity and returns a future that can be used to obtain the result when the
     * caller is ready to block. The supplied request may be streamed to another server node if the
     * context for the specified keyed entity is hosted outside the local server node.
     *
     * @throws EntityNotFoundException if {@code kclass} + {@code key} refer to an unknown entity.
     */
    <T extends Keyed,R> Future<R> requestF (Class<T> kclass, Comparable<?> key,
                                            Request<? super T,R> request);

    //
    // these methods are primarily for use in a multi-server Nexus system

    /**
     * Generates an id for a keyed entity with the specified class that is guaranteed to be unique
     * across the whole network. This entity id is ephemeral, and must not be stored in persistent
     * storage. It is only valid for a keyed entity hosted on this server, and only for the
     * lifetime of this server. This operation uses only data local to this server and is also
     * thread-safe.
     *
     * <p>Note: the uniqueness of this id is based on the assignment to each server in a Nexus
     * network of a transient integer identifier which identifies that server for the duration of
     * its membership in the network. If a server shuts down and leaves the network, a new server
     * may join the network and reuse the server identifier previously used by the old server. As
     * these generated identifiers are based on the server identifier, it is necessary that the
     * entities using these identifiers "go away" when the server goes away. If the entities are
     * hosted on that server and the ids are not written to persistent storage, that happens
     * naturally.</p>
     *
     * <p>Note: the use of these ids implies acceptance of a couple of arbitrary limits. One is
     * that you will not have more than one thousand active server nodes in your Nexus network. The
     * other is that you will not have more than two million entities (of type {@code kclass})
     * actively operating on a single server in your network. These limits seem like a good
     * engineering tradeoff given the state of technology circa the two thousand teens. If they
     * become limiting in the future, we'll raise them.</p>
     */
    <T extends Keyed> int nextId (Class<T> kclass);

    /**
     * Computes a "census" for the keyed entity identified by the specified class. This is computed
     * entirely from server-local data and is thus relatively inexpensive.
     *
     * @return a map from server identifier to the number of instances of the specified keyed
     * entity hosted on the server in question.
     */
    <T extends Keyed> Map<Integer,Integer> census (Class<T> kclass);

    /**
     * Executes an action in the context (server+thread) of the specified keyed entities. This call
     * returns immediately, and executes the actions at a later time. The supplied action will be
     * streamed to other server nodes for those keyed entities that are hosted on other servers.
     * This can be more efficient than issuing actions separately for each entity, as the
     * initiating server will group the keys based on the servers currently hosting those keys and
     * will send the action once to each server rather repeatedly, for each key.
     */
    <T extends Keyed> void invoke (
        Class<T> kclass, Set<? extends Comparable<?>> keys, Action<? super T> action);

    /**
     * Executes a request on a all entities of type {@code kclass} with keys in {@code keys} and
     * gathers the results into a map, indexed by entity key. The request will be run separately in
     * the context of each entity and the result will be added to the map. This can be more
     * efficient than issuing requests separately for each entity, as the initiating server will
     * group the keys based on the servers currently hosting those keys and will issue a single
     * network request to each server rather than one for each key.
     *
     * <p>Any entities that are not currently hosted by any server in the network will simply be
     * ommitted from the map. Any requests that result in failure are also omitted from the map
     * (and the failure will be logged). If you need to know about individual failures, use {@link
     * #gatherF} which preserves and reports failure. The final result is made available once all
     * located entities have completed execution of the request.</p>
     */
    <T extends Keyed,R> Map<Comparable<?>,R> gather (
        Class<T> kclass, Set<? extends Comparable<?>> keys, Request<? super T,R> request);

    /**
     * Executes a request on a all entities of type {@code kclass} with keys in {@code keys} and
     * gathers the results into a map, indexed by entity key. The request will be run separately in
     * the context of each entity and the result will be added to the map. This can be more
     * efficient than issuing requests separately for each entity, as the initiating server will
     * group the keys based on the servers currently hosting those keys and will issue a single
     * network request to each server rather than one for each key.
     *
     * <p>Any entities that are not currently hosted by any server in the network will simply be
     * ommitted from the map. The future for each request will become available as that request is
     * processed, with the caveat that results for remote entities will arrive in batches as the
     * server hosting those entities returns all results at once.</p>
     */
    <T extends Keyed,R> Map<Comparable<?>,Future<R>> gatherF (
        Class<T> kclass, Set<? extends Comparable<?>> keys, Request<? super T,R> request);

    /**
     * Invokes an action on the instance of a singleton hosted on the specified server.
     *
     * @param serverId a server identifier that came from one of the methods that returns
     * information on the currently running servers, like {@link #census}.
     *
     * @exception ServerNotFoundException thrown if {@code serverId} refers to a server that is not
     * known to the network.
     */
    <T extends Singleton> void invokeOn (Class<T> sclass, int serverId, Action<? super T> action);

    /**
     * Invokes a request on the instance of a singleton hosted on the specified server and blocks
     * awaiting the response.
     *
     * @param serverId a server identifier that came from one of the methods that returns
     * information on the currently running servers, like {@link #census}.
     *
     * @exception ServerNotFoundException thrown if {@code serverId} refers to a server that is not
     * known to the network.
     */
    <T extends Singleton,R> R requestFrom (
        Class<T> sclass, int serverId, Request<? super T,R> request);

    /**
     * Invokes a request on the instance of a singleton hosted on the specified server and returns
     * a future that can be used to block awaiting the result when desired.
     *
     * @param serverId a server identifier that came from one of the methods that returns
     * information on the currently running servers, like {@link #census}.
     *
     * @exception ServerNotFoundException thrown if {@code serverId} refers to a server that is not
     * known to the network.
     */
    <T extends Singleton,R> Future<R> requestFromF (
        Class<T> sclass, int serverId, Request<? super T,R> request);

    /**
     * Invokes an action on the specified singleton on every server in the network.
     */
    <T extends Singleton> void broadcast (Class<T> sclass, Action<? super T> action);

    /**
     * Invokes the supplied request on the specified singleton on every server in the network and
     * blocks until all results are available. Any requests that result in failure are omitted from
     * the map (and the failure will be logged). If you need to know about individual failures, use
     * {@link #surveyF} which preserves and reports failure.
     *
     * @return a map from serverId to the result returned by that server.
     */
    <T extends Singleton,R> Map<Integer,R> survey (Class<T> sclass, Request<? super T,R> request);

    /**
     * Invokes the supplied request on the specified singleton on every server in the network.
     *
     * @return a map from server id to a future that can be used to obtain the result from that
     * server.
     */
    <T extends Singleton,R> Map<Integer,Future<R>> surveyF (
        Class<T> sclass, Request<? super T,R> request);
}
