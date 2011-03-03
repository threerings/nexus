//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.distrib;

import com.samskivert.nexus.io.Streamable;

/**
 * An exception used to report failure of Nexus services. Can be streamed, though in said cases,
 * only the message provided to the constructor is preserved (not the stack trace, nor any supplied
 * causal exception).
 */
public class NexusException extends RuntimeException
    implements Streamable
{
    public NexusException (String message)
    {
        super(message);
    }

    public NexusException (String message, Throwable cause)
    {
        super(message, cause);
    }

    public NexusException (Throwable cause)
    {
        super(cause);
    }
}
