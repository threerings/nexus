//
// $Id$

package com.samskivert.nexus.server;

import java.io.ByteArrayInputStream;

import com.samskivert.nexus.io.JVMIO;
import com.samskivert.nexus.io.Streamable;
import com.samskivert.nexus.net.Upstream;

import static com.samskivert.nexus.util.Log.log;

/**
 * Represents an active client session.
 */
public class Session
    implements SessionManager.Input, Upstream.Handler
{
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
    }

    // from interface Upstream.Handler
    public void onSubscribe (Upstream.Subscribe message)
    {
    }

    // from interface Upstream.Handler
    public void onPostEvent (Upstream.PostEvent message)
    {
    }

    @Override public String toString ()
    {
        return _ipaddress; // TODO: if authed, report authed id?
    }

    protected Session (SessionManager mgr, String ipaddress, SessionManager.Output output)
    {
        _mgr = mgr;
        _ipaddress = ipaddress;
        _output = output;
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

    protected final SessionManager _mgr;
    protected final String _ipaddress;
    protected final SessionManager.Output _output;

    // these are used for processing input
    protected final FramedInputStream _fin = new FramedInputStream();
    protected final Streamable.Input _sin = JVMIO.newInput(_fin);

    protected static final byte[] EMPTY_BUFFER = new byte[0];
}
