//
// $Id$

package com.threerings.nexus.streamergen

import scala.collection.JavaConversions._
import scala.collection.mutable.{Seq => MSeq}

import java.util.Set

import javax.annotation.processing.{AbstractProcessor, ProcessingEnvironment}
import javax.annotation.processing.{RoundEnvironment, SupportedAnnotationTypes}

import javax.lang.model.SourceVersion
import javax.lang.model.element.{Element, TypeElement, Name}

/**
 * Generates {@code Streamer} implementations for {@link Streamable} classes.
 */
@SupportedAnnotationTypes(Array("*"))
class Processor extends AbstractProcessor {
  /** A mapping of all scanned class metadata, valid after processor has run. */
  def metas :Seq[ClassMetadata] = _metas

  override def init (procenv :ProcessingEnvironment) {
    super.init(procenv)
    _scanner = new Scanner(procenv)
  }

  override def getSupportedSourceVersion :SourceVersion = SourceVersion.latest

  override def process (annotations :Set[_ <: TypeElement], roundEnv :RoundEnvironment) :Boolean = {
    if (!roundEnv.processingOver) {
      for (elem <- roundEnv.getRootElements) {
        _metas ++= _scanner.scanUnit(elem)
      }
    }
    false
  }

  protected var _scanner :Scanner = _
  protected var _metas = MSeq[ClassMetadata]()
}
