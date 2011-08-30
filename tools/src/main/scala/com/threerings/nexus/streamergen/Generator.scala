//
// $Id$

package com.threerings.nexus.streamergen

import scala.collection.JavaConversions._

import java.lang.{Iterable => JIterable}
import java.io.{InputStreamReader, Writer}

import javax.annotation.processing.{Filer}
import javax.lang.model.element.{Name, TypeElement}
import javax.lang.model.`type`.{TypeKind, TypeMirror}

import com.samskivert.mustache.Mustache

/**
 * Generates streamer source files from extracted metadata.
 */
object Generator
{
  def setSourceHeader (header :String) {
    _header = header
  }

  def generate (filer :Filer, outer :TypeElement, metas :Seq[ClassMetadata]) {
    val out = filer.createSourceFile(streamerName(outer.getQualifiedName.toString), outer)
    val w = out.openWriter
    try generate(outer, metas, w)
    finally w.close
  }

  def generate (oelem :TypeElement, metas :Seq[ClassMetadata], out :Writer) {
    val pkgName = oelem.getEnclosingElement.toString
    val outerFQName = oelem.getQualifiedName.toString
    val dotIdx = outerFQName.lastIndexOf(".")

    // compute the imports needed for this compilation unit
    val allImps = (Set[String]() /: metas.map(_.imports)) { _ ++ _ }
    val prunedImps = allImps.filterNot(fqn => Utils.inPackage(fqn, pkgName))

    val ctx = new AnyRef {
      val `package` = if (dotIdx > 0) outerFQName.substring(0, dotIdx) else null
      val imports :JIterable[String] = prunedImps.toSeq.sorted
      val outerName = outerFQName.substring(dotIdx+1)
      val outerParams = ""
      val outer = metas find(_.elem == oelem) getOrElse(null)
      val inners :JIterable[AnyRef] = metas filterNot(_.elem == oelem)
    }

    val template = {
      val source = new InputStreamReader(getClass.getClassLoader.getResourceAsStream(StreamerTmpl))
      try Mustache.compiler.escapeHTML(false).compile(source)
      finally source.close
    }

    out.write(_header)
    template.execute(ctx, out)
  }

  private[streamergen] def streamerName (fqName :String) = {
    val dotIdx = fqName.lastIndexOf(".")+1
    fqName.substring(0, dotIdx) + "Streamer_" + fqName.substring(dotIdx)
  }

  private var _header = ""

  private final val StreamerTmpl = "com/threerings/nexus/streamergen/Streamer.tmpl"
}
