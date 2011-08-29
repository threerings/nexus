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
        public final T value1;
        public final T _value2;
        public final T value3_;
        public final T value4;
        public final T value5;
        public Box (T value1, T value2, T value3, T _value4, T value5_) {
          this.value1 = value1;
          this._value2 = value2;
          this.value3_ = value3;
          this.value4 = _value4;
          this.value5 = value5_;
        }
      }
      """)
    // System.err.println(metas.head.argToField)
  }

  @Test def testNested {
    val source = TestCompiler.genSource("Container.java", """
      package foo.bar;
      public class Container {
        public class Inner1 implements com.threerings.nexus.io.Streamable {}
        public class Inner2 implements com.threerings.nexus.io.Streamable {}
      }
      """)
    // System.err.println(source)
  }

  @Test def testNestedInStreamable {
    val source = TestCompiler.genSource("Outer.java", """
      package foo.bar;
      import com.threerings.nexus.io.Streamable;
      public class Outer implements Streamable {
        public class Inner1 implements Streamable {}
        public class Inner2 implements Streamable {}
      }
      """)
    // System.err.println(source)
  }

  @Test def testInherit {
    val source = TestCompiler.genSource("Container.java", """
      package foo.bar;
      public class Container {
        public class Parent implements com.threerings.nexus.io.Streamable {
          public final String first, last;
          public Parent (String first, String last) {
            this.first = first;
            this.last = last;
          }
        }
        public class Child extends Parent {
          public Child (String first, String last, int one, int two) {
            super(first, last);
            _one = one;
            _two = two;
          }
          // fields are written in ctor arg order, not field declaration order
          protected int _two;
          protected int _one;
        }
      }
      """)
    // System.err.println(source)
  }

  @Test def testParameterized {
    val source = TestCompiler.genSource("Box.java", """
      import java.util.Map;
      public class Box<A,B,K,V> implements com.threerings.nexus.io.Streamable {
        public final A valueA;
        public final Class<B> classB;
        public final Map<K,V> map;
        public Box (A valueA, Class<B> classB, Map<K,V> map) {
          this.valueA = valueA;
          this.classB = classB;
          this.map = map;
        }
      }
      """)
    // System.err.println(source)
  }

  @Test def testBounded {
    val source = TestCompiler.genSource("Box.java", """
      public class Box<T extends Comparable<T>> implements com.threerings.nexus.io.Streamable {
        public final T value;
        public Box (T value) {
          this.value = value;
        }
      }
      """)
    System.err.println(source)
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
