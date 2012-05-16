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

    public static class NoOp<T> implements Callback<T> {
        @Override public void onSuccess (T result) {
            // We're a No-Op - do nothing.
        }

        @Override public void onFailure (Throwable cause) {
            // We're a No-Op - do nothing.
        }
    }

    /**
     * Called when the asynchronous request succeeded, supplying its result.
     */
    void onSuccess (T result);

    /**
     * Called when the asynchronous request failed, supplying a cause for failure.
     */
    void onFailure (Throwable cause);
}
