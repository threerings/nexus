//
// $Id$

package com.threerings.nexus.streamergen

import scala.collection.mutable.{Map => MMap, LinkedHashMap => LHMap}

import javax.lang.model.element.Name
import javax.lang.model.`type`.TypeMirror

/**
 * Contains metadata for a single {@code Streamable} class.
 */
class ClassMetadata (
  /** This class's fully qualified name. */
  val fqName :Name,

  /** This class's simple name. */
  val name :Name,

  /** This class's parent type. */
  val parent :TypeMirror,

  /** The fqName of the outermost enclosing class (which may be the class itself). */
  val encloser :Name,

  /** Whether or not this type is abstract. */
  val isAbstract :Boolean)
{
  /** An ordered mapping from type parameters to bounds. */
  var typeParams = LHMap[Name,TypeMirror]()

  /** An ordered mapping from constructor argument name to type. */
  var ctorArgs =  LHMap[Name,TypeMirror]()

  /** An unordered mapping from field name to type. */
  val fields = MMap[Name,TypeMirror]()

  /** A mapping from constructor arg name to field name. Computed by {@link #mapArgsToFields}. */
  val argToField = MMap[Name,Name]()

  override def toString () = {
    "[name=" + name + ", parent=" + parent + ", encloser=" + encloser +
    ", isAbstract=" + isAbstract + ", typeParams=" + typeParams + ", ctorArgs=" + ctorArgs +
    ", fields=" + fields + ", argToField=" + argToField + "]"
  }
}
