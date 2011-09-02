//
// $Id$

package com.threerings.nexus.gencode

import scala.collection.JavaConversions._
import scala.collection.immutable.SortedSet

import java.lang.{Iterable => JIterable}
import java.io.{InputStreamReader, Writer}

import javax.annotation.processing.{Filer}
import javax.lang.model.element.{Name, TypeElement}
import javax.lang.model.`type`.{TypeKind, TypeMirror}

import com.samskivert.mustache.Mustache

import com.threerings.nexus.distrib.DService

/**
 * Generates source files from extracted metadata.
 */
object Generator
{
  def setSourceHeader (header :String) {
    _header = header
  }

  def generate (filer :Filer, outer :TypeElement, metas :Seq[Metadata]) {
    // generate a streamer if we have streamable metadata
    val sables = metas collect { case sm :StreamableMetadata => sm }
    if (!sables.isEmpty) {
      val out = filer.createSourceFile(streamerName(outer.getQualifiedName.toString), outer)
      val w = out.openWriter
      try generateStreamer(outer, sables, w)
      finally w.close
    }

    // generate a factory if we have service metadata
    val svcs = metas collect { case sm :ServiceMetadata => sm }
    if (!svcs.isEmpty) {
      val out = filer.createSourceFile(factoryName(outer.getQualifiedName.toString), outer)
      val w = out.openWriter
      try generateFactory(outer, svcs, w)
      finally w.close
    }
  }

  def generateStreamer (oelem :TypeElement, metas :Seq[StreamableMetadata], out :Writer) {
    val (pkgName, className) = splitName(oelem.getQualifiedName.toString)

    // compute the imports needed for this compilation unit
    val stockImps = Set(Utils.StreamableName, Utils.StreamerName)
    val allImps = (stockImps /: metas.map(_.imports)) { _ ++ _ }
    val prunedImps = allImps.
      // filter out classes in the same package as the generated streamer
      filterNot(fqn => Utils.inPackage(fqn, pkgName)).
      // filter out DService due to the way it's handled
      filterNot(_ == DServiceName)

    generate(out, StreamerTmpl, new AnyRef {
      val `package` = pkgName
      val imports = orgImports(prunedImps)
      val outerName = className
      val outer = metas find(_.elem == oelem) getOrElse(null)
      val inners :JIterable[AnyRef] = metas filterNot(_.elem == oelem)
    })
  }

  def generateFactory (oelem :TypeElement, metas :Seq[ServiceMetadata], out :Writer) {
    if (metas.size > 1) {
      System.err.println("Multiple NexusService declarations in a single compilation unit " +
                         "not supported: " + oelem.getQualifiedName)
    }
    val svc = metas.head
    val (pkgName, className) = splitName(oelem.getQualifiedName.toString)

    // compute the imports needed for this compilation unit (filtering out classes in the same
    // package as the generated factory)
    val stockImps = Set(Utils.DServiceName, Utils.ServiceFactoryName)
    val prunedImps = (stockImps ++ svc.imports) filterNot(fqn => Utils.inPackage(fqn, pkgName))

    generate(out, FactoryTmpl, new AnyRef {
      val `package` = pkgName
      val imports = orgImports(prunedImps)
      val serviceName = className
      val methods = svc.methods
    })
  }

  private def orgImports (imports :Set[String]) = {
    val sortedImps = SortedSet[String]() ++ imports
    val javaImps = sortedImps filter(_.startsWith("java"))
    val nexusImps = sortedImps filter(_.startsWith("com.threerings.nexus"))
    val otherImps = sortedImps -- javaImps -- nexusImps
    new AnyRef {
      val java :JIterable[String] = javaImps
      val nexus :JIterable[String] = nexusImps
      val other :JIterable[String] = otherImps
    }
  }

  private def generate (out :Writer, tmpl :String, ctx :AnyRef) {
    val template = {
      val source = new InputStreamReader(getClass.getClassLoader.getResourceAsStream(tmpl))
      try Mustache.compiler.escapeHTML(false).compile(source)
      finally source.close
    }
    out.write(_header)
    template.execute(ctx, out)
  }

  private[gencode] def streamerName (fqName :String) = tagName(fqName, "Streamer")
  private[gencode] def factoryName (fqName :String) = tagName(fqName, "Factory")

  private def tagName (fqName :String, tag :String) = {
    val dotIdx = fqName.lastIndexOf(".")+1
    fqName.substring(0, dotIdx) + tag + "_" + fqName.substring(dotIdx)
  }

  private def splitName (fqName :String) = {
    val dotIdx = fqName.lastIndexOf(".")
    (if (dotIdx > 0) fqName.substring(0, dotIdx) else null, fqName.substring(dotIdx+1))
  }

  private var _header = ""

  private final val StreamerTmpl = "com/threerings/nexus/gencode/Streamer.tmpl"
  private final val FactoryTmpl = "com/threerings/nexus/gencode/Factory.tmpl"
  private final val DServiceName = classOf[DService[_]].getName
}
