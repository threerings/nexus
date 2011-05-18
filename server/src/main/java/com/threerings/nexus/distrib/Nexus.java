//
// $Id$
//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

/**
 * The main source for Nexus services.
 */
public interface Nexus
{
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

    // TODO: invoke an action on all singletons on all nodes
    // TODO: invoke a request on all singletons on all nodes, return a List/Map result?
}
