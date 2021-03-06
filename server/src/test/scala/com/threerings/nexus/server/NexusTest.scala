//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.server

import java.util.concurrent.{ExecutorService, Executors}
import scala.collection.mutable.ArrayBuffer

import com.threerings.nexus.distrib._
import react._

import org.junit._
import org.junit.Assert._

/**
 * Unit tests for various [[Nexus]] bits.
 */
class NexusTest {
  import com.threerings.nexus.server.TestUtil._

  @Before def createServer {
    System.setProperty("nexus.safety_checks", "true")
    _exec = Executors.newFixedThreadPool(3)
    _server = new NexusServer(createTestConfig, _exec)
  }

  @After def shutdownServer {
    _server.shutdown()
    _exec.shutdown()
    TestUtil.awaitTermination(_exec)
    System.setProperty("nexus.safety_checks", "")
  }

  @Test def testRuntimeChecksOnAction {
    var printed = false

    class EntityA extends Singleton {
      def incr (value :Int) = value + 1
    }

    class EntityB extends Singleton {
      def incrAndDoubleAndPrint (value :Int) = {
        _server.invoke(classOf[EntityA], new Action[EntityA] {
          def invoke (a :EntityA) {
            val ivalue = a.incr(value)
            try {
              // naughty naughty, we're using an outer-this pointer to jump contexts; this will
              // fail because our runtime safety checks null out our outer-this pointers
              print(ivalue*2)
            } catch {
              // this NPE is expected, but we can't set a flag to indicate that we got here,
              // because that would require an outer this pointer and those have been nulled out
              case e :NullPointerException =>
            }
          }
        })
      }

      def print (value :Int) = {
        printed = true
        println(value)
      }
    }

    _server.register(classOf[EntityA], new EntityA)
    _server.register(classOf[EntityB], new EntityB)
    _server.invoke(classOf[EntityB], new Action[EntityB] {
      def invoke (b :EntityB) {
        b.incrAndDoubleAndPrint(5)
      }
    })

    delay(100) // give things time to fail
    assertFalse(printed) // ensure that the print call did not execute
  }

  @Test def testRoutedWithRuntimeChecks {
    class EntityA (nexus :Nexus, sig :SignalView[String]) extends Singleton {
      val strs = ArrayBuffer[String]()
      sig.connect(Entities.routed(nexus, classOf[EntityA], new Slot[String] {
        def onEmit (str :String) = strs += str
      }))
    }

    val signal = Signal.create[String]()
    val ea = new EntityA(_server, signal)
    _server.register(classOf[EntityA], ea)

    // these should no longer NPE when outer this pointers are nulled out
    signal.emit("one")
    signal.emit("two")

    delay(100) // give things time to process
    assertEquals(Seq("one", "two"), ea.strs)
  }

  @Test def testIdAssignment {
    class KeyedEnt (id :Int) extends Keyed {
      def getKey = id
    }
    var seen1 = Set[Int]()
    val t1 = new Thread() {
      override def run {
        for (i <- 0 until 1000) seen1 += _server.nextId(classOf[KeyedEnt])
      }
    };
    var seen2 = Set[Int]()
    val t2 = new Thread() {
      override def run {
        for (i <- 0 until 1000) seen2 += _server.nextId(classOf[KeyedEnt])
      }
    };
    t1.start(); t2.start()
    t1.join() ; t2.join()

    // make sure each thread got keys unique to itself
    assertEquals(1000, seen1.size)
    assertEquals(1000, seen2.size)
    // and make sure the returned keys do not overlap
    assertEquals(2000, (seen1 ++ seen2).size)
  }

  @Test def testGather {
    import scala.collection.JavaConversions._
    class Player (id :Int, val name :String) extends Keyed {
      def getKey = id
    }

    _server.registerKeyed(classOf[Player], new Player(1, "Bob"))
    _server.registerKeyed(classOf[Player], new Player(2, "Jim"))
    _server.registerKeyed(classOf[Player], new Player(4, "Jerry"))
    _server.registerKeyed(classOf[Player], new Player(9, "Hank"))

    val keys = Set[Comparable[_]](1, 2, 3, 4, 5, 6, 7, 8, 9)
    val map = _server.gather(classOf[Player], keys, new Request[Player,String] {
      def invoke (p :Player) = p.name
    })
    assertEquals(4, map.size)
    assertEquals("Bob", map.get(1))
    assertEquals("Jim", map.get(2))
    assertEquals("Jerry", map.get(4))
    assertEquals("Hank", map.get(9))

    delay(100) // give things time to process
  }

  @Test def testExceptionUnwrap {
    class Failer extends Singleton {
      def fail () :String = throw new NexusException("Fail!")
    }

    _server.register(classOf[Failer], new Failer)
    try {
      _server.request(classOf[Failer], new Request[Failer,String] {
        def invoke (f :Failer) = f.fail()
      })
    } catch {
      case ne :NexusException => assertEquals("Fail!", ne.getMessage)
    }
  }

  protected def delay (millis :Long) = Thread.sleep(millis)

  protected var _exec :ExecutorService = _
  protected var _server :NexusServer = _
}
