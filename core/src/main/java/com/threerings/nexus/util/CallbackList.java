//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects a list of callbacks, and eases the process of propagating success or failure to all
 * listed callbacks.
 */
public class CallbackList<T> implements Callback<T>
{
    /**
     * Creates a callback list, populated with the supplied callback.
     */
    public static <T> CallbackList<T> create (Callback<T> callback)
    {
        CallbackList<T> list = new CallbackList<T>();
        list.add(callback);
        return list;
    }

    /**
     * Adds the supplied callback to the list.
     *
     * @throws IllegalStateException if this callback has already fired.
     */
    public void add (Callback<T> callback)
    {
        checkState();
        _callbacks.add(callback);
    }

    /**
     * Adds the supplied callback to the list.
     *
     * @throws IllegalStateException if this callback has already fired.
     */
    public void remove (Callback<T> callback)
    {
        checkState();
        _callbacks.remove(callback);
    }

    // from interface Callback
    public void onSuccess (T result)
    {
        propagateSuccess(result);
    }

    // from interface Callback
    public void onFailure (Throwable cause)
    {
        propagateFailure(cause);
    }

    protected void propagateSuccess (T result)
    {
        for (Callback<T> cb : _callbacks) {
            cb.onSuccess(result);
        }
        _callbacks = null; // note that we've fired
    }

    protected void propagateFailure (Throwable cause)
    {
        for (Callback<T> cb : _callbacks) {
            cb.onFailure(cause);
        }
        _callbacks = null; // note that we've fired
    }

    protected void checkState ()
    {
        if (_callbacks == null) {
            throw new IllegalStateException("CallbackList has already fired.");
        }
    }

    /** A list of callbacks which will be notified on success or failure. */
    protected List<Callback<T>> _callbacks = new ArrayList<Callback<T>>();
}
