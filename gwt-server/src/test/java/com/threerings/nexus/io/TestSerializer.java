//
// $Id$
//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.io;

/**
 * A serializer that handles our test classes.
 */
public class TestSerializer extends AbstractSerializer
{
    public TestSerializer ()
    {
        mapStreamer(new Streamer_Widget());
        mapStreamer(new Streamer_Widget.Wangle());
    }
}
