//
// Nexus Tools - code generators for Nexus framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.gencode

import javax.annotation.processing.SupportedAnnotationTypes
import javax.lang.model.element.TypeElement

import org.junit.Assert._
import org.junit.Test

/**
 * Tests the factory generator.
 */
class FactoryGenTest {
  @Test def testFactoryName {
    assertEquals("Factory_Foo", Generator.factoryName("Foo"))
    assertEquals("foo.bar.Factory_Foo", Generator.factoryName("foo.bar.Foo"))
  }

  @Test def testService {
    val source = FactoryTestCompiler.genSource("TestService.java", """
      package foo.bar;
      import com.threerings.nexus.util.Callback;
      public interface TestService extends com.threerings.nexus.distrib.NexusService {
        void addOne (int value, Callback<Integer> callback);
        void launchMissiles ();
      }
    """)
    // System.err.println(source)
  }

  @Test def testMultipleCallbacks {
    println("Expect error re: multiple callback args:")
    val source = FactoryTestCompiler.genSource("TestService.java", """
      package foo.bar;
      import com.threerings.nexus.util.Callback;
      public interface TestService extends com.threerings.nexus.distrib.NexusService {
        void bogus (int value, Callback<Integer> cb1, Callback<Integer> cb2);
      }
    """)
    // TODO: capture diagnostics during compile, ensure error is generated
    assertTrue(source == "")
  }

  @Test def testNonLastArgCallback {
    println("Expect error re: callback in non-final position:")
    val source = FactoryTestCompiler.genSource("TestService.java", """
      package foo.bar;
      import com.threerings.nexus.util.Callback;
      public interface TestService extends com.threerings.nexus.distrib.NexusService {
        void bogus (int value, Callback<Integer> cb1, int bob);
      }
    """)
    // TODO: capture diagnostics during compile, ensure error is generated
    assertTrue(source == "")
  }

  @Test def testMeta {
    val meta = FactoryTestCompiler.genMeta("TestService.java", """
      import com.threerings.nexus.util.Callback;
      public interface TestService extends com.threerings.nexus.distrib.NexusService {
        void addOne (int value, Callback<Integer> callback);
        void launchMissiles ();
      }
    """)
    assertEquals(meta.serviceName, "TestService")
    assertEquals(2, meta.methods.size)
    checkMethod(meta.methods.get(0), "addOne", 2)
    checkArg(meta.methods.get(0).args.get(0), "int", "Integer")
    checkArg(meta.methods.get(0).args.get(1), "Callback<Integer>", "Callback<Integer>")
    checkMethod(meta.methods.get(1), "launchMissiles", 0)
  }

  private def checkMethod (m :ServiceMetadata.Method, name :String, args :Int) {
    assertEquals(name, m.name)
    assertEquals(args, m.args.size)
  }

  private def checkArg (arg :ServiceMetadata.Arg, `type` :String, boxedType :String) {
    assertEquals(`type`, arg.`type`)
    assertEquals(boxedType, arg.boxedType)
  }
}

object FactoryTestCompiler extends TestCompiler {
  def genSource (filename :String, content :String) :String =
    process(new GenSourceProcessor, filename, content)

  @SupportedAnnotationTypes(Array("*"))
  class GenSourceProcessor extends TestProcessor[String] {
    override def result = _source
    override protected def generate (elem :TypeElement, metas :Seq[Metadata]) {
      val out = new java.io.StringWriter
      Generator.generateFactory(elem, metas.collect { case sm :ServiceMetadata => sm }, out)
      _source = out.toString
    }
    protected var _source = ""
  }

  def genMeta (filename :String, content :String) :ServiceMetadata =
    process(new GenMetaProcessor, filename, content)

  @SupportedAnnotationTypes(Array("*"))
  class GenMetaProcessor extends TestProcessor[ServiceMetadata] {
    override def result = _meta
    override protected def generate (elem :TypeElement, metas :Seq[Metadata]) {
      (metas.collect { case sm :ServiceMetadata => sm }).map { m => _meta = m }
    }
    protected var _meta :ServiceMetadata = _
  }

  override protected def stockObjects = List(nexusServiceObj, callbackObj)

  private def nexusServiceObj = mkTestObject("NexusService.java", """
    package com.threerings.nexus.distrib;
    public interface NexusService {}
  """)
  private def callbackObj = mkTestObject("Callback.java", """
    package com.threerings.nexus.util;
    public interface Callback<T> {}
  """)
}
