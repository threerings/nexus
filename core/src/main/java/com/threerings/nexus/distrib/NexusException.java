//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import com.threerings.nexus.io.Streamable;
import com.threerings.nexus.util.Log;

/**
 * An exception used to report failure of Nexus services. Can be streamed, though in said cases,
 * only the message provided to the constructor is preserved (not the stack trace, nor any supplied
 * causal exception).
 */
public class NexusException extends RuntimeException
    implements Streamable
{
    /**
     * Throws a NexusException with the supplied error message if {@code condition} is not true.
     */
    public static void require (boolean condition, String errmsg, Object... args) {
        if (!condition) throw new NexusException(Log.format(errmsg, args));
    }

    public NexusException (String message) {
        super(message);
    }

    public NexusException (String message, Throwable cause) {
        super(message, cause);
    }

    public NexusException (Throwable cause) {
        super(cause);
    }
}
