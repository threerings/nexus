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
  def scanUnit (e :Element) :Seq[ClassMetadata] = {
    try {
      scan(e)
      _metas
    } finally {
      _metas = Seq()
    }
  }

  override def visitType (e :TypeElement, p :Unit) {
    val meta = new ClassMetadata(e)
    _mstack = meta :: _mstack
    super.visitType(e, p)
    _mstack = _mstack.tail

    // if this class was streamable, add it to the list of scanned metadatas; also check that we
    // extracted valid metadata
    if (e.getKind == ElementKind.CLASS && isStreamable(e)) {
      if (meta.unmatchedCtorArgs.isEmpty) _metas = _metas :+ meta
      else env.getMessager.printMessage(
        Diagnostic.Kind.ERROR, "Failed to match one or more ctor fields in " + meta.typ + ": " +
        meta.unmatchedCtorArgs.mkString(", "))
    }
  }

  override def visitExecutable (e :ExecutableElement, p :Unit) {
    super.visitExecutable(e, p)
    if (e.getKind == ElementKind.CONSTRUCTOR) {
      for (arg <- e.getParameters) {
        _mstack.head.ctorArgs += (arg.getSimpleName.toString -> arg.asType)
      }
    }
  }

  override def visitVariable (e :VariableElement, p :Unit) {
    super.visitVariable(e, p)
    if (e.getKind == ElementKind.FIELD) {
      _mstack.head.fields += (e.getSimpleName.toString -> e.asType)
    }
  }

  protected def isStreamable (e :TypeElement) :Boolean = {
    e.getInterfaces.exists(isStreamableIfc) || (e.getSuperclass match {
      case dt :DeclaredType => isStreamable(dt.asElement.asInstanceOf[TypeElement])
      case _ => false
    })
  }

  protected def isStreamableIfc (t :TypeMirror) :Boolean = t match {
    case dt :DeclaredType =>
      (Utils.qualifiedName(dt) == "com.threerings.nexus.io.Streamable" ||
       dt.asElement.asInstanceOf[TypeElement].getInterfaces.exists(isStreamableIfc))
    case _ => false
  }

  protected var _metas = Seq[ClassMetadata]()
  protected var _mstack :List[ClassMetadata] = Nil
}
