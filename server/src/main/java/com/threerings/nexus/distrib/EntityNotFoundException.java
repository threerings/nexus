//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

/**
 * An exception thrown when a request is made to operate on a keyed entity, and that entity could
 * not be found anywhere in the network. This is only reported for {@link Request} operations,
 * {@link Action} operations handle missing entities via {@link Action#onDropped}.
 */
public class EntityNotFoundException extends NexusException
{
    /** The class of the entity that could not be found. */
    public final Class<?> eclass;

    /** The key of the entity that could not be found. */
    public final Comparable<?> key;

    public EntityNotFoundException (String message, Class<?> eclass, Comparable<?> key) {
        super(message);
        this.eclass = eclass;
        this.key = key;
    }

    @Override public String getMessage () {
        return super.getMessage() +  " [class=" + eclass.getName() + ", key=" + key + "]";
    }
}
