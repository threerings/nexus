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
     * Creates a new attribute with the specified owner and initial value.
     */
    public static <T> DValue<T> create (NexusObject owner, T value) {
        return new DValue<T>(owner, value);
    }

    @Override public void readContents (Streamable.Input in) {
        _value = in.<T>readValue();
    }

    @Override public void writeContents (Streamable.Output out) {
        out.writeValue(_value);
    }

    protected DValue (NexusObject owner, T value) {
        super(value);
        _owner = owner;
        _index = owner.registerAttr(this);
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
    protected static class ChangeEvent<T> extends DAttribute.Event {
        public T oldValue = DistribUtil.<T>sentinelValue();

        public ChangeEvent (int targetId, short index, T value) {
            super(targetId, index);
            _value = value;
        }

        @Override public void applyTo (NexusObject target) {
            target.<DValue<T>>getAttribute(this.index).applyChange(_value, oldValue);
        }

        @Override protected void toString (StringBuilder buf) {
            super.toString(buf);
            buf.append(", value=").append(_value);
        }

        protected final T _value;
    }

    /** The object that owns this attribute. */
    protected final NexusObject _owner;

    /** The index of this attribute in its containing object. */
    protected final short _index;
}
