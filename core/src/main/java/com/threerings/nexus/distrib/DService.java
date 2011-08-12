//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import com.threerings.nexus.io.Streamable;

/**
 * An attribute that contains a Nexus service reference.
 */
public abstract class DService<T extends NexusService> implements DAttribute
{
    /** An implementation detail used by service dispatchers. */
    public static abstract class Dispatcher<T extends NexusService> extends DService<T> {
        /** Dispatches a service call that came in over the network. */
        public void dispatchCall (short methodId, Object[] args) {
            throw new IllegalArgumentException("Unknown service method [id=" + methodId +
                                               ", obj=" + _owner.getClass().getName() +
                                               ", attrIdx=" + _index + "]");
        }

        /** Used to concisely, and without warning, cast arguments of generic type. */
        @SuppressWarnings("unchecked")
        protected static <T> T cast (Object obj) {
            return (T)obj;
        }
    }

    /** Returns the service encapsulated by this attribute. */
    public abstract T get ();

    @Override public void init (NexusObject owner, short index) {
        _owner = owner;
        _index = index;
    }

    @Override public void readContents (Streamable.Input in) {
        // NOOP
    }

    @Override public void writeContents (Streamable.Output out) {
        // NOOP
    }

    /**
     * Used by marshallers to dispatch calls over the network.
     */
    protected void postCall (short methodId, Object... args) {
        _owner.postCall(_index, methodId, args);
    }

    /** The object that owns this attribute. */
    protected NexusObject _owner;

    /** The index of this attribute in its containing object. */
    protected short _index;
}
