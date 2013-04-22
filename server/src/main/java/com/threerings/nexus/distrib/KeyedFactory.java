//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

/**
 * Creates keyed entities on demand. See {@link Nexus#registerKeyedFactory}.
 */
public interface KeyedFactory<T extends Keyed>
{
    /**
     * Creates the keyed entity for {@code key}.
     *
     * <p><em>Note:</em> this creation method will be invoked in the execution context of the
     * to-be-created entity. Any other actions to be invoked on the to-be-created entity will be
     * blocked until the entity creation completes. If entity creation fails, any pending actions
     * will be dropped, and pending requests will be failed.</p>
     */
    T create (Nexus nexus, Comparable<?> key);
}
