//
// $Id$
//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.server;

/**
 * Manages session-local attributes.
 */
public class SessionLocal
{
    /**
     * Returns the value of the supplied session-local attribute for the current session, or if no
     * attribute is currently configured for the supplied key.
     * @throws IllegalStateException if there is no current session.
     */
    public static <T> T get (Class<T> key)
    {
        return requireSession().getLocal(key);
    }

    /**
     * Configures the supplied value as the session-local attribute for the specified key.
     * @return the previously configured value for that key. (TODO: reject overwrites instead?)
     * @throws IllegalStateException if there is no current session.
     */
    public static <T> T set (Class<T> key, T value)
    {
        return requireSession().setLocal(key, value);
    }

    /**
     * Returns the current thread-local session.
     * @throws IllegalStateException if there is no current session.
     */
    public static Session requireSession ()
    {
        Session session = getSession();
        if (session == null) {
            throw new IllegalStateException("No current session.");
        }
        return session;
    }

    /**
     * Returns the current thread-local session, or null if the current thread is not processing a
     * request that originated from a session.
     */
    public static Session getSession ()
    {
        return _currentSession.get();
    }

    /**
     * Marks this session as the current session for the current thread. This should be called in a
     * try/finally clause, with the finally clause calling {@link #clearCurrent}.
     */
    protected static void setCurrent (Session current)
    {
        _currentSession.set(current);
    }

    /**
     * Clears the configured current session. This should be called in a try/finally clause with
     * {@link #setCurrent}.
     */
    protected static void clearCurrent ()
    {
        _currentSession.set(null);
    }

    private SessionLocal () {} // no constructsky

    /** Tracks the currently active session for this thread. */
    protected static final ThreadLocal<Session> _currentSession = new ThreadLocal<Session>();
}
