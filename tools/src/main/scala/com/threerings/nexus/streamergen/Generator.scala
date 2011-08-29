//
// $Id$

package com.threerings.nexus.streamergen

import scala.collection.JavaConversions._

import java.lang.{Iterable => JIterable}
import java.io.{InputStreamReader, Writer}

import javax.annotation.processing.{Filer}
import javax.lang.model.element.{Name, TypeElement}

import com.samskivert.mustache.Mustache

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
    val out = filer.createSourceFile(streamerName(outer.getQualifiedName.toString), outer)
    val w = out.openWriter
    try generate(outer, metas, w)
    finally w.close
  }

  def generate (outer :TypeElement, metas :Seq[ClassMetadata], out :Writer) {
    val outerFQName = outer.getQualifiedName.toString
    val dotIdx = outerFQName.lastIndexOf(".")

    val ctx = new Object {
      val `package` = outerFQName.substring(0, dotIdx)
      val imports :JIterable[String] = List[String](
        "com.threerings.nexus.io.Monkey",
        "com.threerings.nexus.io.Butter"
      )
      val outerName = outerFQName.substring(dotIdx+1)
      val outerParams = ""
      val inner :JIterable[Object] = toContext(metas.filterNot(_.fqName == outer.getQualifiedName))
    }

    val template = {
      val source = new InputStreamReader(getClass.getClassLoader.getResourceAsStream(StreamerTmpl))
      try Mustache.compiler.compile(source)
      finally source.close
    }

    template.execute(ctx, out)
  }

  private[streamergen] def streamerName (fqName :String) = {
    val dotIdx = fqName.lastIndexOf(".")+1
    fqName.substring(0, dotIdx) + "Streamer_" + fqName.substring(dotIdx)
  }

  private def toContext (metas :Seq[ClassMetadata]) = metas map { m => new Object {
    val name = m.name
    val params = ""
    val `type` = m.name // TODO
    val writes :JIterable[Object] = List[AnyRef]() // TODO: ftype, field
    val reads :JIterable[Object] = List[AnyRef]() // TODO: fparams, ftype
  }}

  private val StreamerTmpl = "com/threerings/nexus/streamergen/Streamer.tmpl"
}
