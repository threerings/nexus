//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.io;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.threerings.nexus.distrib.NexusException;
import com.threerings.nexus.distrib.NexusService;

/**
 * A basic {@link Serializer} implementation that handles standard types. This is extended by
 * generated serializers to add support for project classes.
 */
public abstract class AbstractSerializer implements Serializer
{
    // from interface Serializer
    public Class<?> getClass (short code) {
        return nonNull(_classes.get(code), "Unknown class code ", code);
    }

    // from interface Serializer
    public Streamer<?> getStreamer (short code) {
        return nonNull(_streamers.get(code), "Unknown class code ", code);
    }

    // from interface Serializer
    public ServiceFactory<?> getServiceFactory (short code) {
        return nonNull(_services.get(code), "Unknown service code ", code);
    }

    // from interface Serializer
    public short getCode (Class<?> clazz) {
        return nonNull(_codes.get(clazz), "Unknown streamable class ", clazz);
    }

    // from interface Serializer
    public short getServiceCode (Class<? extends NexusService> clazz) {
        return nonNull(_serviceCodes.get(clazz), "Unknown service class ", clazz);
    }

    // from interface Serializer
    public <T> Streamer<T> writeStreamer (Streamable.Output out, T value) {
        if (value == null) {
            return this.<T>writeClass(out, (short)0); // null streamer has code 0
        }

        // if the class is known, just look up the code and write it
        Class<?> vclass = value.getClass();
        Short code = _codes.get(vclass);
        if (code != null) {
            return this.<T>writeClass(out, code);
        }

        // if the class is some more obscure subtype of list/set/map, use the stock
        // streamer and cache this type with the same code
        if (value instanceof List) {
            _codes.put(vclass, code = _codes.get(ArrayList.class));
            return this.<T>writeClass(out, code);
        } else if (value instanceof Set) {
            _codes.put(vclass, code = _codes.get(HashSet.class));
            return this.<T>writeClass(out, code);
        } else if (value instanceof Map) {
            _codes.put(vclass, code = _codes.get(HashMap.class));
            return this.<T>writeClass(out, code);
        }

        // otherwise: houston, we have a problem
        throw new NexusException("Requested to stream unknown type " + vclass);
    }

    @SuppressWarnings("unchecked") 
    protected final <T> Streamer<T> writeClass (Streamable.Output out, Short code) {
        out.writeShort(code);
        return (Streamer<T>)_streamers.get(code);
    }

    protected void mapStreamer (Streamer<?> streamer) {
        short code = (short)++_nextCode;
        _classes.put(code, streamer.getObjectClass());
        _streamers.put(code, streamer);
        _codes.put(streamer.getObjectClass(), code);
    }

    protected <T extends Enum<T>> void mapEnumStreamer (Class<T> eclass) {
        mapStreamer(Streamers.create(eclass));
    }

    protected void mapService (ServiceFactory<?> factory, Class<? extends NexusService> clazz) {
        short code = (short)++_nextServiceCode;
        _services.put(code, factory);
        _serviceCodes.put(clazz, code);
    }

    @SuppressWarnings("rawtypes")
    protected AbstractSerializer () {
        // map the streamers for our basic types
        mapStreamer(new Streamers.Streamer_Null());
        mapStreamer(new Streamers.Streamer_Boolean());
        mapStreamer(new Streamers.Streamer_Byte());
        mapStreamer(new Streamers.Streamer_Character());
        mapStreamer(new Streamers.Streamer_Short());
        mapStreamer(new Streamers.Streamer_Integer());
        mapStreamer(new Streamers.Streamer_Long());
        mapStreamer(new Streamers.Streamer_Float());
        mapStreamer(new Streamers.Streamer_Double());
        mapStreamer(new Streamers.Streamer_String());
        // fast path for common implementations; a slow path will catch all other types with a
        // series of instanceof checks
        mapStreamer(new Streamers.Streamer_List());
        mapStreamer(new Streamers.Streamer_Set());
        mapStreamer(new Streamers.Streamer_Map());

        // TEMP: map the core streamables manually while we lack a code generator
        mapStreamer(new com.threerings.nexus.distrib.Streamer_Address.OfKeyed());
        mapStreamer(new com.threerings.nexus.distrib.Streamer_Address.OfSingleton());
        mapStreamer(new com.threerings.nexus.distrib.Streamer_Address.OfAnonymous());
        mapStreamer(new com.threerings.nexus.distrib.Streamer_DMap.PutEvent());
        mapStreamer(new com.threerings.nexus.distrib.Streamer_DMap.RemoveEvent());
        mapStreamer(new com.threerings.nexus.distrib.Streamer_DSet.AddEvent());
        mapStreamer(new com.threerings.nexus.distrib.Streamer_DSet.RemoveEvent());
        mapStreamer(new com.threerings.nexus.distrib.Streamer_DValue.ChangeEvent());
        mapStreamer(new com.threerings.nexus.distrib.Streamer_DSignal.EmitEvent());
        mapStreamer(new com.threerings.nexus.net.Streamer_Downstream.Subscribe());
        mapStreamer(new com.threerings.nexus.net.Streamer_Downstream.SubscribeFailure());
        mapStreamer(new com.threerings.nexus.net.Streamer_Downstream.DispatchEvent());
        mapStreamer(new com.threerings.nexus.net.Streamer_Downstream.ServiceResponse());
        mapStreamer(new com.threerings.nexus.net.Streamer_Downstream.ServiceFailure());
        mapStreamer(new com.threerings.nexus.net.Streamer_Downstream.ObjectCleared());
        mapStreamer(new com.threerings.nexus.net.Streamer_Upstream.Subscribe());
        mapStreamer(new com.threerings.nexus.net.Streamer_Upstream.Unsubscribe());
        mapStreamer(new com.threerings.nexus.net.Streamer_Upstream.PostEvent());
        mapStreamer(new com.threerings.nexus.net.Streamer_Upstream.ServiceCall());
        // END TEMP
    }

    protected static <T> T nonNull (T value, String errmsg, Object data) {
        if (value == null) throw new NexusException(errmsg + data);
        return value;
    }

    protected int _nextCode = -1; // so that null gets zero
    protected int _nextServiceCode = 0; // services start at one, there's no null service

    protected final Map<Class<?>,Short> _codes = new HashMap<Class<?>,Short>();
    protected final Map<Short,Streamer<?>> _streamers = new HashMap<Short,Streamer<?>>();
    protected final Map<Short,Class<?>> _classes = new HashMap<Short,Class<?>>();

    protected final Map<Class<?>,Short> _serviceCodes = new HashMap<Class<?>,Short>();
    protected final Map<Short,ServiceFactory<?>> _services = new HashMap<Short,ServiceFactory<?>>();
}
