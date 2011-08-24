//
// $Id$

package com.threerings.nexus.streamergen

import scala.collection.JavaConversions._

import java.net.URI

import javax.tools.{JavaFileObject, SimpleJavaFileObject, ToolProvider}

import org.junit.Assert._
import org.junit.Test

/**
 * Tests the streamer generator.
 */
class StreamerGenTest
{
  @Test def testBox {
    TestCompiler.process("Box.java", """
      public class Box<T> implements com.threerings.nexus.io.Streamable {
        public final T value;
        public Box (T value) {
          this.value = value;
        }
      }
      """)
  }

  @Test def testNested {
    TestCompiler.process("Outer.java", """
      public class Outer {
        public class Inner implements com.threerings.nexus.io.Streamable {}
      }
      """)
  }
}

object TestCompiler {
  val streamObj = mkTestObject("Streamable.java", """
    package com.threerings.nexus.io;
    public interface Streamable {}
  """)

  def process (filename :String, content :String) :Unit = {
    val files = List(streamObj, mkTestObject(filename, content))
    val options = List("-processor", "com.threerings.nexus.streamergen.Processor", "-proc:only")
    val task = _compiler.getTask(null, null, null, options, null, files)
    val proc = new Processor
    task.setProcessors(List(proc))
    task.call
    proc.metas foreach { case (k, v) =>
      System.err.println(k + " -> " + v)
    }
  }

  private def mkTestObject (file :String, content :String) =
    new SimpleJavaFileObject(URI.create("test:/" + file), JavaFileObject.Kind.SOURCE) {
      override def getCharContent (ignoreEncodingErrors :Boolean) :CharSequence = content
    }

  private val _compiler = ToolProvider.getSystemJavaCompiler
}
