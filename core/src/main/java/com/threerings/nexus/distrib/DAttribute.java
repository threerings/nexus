//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.distrib;

import com.threerings.nexus.io.Streamable;

/**
 * The base type for all Nexus object attributes.
 */
public interface DAttribute
{
    /** A base class for all events associated with an attribute. */
    abstract class Event extends NexusEvent {
        /** The index of the attribute targetted by this event. */
        public final short index;

        protected Event (int targetId, short index) {
            super(targetId);
            this.index = index;
        }

        @Override protected void toString (StringBuilder buf) {
            super.toString(buf);
            buf.append(", idx=").append(index);
        }
    }

    /**
     * Reads the contents of this attribute from the supplied input.
     */
    void readContents (Streamable.Input in);

    /**
     * Writes the contents of this attribute to the supplied output.
     */
    void writeContents (Streamable.Output out);
}
