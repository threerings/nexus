//
// $Id$

package com.threerings.nexus.streamergen

import scala.collection.JavaConversions._

import javax.lang.model.element.{TypeElement, ExecutableElement, ElementKind}
import javax.lang.model.`type`.{DeclaredType, NoType, WildcardType}
import javax.lang.model.`type`.{TypeKind, TypeVariable, TypeMirror}
import javax.lang.model.util.{ElementScanner6, SimpleTypeVisitor6}

/**
 * Various utility bits.
 */
object Utils
{
  /**
   * Returns a Java source string describing the supplied type. Handles type parameters, type
   * bounds and wildcards.
   *
   * @param boundVars whether or not to print type variable bounds.
   */
  def toString (elem :TypeMirror, boundVars :Boolean) :String = {
    val buf = new StringBuilder
    new ToString(boundVars).visit(elem, buf)
    buf.toString
  }

  /**
   * Returns true if the supplied type mirror represents `java.lang.name`.
   */
  def isLang (t :TypeMirror, name :String) = t match {
    case dt :DeclaredType => (qualifiedName(dt) == "java.lang." + name)
    case _ => false
  }

  /**
   * Returns the qualified name of the supplied class type.
   */
  def qualifiedName (dt :DeclaredType) =
    dt.asElement.asInstanceOf[TypeElement].getQualifiedName.toString

  /**
   * Returns a string that can be appended to `in.read` or `out.write` to generate the appropriate
   * read or write call for the supplied type.
   */
  def fieldKind (field :TypeMirror) = field.getKind match {
    case TypeKind.BOOLEAN => "Boolean"
    case TypeKind.BYTE => "Byte"
    case TypeKind.CHAR => "Char"
    case TypeKind.SHORT => "Short"
    case TypeKind.INT => "Int"
    case TypeKind.LONG => "Long"
    case TypeKind.FLOAT => "Float"
    case TypeKind.DOUBLE => "Double"
    case TypeKind.DECLARED if (isLang(field, "String")) => "String"
    case TypeKind.DECLARED if (isLang(field, "Class")) => "Class"
    case TypeKind.DECLARED => "Value"
    case TypeKind.TYPEVAR => "Value"
    case _ => throw new IllegalArgumentException(
      "Encountered unknown type kind " + field.getClass.getName)
  }

  /**
   * Returns the type parameters to be prepented to `readValue` or `readClass` when reading the
   * supplied type, or be the empty string if the supplied type is not read via `readValue` or
   * `readClass`.
   */
  def valueType (field :TypeMirror) = field.getKind match {
    case TypeKind.DECLARED if (isLang(field, "String")) => ""
    case TypeKind.DECLARED if (isLang(field, "Class")) =>
      "<" + toString(field.asInstanceOf[DeclaredType].getTypeArguments.get(0), false) + ">"
    case TypeKind.DECLARED => "<" + toString(field, false) + ">"
    case TypeKind.TYPEVAR => "<" + toString(field, false) + ">"
    case _ => ""
  }

  /**
   * Returns the number of arguments taken by the supplied type element's supertype. The supertype
   * must declare only one constructor (all `Streamable` types must have only one constructor).
   */
  def countSuperCtorArgs (e :TypeElement) = e.getSuperclass match {
    case stype :DeclaredType => {
      var ctors :List[ExecutableElement] = Nil
      val se = stype.asElement
      new ElementScanner6[Unit, Unit] {
        override def visitType (ne :TypeElement, p :Unit) {
          // don't descend into nested types
          if (se == ne) super.visitType(ne, p)
        }
        override def visitExecutable (e :ExecutableElement, p :Unit) {
          super.visitExecutable(e, p)
          if (e.getKind == ElementKind.CONSTRUCTOR) ctors = e :: ctors
        }
      }.scan(se)
      if (ctors.size > 1) throw new IllegalArgumentException(
        "Supertype has multiple ctors! " + e + ": " + ctors.mkString(", "))
      if (ctors.isEmpty) 0 else ctors.head.getParameters.size
    }
    case _ :NoType => 0 // Object's "supertype" takes zero arguments
  }

  private class ToString (boundVars :Boolean) extends SimpleTypeVisitor6[Unit,StringBuilder] {
    override def visitDeclared (t :DeclaredType, buf :StringBuilder) {
      if (t.getEnclosingType.getKind != TypeKind.NONE) {
        visit(t.getEnclosingType, buf)
        buf.append(".")
      }
      val cname = t.asElement.getSimpleName.toString
      // if the name is empty, this is a union type bound and the bounds in question are the
      // supertype and interfaces implemented by this synthetic type hack-a-saurus!
      if (cname.length > 0) {
        buf.append(cname)
        val tas = t.getTypeArguments
        if (!tas.isEmpty) {
          buf.append("<")
          visit(tas.get(0), buf)
          tas.tail foreach { ta =>
              buf.append(",")
              visit(ta, buf)
          }
          buf.append(">")
        }
      } else {
        val te = t.asElement.asInstanceOf[TypeElement]
        visit(te.getSuperclass, buf)
        for (ie <- te.getInterfaces) {
          buf.append(" & ")
          visit(ie, buf)
        }
      }
    }

    override def visitTypeVariable (t :TypeVariable, buf :StringBuilder) {
      buf.append(t.asElement.getSimpleName)
      // we may encounter cycles in the type graph where the same type variable appears multiple
      // times, as in "T extends Comparable<T>"; in such cases, we only want to print the bounds
      // the first time the variable is encountered
      if (boundVars && !_seenVars(t)) {
        _seenVars += t
        if (t.getUpperBound.getKind != TypeKind.NULL) {
          if (!isLang(t.getUpperBound, "Object")) {
            buf.append(" extends ")
            visit(t.getUpperBound, buf)
          }
        } else if (t.getLowerBound.getKind != TypeKind.NULL) {
          buf.append(" super ")
          visit(t.getUpperBound, buf)
        } // else nada?
      }
    }

    override def visitWildcard (t :WildcardType, buf :StringBuilder) {
      buf.append("?")
      if (t.getSuperBound != null) {
        buf.append(" super ")
        visit(t.getSuperBound, buf)
      } else if (t.getExtendsBound != null) {
        buf.append(" extends ")
        visit(t.getExtendsBound, buf)
      }
    }

    private var _seenVars = Set[TypeVariable]()
  }
}
