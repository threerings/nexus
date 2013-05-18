//
// Nexus Tools - code generators for Nexus framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.gencode

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

import java.util.{List => JList}
import javax.lang.model.element.{ExecutableElement, TypeElement, VariableElement}
import javax.lang.model.`type`.{DeclaredType, TypeKind}

/**
 * Contains metadata for a single `NexusService` interface.
 * @param elem the class's type element.
 */
class ServiceMetadata (val elem :TypeElement) extends Metadata {
  import ServiceMetadata._

  /** Returns the simple classname of this service. */
  def serviceName = elem.getSimpleName.toString

  /** Returns the imports needed by this class metadata. */
  def imports :Set[String] = Utils.collectImports(elem.asType) ++ methods.flatMap(_.imports)

  /** Returns all of the methods defined for this service. */
  def methods :JList[Method] = methodsBuf

  /** Adds a method to this metadata. Used when building. */
  def addMethod (elem :ExecutableElement) {
    methodsBuf += Method(elem)
  }
  private val methodsBuf = ListBuffer[Method]()

  override def toString () = String.format("[name=%s, methods=%s]", serviceName, methods)
}

object ServiceMetadata {
  case class Arg (elem :VariableElement, index :Int) {
    def name = elem.getSimpleName.toString
    def `type` = Utils.toString(elem.asType, true)
    def boxedType = Utils.toBoxedString(elem.asType, true)
    override def toString () = String.format("%s %s", `type`, name)
  }

  case class Method (elem :ExecutableElement) {
    private val _params = elem.getParameters.zipWithIndex.map(Arg.tupled)
    private val _rtype = elem.getReturnType

    /* sanity checks (runs in ctor) */ {
      // make sure return type is void or RFuture<T>
    }

    def name = elem.getSimpleName.toString
    val args :JList[Arg] = _params

    val hasResult = _rtype.getKind == TypeKind.DECLARED
    def result = Utils.toString(_rtype, true)
    def rtype = Utils.toString(_rtype.asInstanceOf[DeclaredType].getTypeArguments.get(0), true)

    /** Returns the imports needed by this method's parameters and return type. */
    def imports :Set[String] = (elem.getParameters.map(_.asType) :+ _rtype).flatMap(
      Utils.collectImports).toSet

    override def toString () = String.format("%s(%s)", name, _params.mkString(", "))
  }
}
