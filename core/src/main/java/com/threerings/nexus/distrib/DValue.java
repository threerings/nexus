//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import com.threerings.nexus.io.Streamable;

import static com.threerings.nexus.util.Log.log;

/**
 * A value attribute for a Nexus object. Contains a single value, which may be updated.
 */
public class DValue<T> extends react.Value<T> implements DAttribute
{
    /**
     * Creates a new attribute with the specified initial value.
     */
    public static <T> DValue<T> create (T value) {
        return new DValue<T>(value);
    }

    @Override public void init (NexusObject owner, short index) {
        _owner = owner;
        _index = index;
    }

    @Override public void readContents (Streamable.Input in) {
        _value = in.<T>readValue();
    }

    @Override public void writeContents (Streamable.Output out) {
        out.writeValue(_value);
    }

    protected DValue (T value) {
        super(value);
    }

    @Override protected void emitChange (T value, T oldValue) {
        // we don't call super as we defer notification until the event is dispatched
        ChangeEvent<T> event = new ChangeEvent<T>(_owner.getId(), _index, value);
        event.oldValue = oldValue;
        _owner.postEvent(event);
    }

    protected void applyChange (T value, T oldValue) {
        if (oldValue == DistribUtil.<T>sentinelValue()) {
            // we came in over the network: read our old value and update _value
            oldValue = updateLocal(value);
        } // else: we were initiated in this JVM: _value was already updated
        notifyChange(value, oldValue);
    }

    /** An event emitted when a value changes. */
    protected static class ChangeEvent<T> extends DAttribute.Event
    {
        public T oldValue = DistribUtil.<T>sentinelValue();

        public ChangeEvent (int targetId, short index, T value) {
            super(targetId, index);
            _value = value;
        }

        @Override public void applyTo (NexusObject target) {
            @SuppressWarnings("unchecked") DValue<T> attr =
                (DValue<T>)target.getAttribute(this.index);
            attr.applyChange(_value, oldValue);
        }

        @Override protected void toString (StringBuilder buf) {
            super.toString(buf);
            buf.append(", value=").append(_value);
        }

        protected final T _value;
    }

    /** The object that owns this attribute. */
    protected NexusObject _owner;

    /** The index of this attribute in its containing object. */
    protected short _index;
}
