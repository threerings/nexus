//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import react.RFuture;
import react.Slot;
import react.Try;

import static com.threerings.nexus.util.Log.log;

/**
 * Provides access to interfaces that should not be called by normal clients, but which must be
 * accessible to the Nexus implementation code, which may reside in a different package.
 */
public class DistribUtil
{
    public static void init (NexusObject object, int id, EventSink sink) {
        object.init(id, sink);
    }

    public static void clear (NexusObject object) {
        object.clear();
    }

    public static <R> void dispatchCall (NexusObject object, int attrIdx, short methId,
                                         Object[] args, Slot<? super Try<R>> slot) {
        DService.Dispatcher<?> disp = null;
        try {
            @SuppressWarnings("unchecked") RFuture<R> result = (RFuture<R>)
                object.<DService.Dispatcher<?>>getAttribute(attrIdx).dispatchCall(methId, args);
            if (result != null) result.onComplete(slot);

        } catch (NexusException ne) {
            if (slot != null) slot.onEmit(Try.<R>failure(ne));
            else log.warning("Service call failed", "obj", object, "attr", disp,
                             "methId", methId, ne);

        } catch (Throwable t) {
            log.warning("Service call failed", "obj", object, "attr", disp, "methId", methId, t);
            if (slot != null) slot.onEmit(Try.<R>failure(new NexusException("Internal error")));
        }
    }

    /**
     * Returns a sentinel value for use by events in tracking unset values.
     */
    public static <T> T sentinelValue () {
        @SuppressWarnings("unchecked") T value = (T)SENTINEL_VALUE;
        return value;
    }

    private DistribUtil () {} // no constructsky

    /** Used by {@link #sentinelValue}. */
    private static final Object SENTINEL_VALUE = new Object();
}
