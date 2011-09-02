//
// $Id$

package com.threerings.nexus.gencode

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

import java.lang.{Iterable => JIterable}
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
  val methods = ListBuffer[Method]()

  override def toString () = String.format("[name=%s, methods=%s]", serviceName, methods)
}

object ServiceMetadata {
  case class Arg (elem :VariableElement) {
    def `type` = Utils.toString(elem.asType, true)
    def name = elem.getSimpleName.toString
  }
  case class Method (elem :ExecutableElement) {
    val name = elem.getSimpleName.toString
    val args :JIterable[Arg] = elem.getParameters.map(Arg)
  }
}
