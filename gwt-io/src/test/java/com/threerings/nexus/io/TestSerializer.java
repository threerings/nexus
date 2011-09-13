//
// Nexus GWTIO - I/O and network services for Nexus built on GWT and WebSockets
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
        mapStreamer(Streamers.create(Widget.Color.class));
        mapStreamer(new Streamer_Widget.Wangle());
    }
}
