//
// $Id$

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
      import com.threerings.nexus.util.Callback;
      public interface TestService extends com.threerings.nexus.distrib.NexusService {
        void addOne (int value, Callback<Integer> callback);
        void launchMissiles ();
      }
    """)
    // System.err.println(source)
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
    checkMethod(meta.methods.get(1), "launchMissiles", 0)
  }

  private def checkMethod (m :ServiceMetadata.Method, name :String, args :Int) {
    assertEquals(name, m.name)
    assertEquals(args, m.args.size)
  }
}

object FactoryTestCompiler extends TestCompiler {
  def genSource (filename :String, content :String) :String =
    process(filename, content, new GenSourceProcessor)

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
    process(filename, content, new GenMetaProcessor)

  @SupportedAnnotationTypes(Array("*"))
  class GenMetaProcessor extends TestProcessor[ServiceMetadata] {
    override def result = _meta
    override protected def generate (elem :TypeElement, metas :Seq[Metadata]) {
      (metas.collect { case sm :ServiceMetadata => sm }).filterNot(
        _.serviceName == "NexusService").map { m => _meta = m }
    }
    protected var _meta :ServiceMetadata = _
  }

  override protected def stockObjects = List(nexusServiceObj, callbackObj)

  private val nexusServiceObj = mkTestObject("NexusService.java", """
    package com.threerings.nexus.distrib;
    public interface NexusService {}
  """)
  private val callbackObj = mkTestObject("Callback.java", """
    package com.threerings.nexus.util;
    public interface Callback<T> {}
  """)
}
