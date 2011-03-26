//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.samskivert.nexus.distrib.Action;
import com.samskivert.nexus.distrib.NexusEvent;
import com.samskivert.nexus.distrib.NexusException;
import com.samskivert.nexus.distrib.NexusObject;
import com.samskivert.nexus.io.ByteBufferInputStream;
import com.samskivert.nexus.io.FramingOutputStream;
import com.samskivert.nexus.io.JVMIO;
import com.samskivert.nexus.io.Streamable;
import com.samskivert.nexus.net.Downstream;
import com.samskivert.nexus.net.Upstream;
import com.samskivert.nexus.util.Callback;

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

    /**
     * Returns the value of the specified session-local attribute, or null if no value is currently
     * configured for the supplied key.
     */
    public <T> T getLocal (Class<T> key)
    {
        @SuppressWarnings("unchecked") T value = (T)_locals.get(key);
        return value;
    }

    /**
     * Configures the supplied value as the session-local attribute for the specified key.
     * @return the previously configured value for that key. (TODO: reject overwrites instead?)
     */
    public <T> T setLocal (Class<T> key, T value)
    {
        @SuppressWarnings("unchecked") T ovalue = (T)_locals.put(key, value);
        return ovalue;
    }

    // from interface SessionManager.Input
    public void onMessage (ByteBuffer data)
    {
        try {
            _bin.setBuffer(data);
            Upstream msg = _sin.<Upstream>readValue();
            msg.dispatch(this);
        } catch (Throwable t) {
            log.warning("Failure decoding incoming message", "session", this, t);
        }
    }

    // from interface SessionManager.Input
    public void onSendError (Throwable error)
    {
        // TODO
    }

    // from interface SessionManager.Input
    public void onReceiveError (Throwable error)
    {
        // TODO
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
    public void onSubscribe (Upstream.Subscribe msg)
    {
        SessionLocal.setCurrent(this);
        try {
            // TODO: per-session class loaders or other fancy business
            NexusObject object = _omgr.addSubscriber(msg.addr, this);
            _subscriptions.add(object.getId());
            sendMessage(new Downstream.Subscribe(object));
        } catch (Throwable t) {
            sendMessage(new Downstream.SubscribeFailure(msg.addr, t.getMessage()));
        } finally {
            SessionLocal.clearCurrent();
        }
    }

    // from interface Upstream.Handler
    public void onUnsubscribe (Upstream.Unsubscribe msg)
    {
        // TODO
    }

    // from interface Upstream.Handler
    public void onPostEvent (Upstream.PostEvent msg)
    {
        // we pass things straight through to the object manager which handles everything
        _omgr.dispatchEvent(msg.event, this);
    }

    // from interface Upstream.Handler
    public void onServiceCall (final Upstream.ServiceCall msg)
    {
        // if we have a call id, stick a callback in the final slot which communicates the result
        // back to the calling client
        Object[] args = msg.args.toArray();
        if (msg.callId > 0) {
            assert(args[args.length-1] == null);
            args[args.length-1] = new Callback<Object>() {
                public void onSuccess (Object result) {
                    sendMessage(new Downstream.ServiceResponse(msg.callId, result));
                }
                public void onFailure (Throwable cause) {
                    sendMessage(new Downstream.ServiceFailure(msg.callId, cause.getMessage()));
                }
            };
        }
        _omgr.dispatchCall(msg.objectId, msg.attrIndex, msg.methodId, args, this);
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

    /**
     * Flattens a message into bytes and sends it to the client via the transport layer.
     */
    protected synchronized void sendMessage (Downstream msg)
    {
        // we may be called from many threads, so serialize access to the output streams
        synchronized (_sout) {
            _fout.prepareFrame();
            _sout.writeValue(msg);
            _output.send(_fout.frameAndReturnBuffer());
        }
    }

    protected final SessionManager _smgr;
    protected final ObjectManager _omgr;
    protected final String _ipaddress;
    protected final SessionManager.Output _output;

    // these are used for message I/O
    protected final ByteBufferInputStream _bin = new ByteBufferInputStream();
    protected final Streamable.Input _sin = JVMIO.newInput(_bin);
    protected final FramingOutputStream _fout = new FramingOutputStream();
    protected final Streamable.Output _sout = JVMIO.newOutput(_fout);

    /** Tracks our extant object subscriptions. */
    protected final Set<Integer> _subscriptions = Sets.newHashSet();

    /** Tracks session-local attributes. */
    protected final Map<Class<?>, Object> _locals = Maps.newHashMap();

    protected static final byte[] EMPTY_BUFFER = new byte[0];
}
