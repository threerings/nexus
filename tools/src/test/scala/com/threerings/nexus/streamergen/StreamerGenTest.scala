//
// $Id$

package com.threerings.nexus.streamergen

import scala.collection.JavaConversions._
import scala.collection.mutable.{Seq => MSeq}

import java.io.StringWriter
import java.net.URI

import javax.annotation.processing.SupportedAnnotationTypes
import javax.lang.model.element.TypeElement
import javax.tools.{JavaFileObject, SimpleJavaFileObject, ToolProvider}

import org.junit.Assert._
import org.junit.Test

/**
 * Tests the streamer generator.
 */
class StreamerGenTest
{
  @Test def testStreamerName {
    assertEquals("Streamer_Foo", Generator.streamerName("Foo"))
    assertEquals("foo.bar.Streamer_Foo", Generator.streamerName("foo.bar.Foo"))
  }

  @Test def testBox {
    val metas = TestCompiler.genMetas("Box.java", """
      public class Box<T> implements com.threerings.nexus.io.Streamable {
        public final T value;
        public Box (T value) {
          this.value = value;
        }
      }
      """)
  }

  @Test def testNested {
    val source = TestCompiler.genSource("Container.java", """
      package foo.bar;
      public class Container {
        public class Inner1 implements com.threerings.nexus.io.Streamable {}
        public class Inner2 implements com.threerings.nexus.io.Streamable {}
      }
      """)
    System.err.println(source)
  }

  @Test def testNestedInStreamable {
    val metas = TestCompiler.genMetas("Outer.java", """
      package foo.bar;
      import com.threerings.nexus.io.Streamable;
      public class Outer implements Streamable {
        public class Inner1 implements Streamable {}
        public class Inner2 implements Streamable {}
      }
      """)
    metas foreach System.err.println
  }
}

object TestCompiler {
  val streamObj = mkTestObject("Streamable.java", """
    package com.threerings.nexus.io;
    public interface Streamable {}
  """)

  def genSource (filename :String, content :String) :String =
    process(filename, content, new GenSourceProcessor)

  @SupportedAnnotationTypes(Array("*"))
  class GenSourceProcessor extends TestProcessor[String] {
    override def result = _source
    override protected def generate (elem :TypeElement, metas :Seq[ClassMetadata]) {
      val out = new StringWriter
      Generator.generate(elem, metas, out)
      _source = out.toString
    }
    protected var _source = ""
  }

  def genMetas (filename :String, content :String) :Seq[ClassMetadata] =
    process(filename, content, new GenMetasProcessor)

  @SupportedAnnotationTypes(Array("*"))
  class GenMetasProcessor extends TestProcessor[Seq[ClassMetadata]] {
    override def result = _tmetas
    override protected def generate (elem :TypeElement, metas :Seq[ClassMetadata]) {
      _tmetas ++= metas
    }
    protected var _tmetas = MSeq[ClassMetadata]()
  }

  protected abstract class TestProcessor[R] extends Processor {
    def result :R
  }

  private def process[R] (filename :String, content :String, proc :TestProcessor[R]) :R = {
    val files = List(streamObj, mkTestObject(filename, content))
    val options = List("-processor", "com.threerings.nexus.streamergen.Processor", "-proc:only")
    val task = _compiler.getTask(null, null, null, options, null, files)
    task.setProcessors(List(proc))
    task.call
    proc.result
  }

  private def mkTestObject (file :String, content :String) =
    new SimpleJavaFileObject(URI.create("test:/" + file), JavaFileObject.Kind.SOURCE) {
      override def getCharContent (ignoreEncodingErrors :Boolean) :CharSequence = content
    }

  private val _compiler = ToolProvider.getSystemJavaCompiler
}
