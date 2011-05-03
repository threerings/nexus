//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.io;

/**
 * An interface implemented by generated streamer classes.
 */
public interface Streamer<T>
{
    /**
     * Returns the concrete class handled by this streamer.
     */
    Class<?> getObjectClass ();

    /**
     * Writes the supplied instance to the supplied output.
     */
    void writeObject (Streamable.Output out, T obj);

    /**
     * Instantiates an instance of the appropriate type using data read from the supplied input.
     */
    T readObject (Streamable.Input in);
}
