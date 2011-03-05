//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.server;

import java.io.ByteArrayInputStream;
import java.util.Set;

import com.google.common.collect.Sets;

import com.samskivert.nexus.distrib.Action;
import com.samskivert.nexus.distrib.NexusEvent;
import com.samskivert.nexus.distrib.NexusException;
import com.samskivert.nexus.distrib.NexusObject;
import com.samskivert.nexus.io.JVMIO;
import com.samskivert.nexus.io.Streamable;
import com.samskivert.nexus.net.Downstream;
import com.samskivert.nexus.net.Upstream;

import static com.samskivert.nexus.util.Log.log;

/**
 * Represents an active client session.
 */
public class Session
    implements SessionManager.Input, Upstream.Handler, ObjectManager.Subscriber
{
    /**
     * Returns the IP address via which this session is operating. Note that multiple sessions can
     * be operating over the same IP, so do not treat this as a unique key.
     */
    public String getIPAddress ()
    {
        return _ipaddress;
    }

    // from interface SessionManager.Input
    public void onMessage (byte[] data, int offset, int length)
    {
        try {
            _fin.setFrame(data, offset, length);
            Upstream msg = _sin.<Upstream>readValue();
            msg.dispatch(this);
        } catch (Throwable t) {
            log.warning("Failure decoding incoming message", "session", this);
        }
    }

    // from interface SessionManager.Input
    public void onSendError (Throwable error)
    {
    }

    // from interface SessionManager.Input
    public void onReceiveError (Throwable error)
    {
    }

    // from interface SessionManager.Input
    public void onDisconnect ()
    {
        // clear any object subscriptions we currently have
        for (Integer id : _subscriptions) {
            _omgr.clearSubscriber(id, this);
        }
        _subscriptions.clear();

        // let the session manager know that we disconnected
        _smgr.sessionDisconnected(this);
    }

    // from interface Upstream.Handler
    public void onSubscribe (Upstream.Subscribe message)
    {
        try {
            // TODO: per-session class loaders or other fancy business
            NexusObject object = _omgr.addSubscriber(Class.forName(message.clazz), this);
            _subscriptions.add(object.getId());
            sendMessage(new Downstream.Subscribe(object));
        } catch (Throwable t) {
            sendMessage(new Downstream.SubscribeFailure(message.clazz, t.getMessage()));
        }
    }

    // from interface Upstream.Handler
    public void onPostEvent (final Upstream.PostEvent message)
    {
        _omgr.postEvent(message.event);
    }

    // from interface ObjectManager.Subscriber
    public void forwardEvent (NexusEvent event)
    {
        sendMessage(new Downstream.DispatchEvent(event));
    }

    @Override public String toString ()
    {
        return _ipaddress; // TODO: if authed, report authed id?
    }

    protected Session (SessionManager smgr, ObjectManager omgr, String ipaddress,
                       SessionManager.Output output)
    {
        _smgr = smgr;
        _omgr = omgr;
        _ipaddress = ipaddress;
        _output = output;
    }

    protected void sendMessage (Downstream msg)
    {
        // TODO
    }

    protected static class FramedInputStream extends ByteArrayInputStream
    {
        public FramedInputStream () {
            super(EMPTY_BUFFER);
        }

        public void setFrame (byte[] data, int offset, int length) {
            this.buf = data;
            this.pos = offset;
            this.count = Math.min(offset + length, data.length);
            this.mark = offset;
        }
    }

    protected final SessionManager _smgr;
    protected final ObjectManager _omgr;
    protected final String _ipaddress;
    protected final SessionManager.Output _output;

    // these are used for processing input
    protected final FramedInputStream _fin = new FramedInputStream();
    protected final Streamable.Input _sin = JVMIO.newInput(_fin);

    /** Tracks our extant object subscriptions. */
    protected final Set<Integer> _subscriptions = Sets.newHashSet();

    protected static final byte[] EMPTY_BUFFER = new byte[0];
}
