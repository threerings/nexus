//
// $Id$

package com.threerings.nexus.streamergen;

import java.util.List;
import java.util.Map;

import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.SimpleTypeVisitor6;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Contains metadata for a single {@code Streamable} class.
 */
public class ClassMetadata
{
    public static final TypeVisitor<Void,StringBuilder> TO_STRING =
        new SimpleTypeVisitor6<Void,StringBuilder>() {
        @Override public Void visitDeclared (DeclaredType t, StringBuilder buf) {
            if (t.getEnclosingType().getKind() != TypeKind.NONE) {
                visit(t.getEnclosingType(), buf);
                buf.append(".");
            }
            String cname = t.asElement().getSimpleName().toString();
            // if the name is empty, this is a union type bound and the bounds in question are the
            // supertype and interfaces implemented by this synthetic type; hack-a-saurus!
            if (cname.length() == 0) {
                TypeElement te = (TypeElement)t.asElement();
                visit(te.getSuperclass(), buf);
                for (TypeMirror ie : te.getInterfaces()) {
                    buf.append(" & ");
                    visit(ie, buf);
                }
            } else {
                buf.append(t.asElement().getSimpleName());
            }
            return null;
        }
        @Override public Void visitTypeVariable (TypeVariable t, StringBuilder buf) {
            if (t.getUpperBound().getKind() != TypeKind.NULL) {
                // TODO: check if upper bound is Object?
                buf.append(" extends ");
                visit(t.getUpperBound(), buf);
            } else if (t.getLowerBound().getKind() != TypeKind.NULL) {
                buf.append(" super ");
                visit(t.getUpperBound(), buf);
            }
            return null;
        }
        @Override public Void visitWildcard (WildcardType t, StringBuilder buf) {
            buf.append("?");
            if (t.getSuperBound() != null) {
                buf.append(" super ");
                visit(t.getSuperBound(), buf);
            } else if (t.getExtendsBound() != null) {
                buf.append(" extends ");
                visit(t.getExtendsBound(), buf);
            }
            return null;
        }
    };

    /** This class's parent type. */
    public final TypeMirror parent;

    /** Whether or not this type is abstract. */
    public final boolean isAbstract;

    /** An ordered mapping from type parameters to bounds. */
    public final Map<Name, TypeMirror> typeParams = Maps.newLinkedHashMap();

    /** An ordered mapping from constructor argument name to type. */
    public final Map<Name,TypeMirror> ctorArgs = Maps.newLinkedHashMap();

    /** An unordered mapping from field name to type. */
    public final Map<Name,TypeMirror> fields = Maps.newHashMap();

    /** A mapping from constructor arg name to field name. Computed by {@link #mapArgsToFields}. */
    public final Map<Name,Name> argToField = Maps.newHashMap();

    public ClassMetadata (TypeMirror parent, boolean isAbstract) {
        this.parent = parent;
        this.isAbstract = isAbstract;
    }

    @Override public String toString () {
        return "[parent=" + parent + ", isAbstract=" + isAbstract + ", typeParams=" + typeParams +
            ", ctorArgs=" + ctorArgs + ", fields=" + fields + ", argToField=" + argToField + "]";
    }
}
