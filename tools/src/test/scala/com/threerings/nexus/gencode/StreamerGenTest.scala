//
// $Id$

package com.threerings.nexus.gencode

import javax.annotation.processing.SupportedAnnotationTypes
import javax.lang.model.element.TypeElement

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
    val metas = StreamerTestCompiler.genMetas("Box.java", """
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
    // make sure all the ctor arg and field name variants are matched
    assertEquals(Map("value1" -> "value1", "value2" -> "_value2", "value3" -> "value3_",
                     "_value4" -> "value4", "value5_" -> "value5"), metas.head.argToField)
  }

  @Test def testNested {
    val metas = StreamerTestCompiler.genMetas("Container.java", """
      package foo.bar;
      public class Container {
        public class Inner1 implements com.threerings.nexus.io.Streamable {}
        public class Inner2 implements com.threerings.nexus.io.Streamable {}
      }
      """)
    assertEquals(2, metas.size)
  }

  @Test def testNestedInStreamable {
    val metas = StreamerTestCompiler.genMetas("Outer.java", """
      package foo.bar;
      import com.threerings.nexus.io.Streamable;
      public class Outer implements Streamable {
        public class Inner1 implements Streamable {}
        public class Inner2 implements Streamable {}
      }
      """)
    assertEquals(3, metas.size)
  }

  @Test def testNestedInStreamableIface {
    val metas = StreamerTestCompiler.genMetas("OuterIface.java", """
      package foo.bar;
      import com.threerings.nexus.io.Streamable;
      public interface OuterIface extends Streamable {
        public class Inner1 implements Streamable {}
        public class Inner2<T> implements Streamable {}
      }
      """)
    // make sure we correctly generate the type name of a type enclosed in an interface
    assertEquals("OuterIface.Inner1", metas.find(_.name == "Inner1").get.typeUse)
    assertEquals("OuterIface.Inner2<T>", metas.find(_.name == "Inner2").get.typeUse)
  }

  @Test def testInherit {
    val source = StreamerTestCompiler.genSource("Container.java", """
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

  @Test def testInheritFromInterface {
    val metas = StreamerTestCompiler.genMetas("OuterClass.java", """
      package foo.bar;
      interface OuterIface {
        class Parent implements com.threerings.nexus.io.Streamable {
          public final String first, last;
          public Parent (String first, String last) {
            this.first = first;
            this.last = last;
          }
        }
      }
      public class OuterClass {
        public class Child extends OuterIface.Parent {
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
    // make sure we correctly generate the enclosed name of a type that's nested in an iface
    assertEquals("OuterIface.Parent", metas.find(_.name == "Child").get.parentEnclosedName)
  }

  @Test def testParameterized {
    val metas = StreamerTestCompiler.genMetas("Box.java", """
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
    assertEquals("<A,B,K,V>", metas.head.typeBounds)
  }

  @Test def testBounded {
    val metas = StreamerTestCompiler.genMetas("Box.java", """
      public class Box<T extends Comparable<T>> implements com.threerings.nexus.io.Streamable {
        public final T value;
        public Box (T value) {
          this.value = value;
        }
      }
      """)
    assertEquals("<T extends Comparable<T>>", metas.head.typeBounds)
  }

  @Test def testUnionBounded {
    val metas = StreamerTestCompiler.genMetas("Box.java", """
      public class Box<T extends Comparable<T> & Iterable<?>>
        implements com.threerings.nexus.io.Streamable {
        public final T value;
        public Box (T value) {
          this.value = value;
        }
      }
      """)
    // TODO: omit Object when to-stringing union bounds with all interfaces
    assertEquals("<T extends Object & Comparable<T> & Iterable<?>>", metas.head.typeBounds)
  }

  @Test def testImplementsStreamaerSubIface {
    val metas = StreamerTestCompiler.genMetas("Container.java", """
      package foo.bar;
      public class Container {
        interface Foozle extends com.threerings.nexus.io.Streamable {
        }
        public class Name implements Foozle {
          public final String first, last;
          public Name (String first, String last) {
            this.first = first;
            this.last = last;
          }
        }
      }
      """)
    // ensure that Name is identified as a Streamable, even though it implements an interface that
    // extends Streamable rather than implementing Streamable itself
    assertEquals(1, metas.size)
  }

  @Test def testImports {
    val metas = StreamerTestCompiler.genMetas("Box.java", """
      import java.util.Map;
      public class Box<A,B extends Comparable<B>,K,V>
        implements com.threerings.nexus.io.Streamable {
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
    assertEquals(Set("Box", "java.util.Map"), metas.head.imports)
  }

  @Test def testSourceHeader {
    val header = "// foo bar baz!\n\n"
    Generator.setSourceHeader(header)
    try {
      val source = StreamerTestCompiler.genSource("Box.java", """
        package foo.bar;
        public class Box<T> implements com.threerings.nexus.io.Streamable {
          public final T value;
          public Box (T value) {
            this.value = value;
          }
        }
        """)
      assertTrue(source.startsWith(header))
    } finally {
      Generator.setSourceHeader("")
    }
  }

  @Test def testFieldOrdering {
    val metas = StreamerTestCompiler.genMetas("Upstream.java", """
      package foo.bar;
      import java.util.List;
      import com.threerings.nexus.io.Streamable;
      public interface Upstream extends Streamable {
        public static class ServiceCall implements Upstream {
          public final int callId;
          public final int objectId;
          public final short attrIndex;
          public final short methodId;
          public final List<Object> args;
          public ServiceCall (int callId, int objectId, short attrIndex,
                              short methodId, List<Object> args) {
              this.callId = callId;
              this.objectId = objectId;
              this.attrIndex = attrIndex;
              this.methodId = methodId;
              this.args = args;
          }
        }
      }
      """)
    // make sure our ctorArgs are not reordered due to LocalHashMap.keys funny biz
    assertEquals(metas.head.ctorArgs.keys.toSeq, metas.head.localCtorArgs)
    assertEquals(metas.head.ctorArgs.keys.toSeq, metas.head.orderedFieldNames)
  }

  @Test def testObject {
    val source = StreamerTestCompiler.genSource("FooObject.java", """
      public class FooObject extends com.threerings.nexus.distrib.NexusObject {
        public final String name;
        public FooObject (String name) {
          this.name = name;
        }
      }
      """)
    assertTrue(source.contains("readContents"))
    assertTrue(source.contains("writeContents"))
    // System.err.println(source)
  }

  @Test def testArrays {
    val metas = StreamerTestCompiler.genMetas("Box.java", """
      public class Box<T> implements com.threerings.nexus.io.Streamable {
        public final boolean[] booleans;
        public final byte[] bytes;
        public final char[] chars;
        public final short[] shorts;
        public final int[] ints;
        public final long[] longs;
        public final float[] floats;
        public final double[] doubles;
        public final String[] strings;
        public Box (boolean[] booleans, byte[] bytes, char[] chars, short[] shorts, int[] ints,
                    long[] longs, float[] floats, double[] doubles, String[] strings) {
            this.booleans = booleans;
            this.bytes = bytes;
            this.chars = chars;
            this.shorts = shorts;
            this.ints = ints;
            this.longs = longs;
            this.floats = floats;
            this.doubles = doubles;
            this.strings = strings;
        }
      }
      """)
    assertEquals(List("Booleans", "Bytes", "Chars", "Shorts", "Ints", "Longs", "Floats",
                      "Doubles", "Strings"), metas.head.ctorArgs.values.map(Utils.fieldKind))
  }
}

object StreamerTestCompiler extends TestCompiler {
  def genSource (filename :String, content :String) :String =
    process(filename, content, new GenSourceProcessor)

  @SupportedAnnotationTypes(Array("*"))
  class GenSourceProcessor extends TestProcessor[String] {
    override def result = _source
    override protected def generate (elem :TypeElement, metas :Seq[Metadata]) {
      val out = new java.io.StringWriter
      Generator.generateStreamer(elem, metas.map(_.asInstanceOf[StreamableMetadata]), out)
      _source = out.toString
    }
    protected var _source = ""
  }

  def genMetas (filename :String, content :String) :Seq[StreamableMetadata] =
    process(filename, content, new GenMetasProcessor)

  @SupportedAnnotationTypes(Array("*"))
  class GenMetasProcessor extends TestProcessor[Seq[StreamableMetadata]] {
    override def result = _tmetas
    override protected def generate (elem :TypeElement, metas :Seq[Metadata]) {
      _tmetas ++= metas.map(_.asInstanceOf[StreamableMetadata])
    }
    protected var _tmetas = Seq[StreamableMetadata]()
  }

  override protected def stockObjects = List(streamObj, nexobjObj)

  private val streamObj = mkTestObject("Streamable.java", """
    package com.threerings.nexus.io;
    public interface Streamable {
      public interface Input {}
      public interface Output {}
    }
  """)
  private val nexobjObj = mkTestObject("NexusObject.java", """
    package com.threerings.nexus.distrib;
    import com.threerings.nexus.io.Streamable;
    public abstract class NexusObject implements Streamable {
      public void readContents (Streamable.Input in) {}
      public void writeContents (Streamable.Output out) {}
    }
  """)
}
