//
// $Id$
//
// Nexus GWTIO - I/O and network services for Nexus built on GWT and WebSockets
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.io;

/**
 * A serializer that handles our test classes.
 */
public class TestSerializer extends AbstractSerializer
{
    public TestSerializer ()
    {
        mapStreamer(new Streamer_Widget(), Widget.class);
        mapStreamer(new Streamer_Widget.Wangle(), Widget.Wangle.class);
    }
}
