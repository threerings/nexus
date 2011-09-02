//
// $Id$

package com.threerings.nexus.gencode

import scala.collection.JavaConversions._
import scala.io.Source

import java.util.Set

import javax.annotation.processing.{AbstractProcessor, Filer, Messager, ProcessingEnvironment}
import javax.annotation.processing.{SupportedAnnotationTypes, SupportedOptions, RoundEnvironment}
import javax.lang.model.SourceVersion
import javax.lang.model.element.{Element, TypeElement, Name}
import javax.tools.Diagnostic

/**
 * Generates `Streamer` implementations for {@link Streamable} classes, `Factory` implementations
 * for {@link NexusService} interfaces, and other generated code bits.
 */
@SupportedOptions(Array(ProcessorOpts.Header))
@SupportedAnnotationTypes(Array("*"))
class Processor extends AbstractProcessor {
  override def init (procenv :ProcessingEnvironment) {
    super.init(procenv)
    _filer = procenv.getFiler
    _msgr = procenv.getMessager
    _scanner = new Scanner(procenv)

    // if a source header was specified, read it in and configure the generator
    val header = procenv.getOptions().get(ProcessorOpts.Header)
    if (header != null) {
      try {
        Generator.setSourceHeader(Source.fromFile(header).mkString)
      } catch {
        case e => _msgr.printMessage(
          Diagnostic.Kind.WARNING, "Unable to read source header at '" + header + "': " + e)
      }
    }
  }

  override def getSupportedSourceVersion :SourceVersion = SourceVersion.latest

  override def process (annotations :Set[_ <: TypeElement], roundEnv :RoundEnvironment) = {
    if (!roundEnv.processingOver) {
      for (elem <- roundEnv.getRootElements) elem match {
        case telem :TypeElement => {
          val metas = _scanner.scanUnit(elem)
          if (!metas.isEmpty) generate(telem, metas)
        }
        case _ => _msgr.printMessage(Diagnostic.Kind.WARNING, "Weird element? " + elem.getClass)
      }
    }
    false
  }

  protected def generate (elem :TypeElement, metas :Seq[Metadata]) {
    Generator.generate(_msgr, _filer, elem, metas)
  }

  protected var _msgr :Messager = _
  protected var _filer :Filer = _
  protected var _scanner :Scanner = _
}

object ProcessorOpts {
  final val Header = "com.threerings.nexus.gencode.header"
}
