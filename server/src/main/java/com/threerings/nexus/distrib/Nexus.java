//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import react.Slot;

/**
 * The main source for Nexus services.
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
     * Registers an anonymous object with the Nexus.
     */
    void register (NexusObject object);

    /**
     * Registers an object as a child of the supplied singleton entity. The child and parent will
     * share the same execution context (thread).
     */
    void register (NexusObject child, Singleton parent);

    /**
     * Registers an object as a child of the supplied keyed entity. The child and entity will share
     * the same execution context (thread).
     */
    void register (NexusObject child, Keyed parent);

    /**
     * Registers a singleton (object or non-object) entity with the Nexus. This entity will only be
     * accessible on the server node on which it was created. Code may be executed in this entity's
     * context (thread) via, for example, {@link #invoke(Class,Action}. If the singleton is also a
     * {@link NexusObject}, clients can subscribe to the object using its class.
     */
    void registerSingleton (Singleton entity);

    /**
     * Registers a singleton as a child of the supplied parent singleton entity. The child and
     * parent will share the same execution context (thread).
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
     * entity will share the same execution context (thread).
     */
    void registerKeyed (Keyed child, Keyed parent);

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
     * Executes an action in the context (thread) of the specified singleton entity (either object
     * or non-object entity). This call returns immediately, and executes the action at a later
     * time, regardless of whether the caller is already in the target context.
     */
    <T extends Singleton> void invoke (Class<T> eclass, Action<T> action);

    /**
     * Executes an action in the context (server+thread) of the specified keyed (object or
     * non-object) entity. This call returns immediately, and executes the action at a later time,
     * regardless of whether the caller is already in the target context. The supplied action may
     * be streamed to another server node if the context for the specified keyed entity is hosted
     * outside the local server node.
     */
    <T extends Keyed> void invoke (Class<T> kclass, Comparable<?> key, Action<T> action);

    /**
     * Executes a request in the context (thread) of the specified singleton entity (either object
     * or non-object entity) and returns the result. The caller will remain blocked until the
     * response is received from the target context.
     */
    <T extends Singleton,R> R invoke (Class<T> eclass, Request<T,R> request);

    /**
     * Executes a request in the context (server+thread) of the specified keyed (object or
     * non-object) entity and returns the result. The caller will remain blocked until the response
     * is received from the target context, or the request times out (timeouts are configured on
     * the concrete implementation being used). The supplied request may be streamed to another
     * server node if the context for the specified keyed entity is hosted outside the local server
     * node.
     */
    <T extends Keyed,R> R invoke (Class<T> kclass, Comparable<?> key, Request<T,R> request);

    /**
     * Executes an action in the context (thread) of the specified singleton entity (either object
     * or non-object entity). The action is executed after the specified delay, unless canceled
     * prior.
     */
    <T extends Singleton> Deferred invokeAfter (Class<T> eclass, long delay, Action<T> action);

    /**
     * Executes an action in the context (server+thread) of the specified keyed (object or
     * non-object) entity. The action is executed after the specified delay, unless canceled prior.
     * The action may be streamed to another server node if the context for the specified keyed
     * entity is hosted outside the local server node.
     */
    <T extends Keyed> Deferred invokeAfter (Class<T> eclass, Comparable<?> key,
                                            long delay, Action<T> action);

    // TODO: invoke an action on all singletons on all nodes
    // TODO: invoke a request on all singletons on all nodes, return a List/Map result?
}
