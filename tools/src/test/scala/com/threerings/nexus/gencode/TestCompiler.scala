//
// $Id$

package com.threerings.nexus.gencode

import scala.collection.JavaConversions._

import java.net.URI
import javax.tools.{JavaFileObject, SimpleJavaFileObject, ToolProvider}

/**
 * Allows tests to run the code generating processor internally.
 */
abstract class TestCompiler {
  abstract class TestProcessor[R] extends Processor {
    def result :R
  }

  def process[R] (filename :String, content :String, proc :TestProcessor[R]) :R = {
    val files = stockObjects :+ mkTestObject(filename, content)
    val options = List("-proc:only")
    val task = ToolProvider.getSystemJavaCompiler.getTask(null, null, null, options, null, files)
    task.setProcessors(List(proc))
    task.call
    proc.result
  }

  protected def stockObjects :List[JavaFileObject]

  protected def mkTestObject (file :String, content :String) =
    new SimpleJavaFileObject(URI.create("test:/" + file), JavaFileObject.Kind.SOURCE) {
      override def getCharContent (ignoreEncodingErrors :Boolean) :CharSequence = content
    }
}
