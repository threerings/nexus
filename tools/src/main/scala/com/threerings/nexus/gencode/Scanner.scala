//
// $Id$

package com.threerings.nexus.gencode

import scala.collection.JavaConversions._

import javax.annotation.processing.{Messager, ProcessingEnvironment}
import javax.tools.Diagnostic

import javax.lang.model.element.{Element, ElementKind, ExecutableElement}
import javax.lang.model.element.{Name, TypeElement, TypeParameterElement, VariableElement}
import javax.lang.model.`type`.{DeclaredType, TypeKind, TypeMirror, TypeVariable, NoType}
import javax.lang.model.util.{ElementScanner6, Types}

/**
 * Does the actual AST walking and computes metadata that is used to generate {@code Streamer}
 * and related source files.
 */
class Scanner (env :ProcessingEnvironment) extends ElementScanner6[Unit, Unit]
{
  /**
   * Scans the supplied compilation unit and returns metadata for all encountered classes, mapped
   * by name.
   */
  def scanUnit (e :Element) :Seq[Metadata] = {
    try {
      scan(e)
      _metas
    } finally {
      _metas = Seq()
    }
  }

  override def visitType (e :TypeElement, p :Unit) {
    val c = e.getKind match {
      case ElementKind.CLASS if (Utils.isStreamable(e.asType)) => new StreamableCollector(e)
      case ElementKind.INTERFACE if (Utils.isNexusService(e.asType)) => new ServiceCollector(e)
      case _ => DefaultCollector
    }
    _cstack = c :: _cstack
    super.visitType(e, p)
    _cstack = _cstack.tail
    c.validate(env.getMessager).map(m => _metas = _metas :+ m)
  }

  override def visitExecutable (e :ExecutableElement, p :Unit) {
    super.visitExecutable(e, p)
    _cstack.head.visitExecutable(e)
  }

  override def visitVariable (e :VariableElement, p :Unit) {
    super.visitVariable(e, p)
    _cstack.head.visitVariable(e)
  }

  protected trait Collector {
    def visitExecutable (e :ExecutableElement) :Unit
    def visitVariable (e :VariableElement) :Unit
    def validate (msg :Messager) :Option[Metadata]
  }

  protected object DefaultCollector extends Collector {
    def visitExecutable (e :ExecutableElement) {}
    def visitVariable (e :VariableElement) {}
    def validate (msg :Messager) = None
  }

  protected class StreamableCollector (e :TypeElement) extends Collector {
    val meta = new StreamableMetadata(e)

    def visitExecutable (e :ExecutableElement) {
      if (e.getKind == ElementKind.CONSTRUCTOR) {
        for (arg <- e.getParameters) {
          meta.ctorArgs += (arg.getSimpleName.toString -> arg.asType)
        }
      }
    }

    def visitVariable (e :VariableElement) {
      if (e.getKind == ElementKind.FIELD) {
        meta.fields += (e.getSimpleName.toString -> e.asType)
      }
    }

    def validate (msg :Messager) :Option[Metadata] = {
      // check that we extracted valid metadata
      if (meta.unmatchedCtorArgs.isEmpty) Some(meta)
      else {
        msg.printMessage(
          Diagnostic.Kind.ERROR, "Failed to match one or more ctor fields in " + meta.typ + ": " +
          meta.unmatchedCtorArgs.mkString(", "))
        None
      }
    }
  }

  protected class ServiceCollector (e :TypeElement) extends Collector {
    val meta = new ServiceMetadata(e)

    def visitExecutable (e :ExecutableElement) {
      if (e.getKind == ElementKind.METHOD) {
        meta.methods += ServiceMetadata.Method(e)
      }
    }

    def visitVariable (e :VariableElement) {
      // nada
    }

    def validate (msg :Messager) :Option[Metadata] = Some(meta)
  }

  protected var _metas = Seq[Metadata]()
  protected var _cstack :List[Collector] = Nil
}
