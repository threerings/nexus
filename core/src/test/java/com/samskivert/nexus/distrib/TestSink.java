//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.distrib;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * An event sink used for testing.
 */
public class TestSink implements EventSink
{
    // from interface EventSink
    public void postEvent (NexusObject source, NexusEvent event)
    {
        _events.add(event);
        event.applyTo(source);
    }

    /**
     * Asserts that an event of the specified class was the next event posted to this event sink.
     * Consumes the event so that assertions can be made about subsequent events.
     *
     * @return the event in question, so that assertions can be made about its contents, if
     * desired.
     */
    public <T extends NexusEvent> T assertPosted (Class<T> eclass)
    {
        assertTrue("Have no events, expected " + eclass.getName(), _events.size() > 0);
        NexusEvent event = _events.remove(0);
        assertEquals("Have " + event.getClass().getName() + " expected " + eclass.getName(),
                     event.getClass(), eclass);
        @SuppressWarnings("unchecked") T tevent = (T)event;
        return tevent;
    }

    protected List<NexusEvent> _events = new ArrayList<NexusEvent>();
}
