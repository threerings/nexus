//
// Nexus Tools - code generators for Nexus framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.gencode

import scala.collection.JavaConversions._
import scala.collection.mutable.{Set => MSet}

import javax.lang.model.element.{Element, ExecutableElement, PackageElement, TypeElement}
import javax.lang.model.element.{Name, ElementKind}
import javax.lang.model.`type`.{ArrayType, DeclaredType, NoType, PrimitiveType, WildcardType}
import javax.lang.model.`type`.{TypeKind, TypeMirror, TypeVariable}
import javax.lang.model.util.{ElementScanner6, SimpleTypeVisitor6}

import com.threerings.nexus.io.{Streamable, Streamer}
import com.threerings.nexus.distrib.{DService, NexusObject, NexusService}

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
  def toString (t :TypeMirror, boundVars :Boolean) :String = {
    val buf = new StringBuilder
    new ToString(boundVars).visit(t, buf)
    buf.toString
  }

  /**
   * Returns a Java source string describing the supplied type, promoting primitive types to their
   * boxed counterparts. Handles type parameters, type bounds and wildcards.
   *
   * @param boundVars whether or not to print type variable bounds.
   */
  def toBoxedString (t :TypeMirror, boundVars :Boolean) :String = t.getKind match {
    case TypeKind.BOOLEAN => "Boolean"
    case TypeKind.BYTE => "Byte"
    case TypeKind.CHAR => "Character"
    case TypeKind.SHORT => "Short"
    case TypeKind.INT => "Integer"
    case TypeKind.LONG => "Long"
    case TypeKind.FLOAT => "Float"
    case TypeKind.DOUBLE => "Double"
    case _ => toString(t, true)
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
   * Returns the name of the supplied element, qualified by its enclosing types, if any.
   */
  def enclosedName (elem :Element) :String = {
    def addName (e :Element, buf :StringBuilder) :StringBuilder = e match {
      case te :TypeElement => {
        addName(te.getEnclosingElement, buf)
        if (buf.length > 0) buf.append(".")
        buf.append(te.getSimpleName)
      }
      case pe :PackageElement => buf // stop when we hit the package
      case _ => throw new IllegalArgumentException(
        "Unexpected type when generating enclosedName [elem=" + elem + ", kind=" + elem.getKind +
        ", encl=" + e + ", kind=" + e.getKind + "]")
    }
    addName(elem, new StringBuilder).toString
  }

  /**
   * Returns true if the supplied type extends, or is, the specified class.
   */
  def extendsClass (fqName :String, t :TypeMirror) :Boolean = t match {
    case dt :DeclaredType => (qualifiedName(dt) == fqName || extendsClass(
      fqName, dt.asElement.asInstanceOf[TypeElement].getSuperclass))
    case _ => false
  }

  /**
   * Returns true if the supplied type implements, or is, the specified interface.
   */
  def implementsIface (fqName :String, t :TypeMirror) :Boolean = t match {
    case dt :DeclaredType => (qualifiedName(dt) == fqName) || {
      val et = dt.asElement.asInstanceOf[TypeElement]
      implementsIface(fqName, et.getSuperclass) ||
        et.getInterfaces.exists(implementsIface(fqName, _))
    }
    case _ => false
  }

  def isNexusObject (t :TypeMirror) = extendsClass(NexusObjectName, t)
  def isDService (t :TypeMirror) = (extendsClass(DServiceName, t) ||
    implementsIface(DServiceFactoryName, t))
  def isNexusService (t :TypeMirror) = implementsIface(NexusServiceName, t)
  def isStreamable (t :TypeMirror) = implementsIface(StreamableName, t)
  def isEnum (t :TypeMirror) = extendsClass(EnumName, t)

  /**
   * Returns the fqName of the supplied type's parent `Streamer`, if one exists.
   */
  def getParentStreamer (te :TypeElement) :Option[String] = te.getSuperclass match {
    case dt :DeclaredType if (!isStreamable(dt) || qualifiedName(dt) == NexusObjectName) => None
    case dt :DeclaredType => Some(getStreamerName(dt.asElement))
    case _ => None
  }

  /**
   * Returns the fqStreamerNAme for the supplied type element. For example: `Streamer_Foo.Bar` for
   * an element `Foo.Bar`.
   */
  def getStreamerName (elem :Element) = {
    def addName (e :Element, buf :StringBuilder) :StringBuilder = e match {
      case te :TypeElement => {
        addName(te.getEnclosingElement, buf)
        if (buf.length > 0) buf.append(".")
        // if we're enclosed by a package, the buck stops here
        if (te.getEnclosingElement.isInstanceOf[PackageElement]) buf.append("Streamer_");
        buf.append(te.getSimpleName)
      }
      case pe :PackageElement => buf.append(pe.getQualifiedName.toString) // we're done
      case _ => throw new IllegalArgumentException(
        "Unexpected type when generating streamer name [elem=" + elem + ", kind=" + elem.getKind +
        ", encl=" + e + ", kind=" + e.getKind + "]")
    }
    addName(elem, new StringBuilder).toString
  }

  def directlyExtendsNexusObject (t :TypeMirror) = t match {
    case dt :DeclaredType => {
      // the supertype of a declared type is always another declared type
      val sdt = dt.asElement.asInstanceOf[TypeElement].getSuperclass.asInstanceOf[DeclaredType]
      (qualifiedName(sdt) == NexusObjectName)
    }
    case _ => false
  }

  /**
   * Returns a string that can be appended to `in.read` or `out.write` to generate the appropriate
   * read or write call for the supplied type.
   */
  def fieldKind (field :TypeMirror) :String = field.getKind match {
    case TypeKind.BOOLEAN => "Boolean"
    case TypeKind.BYTE => "Byte"
    case TypeKind.CHAR => "Char"
    case TypeKind.SHORT => "Short"
    case TypeKind.INT => "Int"
    case TypeKind.LONG => "Long"
    case TypeKind.FLOAT => "Float"
    case TypeKind.DOUBLE => "Double"
    case TypeKind.ARRAY => field.asInstanceOf[ArrayType].getComponentType match {
      case pt :PrimitiveType => fieldKind(pt) + "s"
      case dt :DeclaredType if (isLang(field, "String")) => return fieldKind(dt) + "s"
      case ct => throw new IllegalArgumentException("Arrays of " + ct + " not supported")
    }
    case TypeKind.DECLARED if (isLang(field, "String")) => "String"
    case TypeKind.DECLARED if (isLang(field, "Class")) => "Class"
    case TypeKind.DECLARED if (isDService(field)) => "Service"
    case TypeKind.DECLARED if (isEnum(field)) => "Enum"
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
    case TypeKind.DECLARED if (isEnum(field)) => ""
    case TypeKind.DECLARED if (isLang(field, "Class") || isDService(field)) =>
      "<" + toString(field.asInstanceOf[DeclaredType].getTypeArguments.get(0), false) + ">"
    case TypeKind.DECLARED => "<" + toString(field, false) + ">"
    case TypeKind.TYPEVAR => "<" + toString(field, false) + ">"
    case _ => ""
  }

  /**
   * Returns the names of the arguments to the element's supertype's constructor. The supertype
   * must declare only one constructor (all `Streamable` types must have only one constructor).
   */
  def getSuperCtorArgs (e :TypeElement) :Seq[String] = e.getSuperclass match {
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
      ctors.size match {
        case 1 => ctors.head.getParameters.toSeq.map(_.getSimpleName.toString)
        case 0 => Seq()
        case _ => throw new IllegalArgumentException(
          "Supertype has multiple ctors! " + e + ": " + ctors.mkString(", "))
      }
    }
    case _ :NoType => Seq() // Object's "supertype" takes zero arguments
  }

  /**
   * Returns the qualified names of all outermost types that appear in the supplied type (java.lang
   * types are omitted).
   */
  def collectImports (t :TypeMirror) :Set[String] = {
    val imports = MSet[String]()
    new ImportCollector().visit(t, imports)
    imports.toSet
  }

  /**
   * Returns true if the supplied fully-qualified class name is in the supplied package. Note: this
   * only works on outermost types, a fully-qualified nested type will return invalid values.
   */
  def inPackage (fqName :String, pkgName :String) =
    fqName.substring(0, fqName.lastIndexOf(".")+1) == (pkgName + ".")

  private class ToString (boundVars :Boolean) extends SimpleTypeVisitor6[Unit,StringBuilder] {
    override def visitDeclared (t :DeclaredType, buf :StringBuilder) {
      val te = t.asElement.asInstanceOf[TypeElement]

      // if the name is empty, this is a union type bound and the bounds in question are the
      // supertype and interfaces implemented by this synthetic type hack-a-saurus!
      val cname = t.asElement.getSimpleName.toString
      if (cname.isEmpty) {
        // we don't do the enclosing elem check because union type bounds have a spurious encloser
        visit(te.getSuperclass, buf)
        for (ie <- te.getInterfaces) {
          buf.append(" & ")
          visit(ie, buf)
        }

      } else {
        // if we have an enclosing element, prepend it (and any of its enclosing elements);
        // we don't want the type parameters of the enclosers (because streamables can never be
        // non-static inner classes), so we just climb the element hierarchy
        val encl = te.getEnclosingElement
        if (encl != null && encl.getKind != ElementKind.PACKAGE) {
          buf.append(enclosedName(encl)).append(".");
        }

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

    override def visitPrimitive (t :PrimitiveType, buf :StringBuilder) {
      // this is a bit of a hack, we can fix it up later if need be
      buf.append(t.getKind.toString.toLowerCase)
    }

    private var _seenVars = Set[TypeVariable]()
  }

  private class ImportCollector extends SimpleTypeVisitor6[Unit,MSet[String]] {
    override def visitDeclared (t :DeclaredType, imports :MSet[String]) {
      val te = t.asElement.asInstanceOf[TypeElement]

      // if the name is empty, this is a union type bound and the bounds in question are the
      // supertype and interfaces implemented by this synthetic type hack-a-saurus!
      val cname = t.asElement.getSimpleName.toString
      if (cname.isEmpty) {
        visit(te.getSuperclass, imports)
        for (ie <- te.getInterfaces) {
          visit(ie, imports)
        }

      } else {
        // if we have an enclosing element, we'll be importing that rather than this type directly,
        // as we render all types qualified by their enclosing classes
        val encl = te.getEnclosingElement
        if (encl != null && encl.getKind != ElementKind.PACKAGE) {
          // we need to note that we're processing an outer type, because we don't want to process
          // the type variables of the outer type as those will never appear in the fully qualified
          // expression of the inner type in question
          _outerNest += 1
          visit(encl.asType, imports)
          _outerNest -= 1
        }
        // otherwise we (potentially) need to import this type
        else {
          val fqName = te.getQualifiedName.toString
          if (!inPackage(fqName, "java.lang")) imports += fqName
        }
        // visit our type variables if we're processing the inner-most type
        if (_outerNest == 0) t.getTypeArguments foreach { ta => visit(ta, imports) }
      }
    }

    override def visitTypeVariable (t :TypeVariable, imports :MSet[String]) {
      // we may encounter cycles in the type graph where the same type variable appears multiple
      // times, as in "T extends Comparable<T>"; in such cases, we need to avoid infinite loops
      if (!_seenVars(t)) {
        _seenVars += t
        if (t.getUpperBound.getKind != TypeKind.NULL) {
          if (!isLang(t.getUpperBound, "Object")) {
            visit(t.getUpperBound, imports)
          }
        } else if (t.getLowerBound.getKind != TypeKind.NULL) {
          visit(t.getLowerBound, imports)
        }
      }
    }

    override def visitWildcard (t :WildcardType, imports :MSet[String]) {
      if (t.getSuperBound != null) {
        visit(t.getSuperBound, imports)
      } else if (t.getExtendsBound != null) {
        visit(t.getExtendsBound, imports)
      }
    }

    private var _seenVars = Set[TypeVariable]()
    private var _outerNest = 0
  }

  private[gencode] final val NexusObjectName = classOf[NexusObject].getName
  private[gencode] final val NexusServiceName = classOf[NexusService].getName
  private[gencode] final val DServiceName = classOf[DService[_]].getName
  private[gencode] final val DServiceFactoryName =
    classOf[DService.Factory[_]].getName.replace('$', '.')
  private[gencode] final val StreamableName = classOf[Streamable].getName
  private[gencode] final val StreamerName = classOf[Streamer[_]].getName
  private[gencode] final val EnumName = classOf[Enum[_]].getName
}
