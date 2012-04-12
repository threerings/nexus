//
// $Id$

package com.threerings.nexus.gencode

import scala.collection.JavaConversions._
import scala.collection.mutable.{Map => MMap, LinkedHashMap => LHMap, Set => MSet}

import java.lang.{Iterable => JIterable}
import javax.lang.model.element.{Modifier, Name, TypeElement}
import javax.lang.model.`type`.{DeclaredType, TypeMirror}

/**
 * Contains metadata for a single `Streamable` class.
 * @param elem the class's type element.
 */
class StreamableMetadata (val elem :TypeElement) extends Metadata {
  /** The names of the args to our supertype constructor. */
  lazy val superCtorArgs :Seq[String] = Utils.getSuperCtorArgs(elem)

  /** An ordered mapping from constructor argument name to type. */
  val ctorArgs = LHMap[String,TypeMirror]()

  /** The constructor arguments with the first `superCtorArgs` arguments dropped.
   * These arguments should correspond to fields declared by this class. */
  lazy val localCtorArgs :Seq[String] = ctorArgs.keys.toSeq.drop(superCtorArgs.size)

  /** An unordered mapping from field name to type. Includes supertype fields. */
  val fields = MMap[String,TypeMirror]()

  /** A mapping from constructor arg name to field name. */
  lazy val argToField :Map[String,String] = (for {
    field <- fields keys;
    arg <- variants(field) find(ctorArgs.contains)
  } yield (arg -> field)) toMap

  /** Returns info on any unmatched constructor args. */
  lazy val unmatchedCtorArgs :Seq[String] =
    (localCtorArgs.toSet -- argToField.keySet) map { n => ctorArgs(n) + " " + n } toSeq

  /** This class's type. */
  def typ = elem.asType.asInstanceOf[DeclaredType]

  /** This class's simple name. */
  def name = elem.getSimpleName.toString

  /** Whether or not this type is abstract. */
  def isAbstract = elem.getModifiers.contains(Modifier.ABSTRACT)

  /** Whether or not this is metadata for a `NexusObject` derivative. */
  def isNexusObject = Utils.isNexusObject(elem.asType)

  /** Returns the imports needed by this class metadata. */
  def imports :Set[String] = {
    val self = Utils.collectImports(elem.asType)
    val pstr = Utils.getParentStreamer(elem)
    val cimps = ctorArgs.values.map(Utils.collectImports)
    ((self ++ pstr) /: cimps) { _ ++ _ } // look Ma, it's like APL!
  }

  /** Returns the name of our parent class, including enclosing classes, not including type
   * parameters. For example: `Outer.Parent`. */
  def parentEnclosedName =
    Utils.enclosedName(elem.getSuperclass.asInstanceOf[DeclaredType].asElement)

  /** Returns the type name for this class (including type parameters without bounds). */
  def typeUse = Utils.toString(typ, false)

  /** Returns just this type's type parameters (including type bounds). */
  def typeBounds =
    if (typ.getTypeArguments.isEmpty) ""
    else typ.getTypeArguments.map(ta => Utils.toString(ta, true)).mkString("<", ",", ">")

  /** The field names in the order defined by their corresponding constructor argument. */
  def orderedFieldNames :Seq[String] = localCtorArgs map(argToField)

  /** Whether or not this class needs to call `super.writeObject` (used in template). */
  def hasSuperWrite = ctorArgs.size > fields.size

  /** Returns a list of objects used by the template to format the field writes. */
  def writes :JIterable[AnyRef] = orderedFieldNames map(f => fieldToWrite(f, fields(f)))

  /** Returns a list of objects used by the template to format the field reads. */
  def reads :JIterable[AnyRef] = ctorArgs.values.toSeq map(fieldToRead)

  override def toString () = String.format(
    "[type=%s, ctorArgs=%s, fields=%s]", typ, ctorArgs, fields)

  private def fieldToWrite (name :String, field :TypeMirror) = new AnyRef {
    val fname = name
    val fkind = Utils.fieldKind(field)
  }

  private def fieldToRead (field :TypeMirror) = new AnyRef {
    val fkind = Utils.fieldKind(field)
    val vtype = Utils.valueType(field)
    val readArgs = if (fkind != "Enum") ""
                   else Utils.toString(field, false) + ".class"
  }

  /** Generates all variants of a field name that we allow as a matching constructor arg. */
  private def variants (field :String) = Iterator(
    field,                      // the field name itself
    field.replaceAll("^_", ""), // the field name minus leading _
    field.replaceAll("_$", ""), // the field name minus trailing _
    "_" + field,                // the field name plus leading _
    field + "_"                 // the field name plus trailing _
  )
}
