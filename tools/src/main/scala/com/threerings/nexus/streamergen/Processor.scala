//
// $Id$

package com.threerings.nexus.streamergen

import scala.collection.JavaConversions._

import java.util.Set

import javax.annotation.processing.{AbstractProcessor, Filer, ProcessingEnvironment}
import javax.annotation.processing.{RoundEnvironment, SupportedAnnotationTypes}
import javax.lang.model.SourceVersion
import javax.lang.model.element.{Element, TypeElement, Name}
import javax.tools.Diagnostic

/**
 * Generates {@code Streamer} implementations for {@link Streamable} classes.
 */
@SupportedAnnotationTypes(Array("*"))
class Processor extends AbstractProcessor {
  override def init (procenv :ProcessingEnvironment) {
    super.init(procenv)
    _filer = procenv.getFiler
    _scanner = new Scanner(procenv)
  }

  override def getSupportedSourceVersion :SourceVersion = SourceVersion.latest

  override def process (annotations :Set[_ <: TypeElement], roundEnv :RoundEnvironment) = {
    if (!roundEnv.processingOver) {
      for (elem <- roundEnv.getRootElements) elem match {
        case telem :TypeElement => {
          val metas = _scanner.scanUnit(elem)
          if (!metas.isEmpty) generate(telem, metas)
        }
        case _ => processingEnv.getMessager.printMessage(
          Diagnostic.Kind.WARNING, "Weird element? " + elem.getClass)
      }
    }
    false
  }

  protected def generate (elem :TypeElement, metas :Seq[ClassMetadata]) {
    Generator.generate(_filer, elem, metas)
  }

  protected var _scanner :Scanner = _
  protected var _filer :Filer = _
}
