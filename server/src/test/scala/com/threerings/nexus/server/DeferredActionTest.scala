//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.server

import java.util.concurrent.{ExecutorService, Executors}

import org.junit._
import org.junit.Assert._

import com.threerings.nexus.distrib.Action
import com.threerings.nexus.distrib.Nexus
import com.threerings.nexus.distrib.Request

/**
* Tests the deferred action services of the Nexus server.
*/
class DeferredActionTest
{
  import com.threerings.nexus.server.TestUtil._

  @Before def createServer {
    _exec = Executors.newFixedThreadPool(3)
    _server = new NexusServer(createTestConfig, _exec)
    _server.registerSingleton(new TestSingleton)
  }

  @After def shutdownServer {
    _server.shutdown()
    _exec.shutdown()
    TestUtil.awaitTermination(_exec)
  }

  @Test def testNonDeferredAction {
    // ensure that non-deferred actions are dispatched "quickly"
    val start = System.currentTimeMillis
    var invokedAt = 0L
    _server.invoke(classOf[TestSingleton], new Action[TestSingleton] {
      def invoke (obj :TestSingleton) {
        invokedAt = System.currentTimeMillis
      }
    })

    _exec.execute(new Runnable {
      def run {
        val elapsed = invokedAt - start
        assertTrue(elapsed < 20)
      }
    })
  }

  @Test def testDeferredAction {
    val start = System.currentTimeMillis
    var invokedAt = 0L
    _server.invokeAfter(classOf[TestSingleton], 20, new Action[TestSingleton] {
      def invoke (obj :TestSingleton) {
        invokedAt = System.currentTimeMillis
      }
    })

    delay(30)
    _exec.execute(new Runnable {
      def run {
        val elapsed = invokedAt - start
        assertFalse(elapsed < 20)
      }
    })
  }

  @Test def testCanceledAction {
    var invokedAt = 0L
    val defer = _server.invokeAfter(classOf[TestSingleton], 20, new Action[TestSingleton] {
      def invoke (obj :TestSingleton) {
        invokedAt = System.currentTimeMillis
      }
    })
    defer.cancel()

    delay(30)
    _exec.execute(new Runnable {
      def run = assertEquals(0L, invokedAt)
    })
  }

  @Test def testRepeatedAction {
    var invoked = 0
    val defer = _server.invokeAfter(classOf[TestSingleton], 20, new Action[TestSingleton] {
      def invoke (obj :TestSingleton) {
        invoked += 1
      }
    })
    defer.repeatEvery(20)

    delay(90)
    defer.cancel()

    delay(30)
    _exec.execute(new Runnable {
      def run = assertEquals(4, invoked)
    })
  }

  protected def delay (millis :Long) = Thread.sleep(millis)

  protected var _exec :ExecutorService = _
  protected var _server :NexusServer = _
}
