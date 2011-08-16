//
// $Id$

package com.threerings.nexus.streamergen;

import scala.collection.JavaConversions._

import javax.annotation.processing.{Messager, ProcessingEnvironment}

import javax.lang.model.element.{Element, ElementKind, ExecutableElement, Modifier}
import javax.lang.model.element.{Name, TypeElement, TypeParameterElement, VariableElement}
import javax.lang.model.`type`.{TypeKind, TypeMirror, TypeVariable}
import javax.lang.model.util.{ElementScanner6, Types}

/**
 * Does the actual AST walking and computes metadata that is used to generate {@code Streamer}
 * source files.
 */
class Scanner (env :ProcessingEnvironment) extends ElementScanner6[Unit, Unit]
{
  /**
   * Scans the supplied compilation unit and returns metadata for all encountered classes, mapped
   * by name.
   */
  def scanUnit (e :Element) :Map[Name,ClassMetadata] = {
    _map = Map()
    try {
      scan(e)
      _map
    } finally {
      _map = null
    }
  }

  override def visitType (e :TypeElement, p :Unit) {
    val meta = new ClassMetadata(e.getSuperclass, e.getModifiers.contains(Modifier.ABSTRACT))
    e.getTypeParameters foreach { tpe => meta.typeParams.put(tpe.getSimpleName, tpe.asType) }
    _map += (e.getSimpleName -> meta)
    _metas = meta :: _metas
    super.visitType(e, p)
    _metas = _metas.tail
    System.err.println("Processed type " + e.getSimpleName + " -> " + meta)
  }

  override def visitExecutable (e :ExecutableElement, p :Unit) {
    super.visitExecutable(e, p)
    if (e.getKind == ElementKind.CONSTRUCTOR) {
      e.getParameters foreach { arg => _metas.head.ctorArgs.put(arg.getSimpleName, arg.asType) }
    }
  }

  override def visitVariable (e :VariableElement, p :Unit) {
    super.visitVariable(e, p)
    if (e.getKind == ElementKind.FIELD) {
      System.err.println("Noting field " + e)
      _metas.head.fields.put(e.getSimpleName, e.asType)
    }
  }

  protected val _msgr = env.getMessager
  protected val _types = env.getTypeUtils

  protected var _metas :List[ClassMetadata] = Nil
  protected var _map :Map[Name,ClassMetadata] = _
}
