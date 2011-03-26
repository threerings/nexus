//
// $Id$

package com.samskivert.nexus.io;

import com.google.gwt.core.client.JavaScriptObject;

import com.samskivert.nexus.distrib.NexusService;

/**
 * Handles the decoding of an input payload into proper values.
 */
class StreamableInput extends Streamable.Input
{
    public StreamableInput (GWTIO.Serializer szer, String data)
    {
        _szer = szer;
        _values = decode(data);
    }

    @Override public native boolean readBoolean ()
    /*-{
        return !!this.@com.samskivert.nexus.io.StreamableInput::_values[
            this.@com.samskivert.nexus.io.StreamableInput::_nextValIdx++];
    }-*/;

    @Override public native byte readByte ()
    /*-{
        return this.@com.samskivert.nexus.io.StreamableInput::_values[
            this.@com.samskivert.nexus.io.StreamableInput::_nextValIdx++];
    }-*/;

    @Override public native short readShort ()
    /*-{
        return this.@com.samskivert.nexus.io.StreamableInput::_values[
            this.@com.samskivert.nexus.io.StreamableInput::_nextValIdx++];
    }-*/;

    @Override public native char readChar ()
    /*-{
        return this.@com.samskivert.nexus.io.StreamableInput::_values[
            this.@com.samskivert.nexus.io.StreamableInput::_nextValIdx++];
    }-*/;

    @Override public native int readInt ()
    /*-{
        return this.@com.samskivert.nexus.io.StreamableInput::_values[
            this.@com.samskivert.nexus.io.StreamableInput::_nextValIdx++];
    }-*/;

    @Override public native long readLong ()
    /*-{
        var data = this.@com.samskivert.nexus.io.StreamableInput::_values[
            this.@com.samskivert.nexus.io.StreamableInput::_nextValIdx++];
        return @com.google.gwt.lang.LongLib::longFromBase64(Ljava/lang/String;)(s);
    }-*/;

    @Override public native float readFloat ()
    /*-{
        return this.@com.samskivert.nexus.io.StreamableInput::_values[
            this.@com.samskivert.nexus.io.StreamableInput::_nextValIdx++];
    }-*/;

    @Override public native double readDouble ()
    /*-{
        return this.@com.samskivert.nexus.io.StreamableInput::_values[
            this.@com.samskivert.nexus.io.StreamableInput::_nextValIdx++];
    }-*/;

    @Override public String readString ()
    {
        return readBoolean() ? readNonNullString() : null;
    }

    @Override public <T extends Streamable> Class<T> readClass ()
    {
        @SuppressWarnings("unchecked") Class<T> clazz = (Class<T>)_szer.getClass(readShort());
        return clazz;
    }

    @Override protected <T> Streamer<T> readStreamer ()
    {
        @SuppressWarnings("unchecked") Streamer<T> ts = (Streamer<T>)_szer.getStreamer(readShort());
        return ts;
    }

    @Override protected <T extends NexusService> ServiceFactory<T> readServiceFactory ()
    {
        @SuppressWarnings("unchecked") ServiceFactory<T> factory =
            (ServiceFactory<T>)_szer.getServiceFactory(readShort());
        return factory;
    }

    protected native String readNonNullString ()
    /*-{
        return this.@com.samskivert.nexus.io.StreamableInput::_values[
            this.@com.samskivert.nexus.io.StreamableInput::_nextValIdx++];
    }-*/;

    /** Decodes an encoded payload into a JavaScript array. As the payload is formatted as a
     * JavaScript array, this is done using {@code eval()} for efficiency. */
    private static native JavaScriptObject decode (String encoded)
    /*-{
        return eval(encoded);
    }-*/;

    /** Returns the length of the supplied JavaScript array. */
    private static native int getLength (JavaScriptObject array)
    /*-{
        return array.length;
    }-*/;

    protected final GWTIO.Serializer _szer;
    protected final JavaScriptObject _values;
    protected int _nextValIdx;
}
