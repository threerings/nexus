//
// $Id$

package com.threerings.nexus.gencode

import scala.collection.JavaConversions._

import javax.annotation.processing.{Messager, ProcessingEnvironment}
import javax.tools.Diagnostic

import javax.lang.model.element.{Element, ElementKind, ExecutableElement, Modifier}
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
        meta.ctorArgs ++= e.getParameters.map(a => (a.getSimpleName.toString -> a.asType))
      }
    }

    def visitVariable (e :VariableElement) {
      if (e.getKind == ElementKind.FIELD && !e.getModifiers.contains(Modifier.STATIC)) {
        meta.fields += (e.getSimpleName.toString -> e.asType)
      }
    }

    def validate (msg :Messager) :Option[Metadata] = {
      // if we're processing NexusObject itself, generate no streamer
      if (meta.elem.getQualifiedName.toString == Utils.NexusObjectName ||
          // skip private streamables (which could never be instantiated by a streamer and thus
          // probably exist only to assuage the type system in some circumstance)
          meta.elem.getModifiers.contains(Modifier.PRIVATE)) None
      // make sure our super constructor's arguments are first in our argument list
      else if (!meta.ctorArgs.keys.toSeq.startsWith(meta.superCtorArgs)) {
        msg.printMessage(
          Diagnostic.Kind.ERROR, "Arguments passed to super() must come first in argument list: " +
          meta.typ + ": " + meta.superCtorArgs.mkString(", "))
        None
      }
      // check that we extracted valid metadata
      else if (meta.unmatchedCtorArgs.isEmpty) Some(meta)
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
        meta.addMethod(e)
      }
    }

    def visitVariable (e :VariableElement) {
      // nada
    }

    def validate (msg :Messager) :Option[Metadata] = {
      // if we're processing NexusService itself, generate no factory
      if (meta.elem.getQualifiedName.toString == Utils.NexusServiceName) None
      else Some(meta)
    }
  }

  protected var _metas = Seq[Metadata]()
  protected var _cstack :List[Collector] = Nil
}
