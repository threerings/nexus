//
// $Id$

package com.threerings.nexus.streamergen

import scala.collection.JavaConversions._

import javax.lang.model.element.TypeElement
import javax.lang.model.`type`.{DeclaredType, TypeKind, TypeVariable, WildcardType}
import javax.lang.model.util.SimpleTypeVisitor6

/**
 * Various utility bits.
 */
object Utils
{
  val ToString = new SimpleTypeVisitor6[Unit,StringBuilder] {
    override def visitDeclared (t :DeclaredType, buf :StringBuilder) {
      if (t.getEnclosingType.getKind != TypeKind.NONE) {
        visit(t.getEnclosingType, buf)
        buf.append(".")
      }
      val cname = t.asElement.getSimpleName.toString
      // if the name is empty, this is a union type bound and the bounds in question are the
      // supertype and interfaces implemented by this synthetic type hack-a-saurus!
      if (cname.length > 0) buf.append(cname)
      else {
        val te = t.asElement.asInstanceOf[TypeElement]
        visit(te.getSuperclass, buf)
        for (ie <- te.getInterfaces) {
          buf.append(" & ")
          visit(ie, buf)
        }
      }
    }

    override def visitTypeVariable (t :TypeVariable, buf :StringBuilder) {
      if (t.getUpperBound.getKind != TypeKind.NULL) {
        // TODO: check if upper bound is Object?
        buf.append(" extends ")
        visit(t.getUpperBound, buf)
      } else if (t.getLowerBound.getKind != TypeKind.NULL) {
        buf.append(" super ")
        visit(t.getUpperBound, buf)
      } // else nada?
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
  }
}
