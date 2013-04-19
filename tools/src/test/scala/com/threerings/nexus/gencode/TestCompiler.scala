//
// Nexus Tools - code generators for Nexus framework
// http://github.com/threerings/nexus/blob/master/LICENSE

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

  def process[R] (proc :TestProcessor[R], filename :String, content :String) :R = {
    process(proc, mkTestObject(filename, content))
  }

  def process[R] (proc :TestProcessor[R], testObjs :JavaFileObject*) :R = {
    val files = stockObjects ++ testObjs
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
