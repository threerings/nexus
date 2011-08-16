//
// $Id$

package com.threerings.nexus.streamergen;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.ElementScanner6;
import javax.lang.model.util.Types;

import com.google.common.collect.Maps;

/**
 * Does the actual AST walking and computes metadata that is used to generate {@code Streamer}
 * source files.
 */
public class Scanner extends ElementScanner6<Void, Void>
{
    public Scanner (ProcessingEnvironment env) {
        _msgr = env.getMessager();
        _types = env.getTypeUtils();
    }

    /**
     * Scans the supplied compilation unit and returns metadata for all encountered classes, mapped
     * by name.
     */
    public Map<Name,ClassMetadata> scanUnit (Element e) {
        Map<Name,ClassMetadata> result = (_map = Maps.newHashMap());
        scan(e);
        return result;
    }

    @Override
    public Void visitType (TypeElement e, Void p) {
        ClassMetadata meta = new ClassMetadata(e.getSuperclass(),
                                               e.getModifiers().contains(Modifier.ABSTRACT));
        for (TypeParameterElement tpe : e.getTypeParameters()) {
            meta.typeParams.put(tpe.getSimpleName(), tpe.asType());
        }
        _map.put(e.getSimpleName(), meta);
        _metas.push(meta);
        super.visitType(e, p);
        _metas.pop();
        System.err.println("Processed type " + e.getSimpleName() + " -> " + meta);
        return null;
    }

    @Override
    public Void visitExecutable (ExecutableElement e, Void p) {
        super.visitExecutable(e, p);
        if (e.getKind() == ElementKind.CONSTRUCTOR) {
            for (VariableElement arg : e.getParameters()) {
                _metas.peek().ctorArgs.put(arg.getSimpleName(), arg.asType());
            }
        }
        return null;
    }

    @Override
    public Void visitVariable (VariableElement e, Void p) {
        super.visitVariable(e, p);
        if (e.getKind() == ElementKind.FIELD) {
            System.err.println("Noting field " + e);
            _metas.peek().fields.put(e.getSimpleName(), e.asType());
        }
        return null;
    }

    protected final Messager _msgr;
    protected final Types _types;

    protected Deque<ClassMetadata> _metas = new ArrayDeque<ClassMetadata>();
    protected Map<Name,ClassMetadata> _map;
}
