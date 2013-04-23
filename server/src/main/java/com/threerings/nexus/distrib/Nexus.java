//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import java.util.concurrent.Future;

import react.RMap;
import react.Slot;

/**
 * The main source for Nexus services. See
 * <a href="https://github.com/threerings/nexus/wiki/ServerConcepts">this documentation</a> for a
 * detailed explanation of entities and execution contexts.
 *
 * <p><b>Note:</b> {@link Keyed} and {@link Singleton} entities are registered and referenced by
 * their concrete class. However, some special handling is performed to allow for more natural
 * handling of subclasses. Specifically, the class that actually <em>declares</em> that it
 * implements {@code Keyed} or {@code Singleton} is the class under which a keyed or singleton
 * entity will be registered, and that class token must be used to reference the entity. For
 * example:
 * <pre>{@code
 * class BaseKeyed implements Keyed {
 *   public final int id;
 *   public BasedKeyed (int id) {
 *     this.id = id;
 *   }
 *   public Comparable<?> getKey () {
 *     return id;
 *   }
 * }
 *
 * class SpecializedKeyed extends BaseKeyed {
 *   public SpecializedKeyed (int id) {
 *     super(id);
 *   }
 * }
 *
 * nexus.register(new SpecializedKeyed(15));
 * nexus.invoke(BaseKeyed.class, 15, new Action<BaseKeyed>() {
 *   public void invoke (BaseKeyed entity) {
 *     // entity will be the instance of SpecializedKeyed
 *   }
 * });
 *
 * // the same holds for singletons, though that is a rarer use case
 * }</pre>
 */
public interface Nexus
{
    /** A handle used to control deferred actions. See {@link #invokeAfter}. */
    interface Deferred {
        /** Cancels this deferred action. This is mainly intended for stopping repeating actions.
         * <p> The caller should not assume that a canceled action will not subsequently be
         * executed at least once. It is possible that the cancellation occurs simultaneously with
         * the action being queued for execution on its entity, at which point the action cannot be
         * stopped. Thus any deferred action should contain code that confirm that its
         * preconditions still hold. </p>
         * @throws IllegalStateException if this action has already been canceled. */
        void cancel ();

        /** Causes this deferred action to repeat every {@code period} millis after its first
         * invocation, until canceled. This must be called immediately after {@link #invokeAfter}.
         * @return a reference to this instance for convenient chaining.
         * @throws IllegalStateException if this action has been canceled. */
        Deferred repeatEvery (long period);
    }

    /**
     * Registers an anonymous object with the Nexus in its own execution context.
     */
    void register (NexusObject object);

    /**
     * Registers an object as a child of the supplied singleton entity. The child and parent will
     * share the same execution context.
     */
    void register (NexusObject child, Singleton parent);

    /**
     * Registers an object as a child of the supplied keyed entity. The child and entity will share
     * the same execution context.
     */
    void register (NexusObject child, Keyed parent);

    /**
     * Registers a singleton (object or non-object) entity with the Nexus. This entity will only be
     * accessible on the server node on which it was created. Code may be executed in this entity's
     * context via, for example, {@link #invoke(Class,Action}. If the singleton is also a {@link
     * NexusObject}, clients can subscribe to the object using its class.
     *
     * @throws NexusException if an entity is already mapped for this singleton type.
     */
    void registerSingleton (Singleton entity);

    /**
     * Registers a singleton as a child of the supplied parent singleton entity. The child and
     * parent will share the same execution context.
     */
    void registerSingleton (Singleton child, Singleton parent);

    /**
     * Registers a keyed (object or non-object) entity with the Nexus. This entity will be
     * accessible to all server nodes in the Nexus. Code may be executed in this entity's context
     * (server+thread) via, for example, {@link #invoke{Class,Comparable,Action}}. If the keyed is
     * also a {@link NexusObject}, clients can subscribe to the object using its class and key.
     */
    void registerKeyed (Keyed entity);

    /**
     * Registers a keyed entity as a child of the supplied parent keyed entity. The child and
     * entity will share the same execution context.
     */
    void registerKeyed (Keyed child, Keyed parent);

    /**
     * Registers a factory which will be used to create keyed entities on demand. If an action or
     * request is dispatched to a non-existent entity of type {@code kclass}, the entity will be
     * created and registered with the Nexus, and then the action or request will be dispatched to
     * it as normal.
     */
    <T extends Keyed> void registerKeyedFactory (Class<T> kclass, KeyedFactory<T> factory);

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
     * entries from it and cease updates to reduce its overhead to essentially nil.</p>
     *
     * <p>Beware: updates to a global map should not be made frequently by different nodes, as no
     * mechanism is in place to ensure that updates to a global map from two different nodes will
     * be applied in a consistent order. Global maps are best suited for situations where one
     * particular node "controls" a given mapping, and is the only node responsible for updating
     * it. If two nodes update the same mapping, one node's updates may be applied last (and
     * therefore "win") on some subset of the nodes, and another node's updates may win on the
     * remaining nodes.</p>
     *
     * @param id a unique string that identifies this map.
     * @return a reference to the newly registered map.
     * @throws NexusException if a map is already registered for {@code id}.
     */
    <K,V> RMap<K,V> registerMap (String id);

    /**
     * Clears a registration created via {@link #register} or either {@link #registerChild}
     * variant.
     */
    void clear (NexusObject object);

    /**
     * Clears registration created via {@link #registerSingleton}.
     */
    void clearSingleton (Singleton entity);

    /**
     * Clears a registration created via {@link #registerKeyed}.
     */
    void clearKeyed (Keyed entity);

    /**
     * Returns a slot that routes an event notification into the appropriate execution context for
     * the supplied entity. Thus, regardless of what thread emits the event from the signal, the
     * supplied slot will be notified in the execution context of the supplied entity.
     *
     * <em>Note:</em> because the supplied slot has no reference to the entity in question, one
     * will necessarily only use this mechanism within the same server. Hence the requirement for
     * an actual reference to the entity, rather than its key.
     */
    <E, T extends Singleton> Slot<E> routed (T entity, Slot<E> slot);

    /**
     * Returns a slot that routes an event notification into the appropriate execution context for
     * the supplied entity. Thus, regardless of what thread emits the event from the signal, the
     * supplied slot will be notified in the execution context of the supplied entity.
     *
     * <em>Note:</em> because the supplied slot has no reference to the entity in question, one
     * will necessarily only use this mechanism within the same server. Hence the requirement for
     * an actual reference to the entity, rather than its key.
     */
    <E, T extends Keyed> Slot<E> routed (T entity, Slot<E> slot);

    /**
     * Executes an action in the context of the specified singleton entity (either object or
     * non-object entity). This call returns immediately, and executes the action at a later time,
     * regardless of whether the caller is already in the target context.
     *
     * @throws NexusException if no singleton instance is registered for {@code eclass}.
     */
    <T extends Singleton> void invoke (Class<T> eclass, Action<? super T> action);

    /**
     * Executes an action in the context (server+thread) of the specified keyed (object or
     * non-object) entity. This call returns immediately, and executes the action at a later time,
     * regardless of whether the caller is already in the target context. The supplied action may
     * be streamed to another server node if the context for the specified keyed entity is hosted
     * outside the local server node.
     */
    <T extends Keyed> void invoke (Class<T> kclass, Comparable<?> key, Action<? super T> action);

    @Deprecated /** Deprecated use {@code request}. */
    <T extends Singleton,R> R invoke (Class<T> eclass, Request<? super T,R> request);

    @Deprecated /** Deprecated use {@code request}. */
    <T extends Keyed,R> R invoke (Class<T> kclass, Comparable<?> key, Request<? super T,R> request);

    /**
     * Executes a request in the context of the specified singleton entity (either object or
     * non-object entity) and returns the result. The caller will remain blocked until the response
     * is received from the target context.
     *
     * @throws EntityNotFoundException if no singleton instance is registered for {@code eclass}.
     * @throws NexusException if an exception occurs while processing the request. The triggering
     * exception wil be available via {@link Exception#getCause}.
     */
    <T extends Singleton,R> R request (Class<T> eclass, Request<? super T,R> request);

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
     * Executes a request in the context of the specified singleton entity (either object or
     * non-object entity) and returns a future that can be used to obtain the result when the
     * caller is ready to block.
     *
     * @throws EntityNotFoundException if no singleton instance is registered for {@code eclass}
     */
    <T extends Singleton,R> Future<R> requestF (Class<T> eclass, Request<? super T,R> request);

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

    /**
     * Executes an action in the context of the specified singleton entity (either object or
     * non-object entity). The action is executed after the specified delay, unless canceled prior.
     *
     * @throws EntityNotFoundException if no singleton instance is registered for {@code eclass}
     */
    <T extends Singleton> Deferred invokeAfter (Class<T> eclass, long delay,
                                                Action<? super T> action);

    /**
     * Executes an action in the context (server+thread) of the specified keyed (object or
     * non-object) entity. The action is executed after the specified delay, unless canceled prior.
     * The action may be streamed to another server node if the context for the specified keyed
     * entity is hosted outside the local server node.
     */
    <T extends Keyed> Deferred invokeAfter (Class<T> eclass, Comparable<?> key,
                                            long delay, Action<? super T> action);

    /**
     * Asserts that one is currently executing in the context of the specified singleton entity.
     * This check is only made if assertions are enabled in the executing JVM.
     * @throws AssertionError if one is not executing in the required context.
     */
    <T extends Singleton> void assertContext (Class<T> eclass);

    /**
     * Asserts that one is currently executing in the context of the specified keyed entity. This
     * check is only made if assertions are enabled in the executing JVM.
     * @throws AssertionError if one is not executing in the required context.
     */
    <T extends Keyed> void assertContext (Class<T> kclass, Comparable<?> key);

    // TODO: invoke an action on all singletons on all nodes
    // TODO: invoke a request on all singletons on all nodes, return a List/Map result?
}
