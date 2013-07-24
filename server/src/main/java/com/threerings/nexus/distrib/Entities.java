//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import react.Slot;

/**
 * Provides various entity-related helper methods.
 */
public class Entities
{
    /**
     * Returns a slot that routes an event notification into the appropriate execution context for
     * the supplied singleton entity. Thus, regardless of what thread emits the event from the
     * signal, the supplied slot will be notified in the execution context of the supplied entity.
     */
    public static <E, T extends Singleton> Slot<E> routed (final Nexus nexus, final Class<T> eclass,
                                                           final Slot<E> slot) {
        return new Slot<E>() {
            @Override public void onEmit (final E event) {
                nexus.invoke(eclass, new SlotAction<T,E>(slot, event));
            }
        };
    }

    /**
     * Returns a slot that routes an event notification into the appropriate execution context for
     * the supplied entity. Thus, regardless of what thread emits the event from the signal, the
     * supplied slot will be notified in the execution context of the supplied entity.
     */
    public static <E, T extends Keyed> Slot<E> routed (final Nexus nexus, T entity,
                                                       final Slot<E> slot) {
        // javac isn't smart enough to know that all the T's are the same here
        @SuppressWarnings("unchecked") final Class<T> eclass = (Class<T>)entity.getClass();
        return routed(nexus, eclass, entity.getKey(), slot);
    }

    /**
     * Returns a slot that routes an event notification into the appropriate execution context for
     * the supplied entity. Thus, regardless of what thread emits the event from the signal, the
     * supplied slot will be notified in the execution context of the supplied entity.
     *
     * <em>Note:</em> this method can only be used when the entity in question is known to be
     * hosted on this server.
     */
    public static <E, T extends Keyed> Slot<E> routed (final Nexus nexus, final Class<T> eclass,
                                                       final Comparable<?> key, final Slot<E> slot) {
        return new Slot<E>() {
            @Override public void onEmit (final E event) {
                nexus.invoke(eclass, key, new SlotAction<T,E>(slot, event));
            }
        };
    }

    protected static class SlotAction<T,E> extends Action<T> {
        public SlotAction (Slot<E> target, E event) {
            _target = target;
            _event = event;
        }
        @Override public void invoke (T entity) {
            _target.onEmit(_event);
        }
        protected final Slot<E> _target;
        protected final E _event;
    }
}
