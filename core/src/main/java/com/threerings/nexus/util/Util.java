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
