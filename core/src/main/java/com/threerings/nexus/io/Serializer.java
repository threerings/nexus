//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.io;

import com.threerings.nexus.distrib.DService;
import com.threerings.nexus.distrib.NexusService;

/**
 * An automatically generated class that knows about all {@link Streamable} and
 * {@link NexusService} classes that will be used by a client. This is used on platforms that lack
 * reflection capabilities.
 */
public interface Serializer
{
    /** Returns the class assigned the supplied code.
     * @throws NexusException if no class is registered for the supplied code. */
    Class<?> getClass (short code);

    /** Returns the streamer for the class assigned the supplied code.
     * @throws NexusException if no streamer is registered for the supplied code.  */
    Streamer<?> getStreamer (short code);

    /** Returns the service factory for the class assigned the supplied code.
     * @throws NexusException if no service is registered for the supplied code.  */
    DService.Factory<?> getServiceFactory (short code);

    /** Returns the code assigned to the supplied class.
     * @throws NexusException if the class in question is not registered. */
    short getCode (Class<?> clazz);

    /** Returns the code assigned to the supplied service class.
     * @throws NexusException if the class in question is not registered. */
    short getServiceCode (Class<? extends NexusService> clazz);

    /** Writes the class code for the supplied value, and returns the streamer for same.
     * @throws NexusException if the class for the value in question is not registered. */
    <T> Streamer<T> writeStreamer (Streamable.Output out, T value);
}
