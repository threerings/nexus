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
      public interface TestService extends com.threerings.nexus.distrib.NexusService {
        react.RFuture<Integer> addOne (int value);
        void launchMissiles ();
      }
    """)
    // System.err.println(source)
  }

  @Test def testMeta {
    val meta = FactoryTestCompiler.genMeta("TestService.java", """
      public interface TestService extends com.threerings.nexus.distrib.NexusService {
        react.RFuture<Integer> addOne (int value);
        void launchMissiles ();
      }
    """)
    assertEquals(meta.serviceName, "TestService")
    assertEquals(2, meta.methods.size)
    checkMethod(meta.methods.get(0), "addOne", 1)
    checkArg(meta.methods.get(0).args.get(0), "int", "Integer")
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

  override protected def stockObjects = List(nexusServiceObj, rFutureObj)

  private def nexusServiceObj = mkTestObject("NexusService.java", """
    package com.threerings.nexus.distrib;
    public interface NexusService {}
  """)
  private def rFutureObj = mkTestObject("RFuture.java", """
    package react;
    public interface RFuture<T> {}
  """)
}
