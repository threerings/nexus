//
// $Id$

package com.threerings.nexus.streamergen;

import scala.collection.JavaConversions._

import javax.annotation.processing.{Messager, ProcessingEnvironment}

import javax.lang.model.element.{Element, ElementKind, ExecutableElement, Modifier}
import javax.lang.model.element.{Name, TypeElement, TypeParameterElement, VariableElement}
import javax.lang.model.`type`.{DeclaredType, TypeKind, TypeMirror, TypeVariable}
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
  def scanUnit (e :Element) :Seq[ClassMetadata] = {
    try {
      scan(e)
      _metas
    } finally {
      _metas = Seq()
    }
  }

  override def visitType (e :TypeElement, p :Unit) {
    val meta = new ClassMetadata(e.getQualifiedName, e.getSimpleName, e.getSuperclass,
                                 getEncloser(e).getQualifiedName,
                                 e.getModifiers.contains(Modifier.ABSTRACT))
    e.getTypeParameters foreach { tpe => meta.typeParams.put(tpe.getSimpleName, tpe.asType) }
    // only add this class if it's streamable, but let it be processed regardless, as it may
    // contain nested types which are themselves streamable
    if (isStreamable(e)) _metas = _metas :+ meta
    _mstack = meta :: _mstack
    super.visitType(e, p)
    _mstack = _mstack.tail
    // System.err.println("Processed type " + e.getSimpleName + " -> " + meta)
  }

  override def visitExecutable (e :ExecutableElement, p :Unit) {
    super.visitExecutable(e, p)
    if (e.getKind == ElementKind.CONSTRUCTOR) {
      e.getParameters foreach { arg => _mstack.head.ctorArgs.put(arg.getSimpleName, arg.asType) }
    }
  }

  override def visitVariable (e :VariableElement, p :Unit) {
    super.visitVariable(e, p)
    if (e.getKind == ElementKind.FIELD) {
      // System.err.println("Noting field " + e)
      _mstack.head.fields.put(e.getSimpleName, e.asType)
    }
  }

  protected def isStreamable (e :TypeElement) :Boolean = {
    e.getInterfaces.exists(isStreamableIfc) || (e.getSuperclass match {
      case dt :DeclaredType => isStreamable(dt.asElement.asInstanceOf[TypeElement])
      case _ => false
    })
  }

  protected def isStreamableIfc (t :TypeMirror) = t match {
    case dt :DeclaredType => dt.asElement.asInstanceOf[TypeElement].getQualifiedName.toString ==
      "com.threerings.nexus.io.Streamable"
    case _ => false
  }

  protected def getEncloser (e :TypeElement) :TypeElement = e.getEnclosingElement match {
    case ee :TypeElement if (ee != null) => getEncloser(ee)
    case _ => e
  }

  protected val _msgr = env.getMessager
  protected val _types = env.getTypeUtils

  protected var _metas = Seq[ClassMetadata]()
  protected var _mstack :List[ClassMetadata] = Nil
}
