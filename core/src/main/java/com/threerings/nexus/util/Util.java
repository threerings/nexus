//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.util;

import react.Signal;

/**
 * Ultra generic utility methods.
 */
public class Util
{
    /**
     * Notifies the supplied callback of success, capturing and logging any exceptions.
     */
    public static <T> void notifySuccess (Callback<T> cb, T result) {
        try {
            cb.onSuccess(result);
        } catch (Exception e) {
            Log.log.warning("Callback.onSuccess failure", "cb", cb, e);
        }
    }

    /**
     * Notifies the supplied callback of failure, capturing and logging any exceptions.
     */
    public static void notifyFailure (Callback<?> cb, Throwable cause) {
        try {
            cb.onFailure(cause);
        } catch (Exception e) {
            Log.log.warning("Callback.onFailure failure", "cb", cb, e);
        }
    }

    /**
     * Emits an event to the supplied signal, capturing and logging any exceptions.
     */
    public static <T> void emit (Signal<T> signal, T event) {
        try {
            signal.emit(event);
        } catch (Exception e) {
            Log.log.warning("Signal.emit failure", "signal", signal, "event", event, e);
        }
    }
}
