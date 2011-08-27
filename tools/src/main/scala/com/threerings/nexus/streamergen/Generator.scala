//
// $Id$

package com.threerings.nexus.streamergen

import java.io.Writer

import javax.annotation.processing.{Filer}
import javax.lang.model.element.{Name, TypeElement}

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
  def generate (filer :Filer, outer :TypeElement, metas :Seq[ClassMetadata]) {
    val outerName = outer.getQualifiedName.toString
    val splitIdx = outerName.lastIndexOf(".")+1
    val streamerName =
      outerName.substring(0, splitIdx) + "Streamer_" + outerName.substring(splitIdx)
    val out = filer.createSourceFile(streamerName, outer)
    System.out.println("Creating " + out)

    val w = out.openWriter
    try {
      generate(outer, metas, w)
    } finally {
      w.close
    }
  }

  def generate (outer :TypeElement, metas :Seq[ClassMetadata], out :Writer) {
    // TODO
  }
}
