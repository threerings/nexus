//
// Nexus Tools - code generators for Nexus framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.gencode

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

import java.util.{List => JList}
import javax.lang.model.element.{ExecutableElement, TypeElement, VariableElement}

/**
 * Contains metadata for a single `NexusService` interface.
 * @param elem the class's type element.
 */
class ServiceMetadata (val elem :TypeElement) extends Metadata {
  import ServiceMetadata._

  /** Returns the simple classname of this service. */
  def serviceName = elem.getSimpleName.toString

  /** Returns the imports needed by this class metadata. */
  def imports :Set[String] = Utils.collectImports(elem.asType) ++
    methods.flatMap(_.elem.getParameters.flatMap(p => Utils.collectImports(p.asType)))

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

    /*sanity checks (runs in ctor)*/ {
      val cbs = _params filter(a => a.`type` == "Callback" || (a.`type` startsWith "Callback"))
      // ensure that we have exactly zero or one callback arguments
      if (cbs.size > 1) throw new Generator.InvalidCodeException(
        this + " has more than one Callback argument")
      // ensure that we don't have a callback in the wrong position
      if (cbs.size == 1 && _params.last != cbs(0)) throw new Generator.InvalidCodeException(
        this + " Callback argument in non-final position")
    }

    val name = elem.getSimpleName.toString
    val args :JList[Arg] = _params
    override def toString () = String.format("%s(%s)", name, _params.mkString(", "))
  }
}
