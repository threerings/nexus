//
// $Id$

package com.threerings.nexus.streamergen;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

/**
 * Generates {@code Streamer} implementations for {@link Streamable} classes.
 */
@SupportedAnnotationTypes("*")
public class Processor extends AbstractProcessor
{
    @Override public void init (ProcessingEnvironment procenv) {
        super.init(procenv);
        _scanner = new Scanner(procenv);
    }

    @Override public SourceVersion getSupportedSourceVersion () {
        return SourceVersion.latest();
    }

    @Override public boolean process (Set<? extends TypeElement> annotations,
                                      RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            for (Element elem : roundEnv.getRootElements()) {
                _scanner.scanUnit(elem);
            }
        }
        return false;
    }

    protected Scanner _scanner;
}
