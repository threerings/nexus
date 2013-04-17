//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.server

import java.util.Properties
import java.util.concurrent.{ExecutorService, TimeUnit}

import com.threerings.nexus.distrib.{DService, Factory_TestService, Keyed, Singleton, TestService}
import com.threerings.nexus.util.Callback

import org.junit.Assert

/**
 * Test-related utility methods.
 */
object TestUtil
{
  class TestSingleton extends Singleton {
    def increment (value :Int) = value+1
  }

  class ChildSingleton extends TestSingleton

  class TestKeyed (key :Int) extends Keyed {
    override def getKey = key
    def decrement (value :Int) = value-1
  }

  class ChildKeyed (key :Int) extends TestKeyed(key)

  def createTestConfig :NexusConfig = {
    val props = new Properties()
    props.setProperty("nexus.node", "test")
    props.setProperty("nexus.hostname", "localhost")
    props.setProperty("nexus.rpc_timeout", "1000")
    new NexusConfig(props)
  }

  def awaitTermination (exec :ExecutorService) {
    try {
      if (!exec.awaitTermination(2, TimeUnit.SECONDS)) { // TODO: change back to 10
        Assert.fail("Executor failed to terminate after 10 seconds.")
      }
    } catch {
      case ie :InterruptedException => Assert.fail("Executor interrupted?")
    }
  }

  def createTestServiceAttr :DService.Factory[TestService] = {
    Factory_TestService.createDispatcher(new TestService () {
      def addOne (value :Int, callback :Callback[java.lang.Integer]) {
        callback.onSuccess(value+1)
      }
      def launchMissiles () {
        println("Bang!")
      }
    })
  }
}
