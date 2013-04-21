//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.server

import java.util.concurrent.{ExecutorService, Executors}

import com.threerings.nexus.distrib.{Action, Singleton}
import com.threerings.nexus.distrib.NexusException

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

    _server.registerSingleton(new EntityA)
    _server.registerSingleton(new EntityB)
    _server.invoke(classOf[EntityB], new Action[EntityB] {
      def invoke (b :EntityB) {
        b.incrAndDoubleAndPrint(5)
      }
    })

    delay(500) // give things time to fail
    assertFalse(printed) // ensure that the print call did not execute
  }

  protected def delay (millis :Long) = Thread.sleep(millis)

  protected var _exec :ExecutorService = _
  protected var _server :NexusServer = _
}
