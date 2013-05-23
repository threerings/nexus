//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import java.util.ArrayList;
import java.util.List;

import react.RPromise;

import static org.junit.Assert.*;

/**
 * An event sink used for testing.
 */
public class TestSink implements EventSink
{
    // from interface EventSink
    public String getHost ()
    {
        return "localhost";
    }

    // from interface EventSink
    public void postEvent (NexusObject source, NexusEvent event)
    {
        _events.add(event);
        event.applyTo(source);
    }

    // from interface EventSink
    public <R> void postCall (NexusObject source, short attrIndex, short methodId, Object[] args,
                              RPromise<R> onResult)
    {
        DistribUtil.dispatchCall(source, attrIndex, methodId, args,
                                 onResult == null ? null : onResult.completer());
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

    /**
     * Asserts that exactly the specified number of events have been posted, and consumes them so
     * that assertions can be made about subsequent events.
     */
    public void assertPostedCount (int eventCount)
    {
        assertTrue("Event count mismatch, expected " + eventCount + ", have " + _events.size(),
                   _events.size() == eventCount);
        _events.clear();
    }

    protected List<NexusEvent> _events = new ArrayList<NexusEvent>();
}
