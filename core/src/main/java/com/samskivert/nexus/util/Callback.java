//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.util;

/**
 * Used to communicate asynchronous results.
 */
public interface Callback<T>
{
    /**
     * Called when the asynchronous request succeeded, supplying its result.
     */
    public void onSuccess (T result);

    /**
     * Called when the asynchronous request failed, supplying a cause for failure.
     */
    public void onFailure (Throwable cause);
}
