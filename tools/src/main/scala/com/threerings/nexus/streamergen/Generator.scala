//
// $Id$

package com.threerings.nexus.streamergen

import java.io.Writer

import javax.lang.model.element.Name

/**
 * Generates streamer source files from extracted metadata.
 */
object Generator
{
  // /** Handles streaming of {@link DSignal.EmitEvent} instances. */
  // public static class EmitEvent<T> implements Streamer<DSignal.EmitEvent<T>> {
  //     public Class<?> getObjectClass () {
  //         return DSignal.EmitEvent.class;
  //     }
  //     public void writeObject (Streamable.Output out, DSignal.EmitEvent<T> obj) {
  //         out.writeInt(obj.targetId);
  //         out.writeShort(obj.index);
  //         out.writeValue(obj._event);
  //     }
  //     public DSignal.EmitEvent<T> readObject (Streamable.Input in) {
  //         return new DSignal.EmitEvent<T>(in.readInt(), in.readShort(), in.<T>readValue());
  //     }
  // }
  def generate (outer :Name, metas :List[ClassMetadata], out :Writer) {
  }
}
