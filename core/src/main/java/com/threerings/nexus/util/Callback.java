//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.util;

/**
 * Used to communicate asynchronous results.
 */
public interface Callback<T>
{
    /** A callback that chains failure to the supplied delegate callback. */
    public static abstract class Chain<T> implements Callback<T> {
        public Chain (Callback<?> onFailure) {
            _onFailure = onFailure;
        }
        public void onFailure (Throwable cause) {
            _onFailure.onFailure(cause);
        }
        protected Callback<?> _onFailure;
    }

    /** A callback that does nothing, including nothing in the event of failure. Don't use this if
     * doing so will result in the suppression of errors! */
    public static final Callback<Object> NOOP = new Callback<Object>() {
        @Override public void onSuccess (Object result) {} // noop!
        @Override public void onFailure (Throwable cause) {} // noop!
    };

    /**
     * Called when the asynchronous request succeeded, supplying its result.
     */
    void onSuccess (T result);

    /**
     * Called when the asynchronous request failed, supplying a cause for failure.
     */
    void onFailure (Throwable cause);
}
