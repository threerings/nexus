//
// $Id$

package com.threerings.nexus.streamergen

import scala.collection.JavaConversions._

import java.util.Set

import javax.annotation.processing.{AbstractProcessor, ProcessingEnvironment}
import javax.annotation.processing.{RoundEnvironment, SupportedAnnotationTypes}

import javax.lang.model.SourceVersion
import javax.lang.model.element.{Element, TypeElement}

/**
 * Generates {@code Streamer} implementations for {@link Streamable} classes.
 */
@SupportedAnnotationTypes(Array("*"))
class Processor extends AbstractProcessor {
  override def init (procenv :ProcessingEnvironment) {
    super.init(procenv)
    _scanner = new Scanner(procenv)
  }

  override def getSupportedSourceVersion :SourceVersion = SourceVersion.latest

  override def process (annotations :Set[_ <: TypeElement], roundEnv :RoundEnvironment) :Boolean = {
    if (!roundEnv.processingOver) {
      for (elem <- roundEnv.getRootElements) {
        _scanner.scanUnit(elem)
      }
    }
    false
  }

  protected var _scanner :Scanner = _
}
