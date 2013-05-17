//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.util;

import react.Slot;

/**
 * Callback related utility methods.
 */
public class Callbacks
{
    /**
     * Creates a callback that notifies the supplied signals on success or failure.
     */
    public static <T> Callback<T> from (final Slot<? super T> onSuccess,
                                        final Slot<? super Throwable> onFailure) {
        return new Callback<T>() {
            @Override public void onSuccess (T value) { onSuccess.onEmit(value); }
            @Override public void onFailure (Throwable cause) { onFailure.onEmit(cause); }
        };
    }
}
