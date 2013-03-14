//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.server

import scala.collection.mutable.ListBuffer
import java.util.concurrent.Executor

import com.threerings.nexus.distrib.{Action, DValue, Nexus, NexusException, Request, TestObject}

import org.junit._
import org.junit.Assert._

import react.UnitSlot

import com.threerings.nexus.server.TestUtil._

/**
 * Unit tests for the {@link ObjectManager}.
 */
class ObjectManagerTest
{
  @Test def testRegister {
    val omgr = createObjectManager
    val test = new TestObject(createTestServiceAttr)
    omgr.register(test)

    // ensure that we've been assigned an id
    assertTrue(test.getId > 0)

    // ensure that event dispatch is wired up
    val ovalue = test.value.get
    val nvalue = "newValue"
    var triggered = false
    test.value.connect(new react.ValueView.Listener[String] {
      override def onChange (value :String, oldValue :String) {
        assertEquals(ovalue, oldValue)
        assertEquals(nvalue, value)
        triggered = true
      }
    })

    // update the value and make sure the listener was triggered (tests use the direct executor, so
    // it will process events directly inline with the update)
    test.value.update(nvalue)
    assertTrue(triggered)

    // ensure that after clear, we no longer have an id
    omgr.clear(test)
    assertFalse(test.getId > 0)
  }

  @Test def testSingleton {
    val omgr = createObjectManager
    val test = new TestSingleton
    omgr.registerSingleton(test)

    // ensure that actions are dispatched on our registered entity
    var invoked = false
    omgr.invoke(classOf[TestSingleton], new Action[TestSingleton] {
      def invoke (obj :TestSingleton) {
        invoked = true
      }
    })
    assertTrue(invoked)

    // ensure that requests are dispatched on our registered entity
    val result = omgr.invoke(classOf[TestSingleton], new Request[TestSingleton,Int] {
      def invoke (obj :TestSingleton) = obj.increment(0)
    })
    assertEquals(1, result)

    // ensure that actions are not dispatched once the entity is cleared
    omgr.clearSingleton(test)
    try {
      omgr.invoke(classOf[TestSingleton], FAIL_SINGLE)
      fail()
    } catch {
      case ne :NexusException => // expected
    }
  }

  @Test def testMissingSingletonInvoke {
    val omgr = createObjectManager
    try {
      omgr.invoke(classOf[TestSingleton], FAIL_SINGLE)
      fail()
    } catch {
      case ne :NexusException => // expected
    }
  }

  @Test def testRegisterAndClearKeyed {
    val omgr = createObjectManager
    val test = new TestKeyed(5)
    omgr.registerKeyed(test)

    // ensure that we report that we currently host this entity
    assertTrue(omgr.hostsKeyed(classOf[TestKeyed], test.getKey))

    // ensure that actions are dispatched on our registered entity
    var invoked = false
    omgr.invoke(classOf[TestKeyed], test.getKey, new Action[TestKeyed] {
      def invoke (obj :TestKeyed) {
        invoked = true
      }
    })
    assertTrue(invoked)

    // ensure that requests are dispatched on our registered entity
    val result = omgr.invoke(classOf[TestKeyed], test.getKey, new Request[TestKeyed,Int] {
      def invoke (obj :TestKeyed) = obj.decrement(0)
    })
    assertEquals(-1, result)

    // ensure that actions are not dispatched once the entity is cleared
    omgr.clearKeyed(test)
    omgr.invoke(classOf[TestKeyed], test.getKey, MISSING_KEYED)

    // ensure that we no longer report that we currently host this entity
    assertFalse(omgr.hostsKeyed(classOf[TestKeyed], test.getKey))
  }

  @Test def testMissingKeyedInvoke {
    val omgr = createObjectManager

    // test when there are no registrations at all
    omgr.invoke(classOf[TestKeyed], 3, MISSING_KEYED)

    // now test just a key mismatch
    omgr.registerKeyed(new TestKeyed(1))
    omgr.invoke(classOf[TestKeyed], 3, MISSING_KEYED)
  }

  @Test def testKeyedSubclass {
    val omgr = createObjectManager
    omgr.registerKeyed(new ChildKeyed(1))
    var invoked = false
    omgr.invoke(classOf[TestKeyed], 1, new Action[TestKeyed] {
      def invoke (obj :TestKeyed) {
        assertTrue(obj.isInstanceOf[ChildKeyed])
        invoked = true
      }
    })
    assertTrue(invoked)
  }

  @Test def testSingletonSubclass {
    val omgr = createObjectManager
    omgr.registerSingleton(new ChildSingleton)
      var invoked = false
    omgr.invoke(classOf[TestSingleton], new Action[TestSingleton] {
      def invoke (obj :TestSingleton) {
        assertTrue(obj.isInstanceOf[ChildSingleton])
        invoked = true
      }
    })
    assertTrue(invoked)
  }

  @Test def testGlobalMap {
    val toExec = ListBuffer[Runnable]()
    val omgr = new ObjectManager(createTestConfig, null, new Executor {
      override def execute (op :Runnable) = toExec += op
    })
    val map = omgr.registerMap[String,String]("test")
    var (oneOps, twoOps) = (0, 0)
    map.getView("one").connect(new UnitSlot {
      def onEmit = oneOps += 1
    })
    map.getView("two").connect(new UnitSlot {
      def onEmit = twoOps += 1
    })
    map.put("one", "one")
    map.put("one", "two")
    map.put("two", "two")
    map.put("three", "two") // will trigger no listener
    map.remove("one")
    map.remove("one") // will trigger no listener
    // no notifications should yet have been sent
    assertEquals(0, oneOps)
    assertEquals(0, twoOps)
    // now execute our pending ops which will dispatch notifications
    toExec foreach { _.run() }
    assertEquals(3, oneOps)
    assertEquals(1, twoOps)
  }

  @Test def testRequireContext {
    val omgr = createObjectManager
    omgr.registerSingleton(new TestSingleton)
    omgr.registerKeyed(new TestKeyed(5))
    var checks = 0

    omgr.invoke(classOf[TestSingleton], new Action[TestSingleton] {
      def invoke (obj :TestSingleton) {
        omgr.assertContext(classOf[TestSingleton])
        checks += 1
        try {
          omgr.assertContext(classOf[TestKeyed], 5)
        } catch {
          case ae :AssertionError => checks += 1
        }
      }
    })
    assertEquals(2, checks)

    omgr.invoke(classOf[TestKeyed], 5, new Action[TestKeyed] {
      def invoke (obj :TestKeyed) {
        omgr.assertContext(classOf[TestKeyed], 5)
        checks += 1
        try {
          omgr.assertContext(classOf[TestSingleton])
        } catch {
          case ae :AssertionError => checks += 1
        }
      }
    })
    assertEquals(4, checks)
  }

  protected def createObjectManager = new ObjectManager(createTestConfig, null, DIRECT_EXEC)

  protected val DIRECT_EXEC = new Executor {
    override def execute (op :Runnable) = op.run()
  }

  protected val FAIL_SINGLE = new Action[TestSingleton] {
    override def invoke (obj :TestSingleton) = fail()
  }

  protected val MISSING_KEYED = new Action[TestKeyed] {
    override def invoke (obj :TestKeyed) = fail()
    override def onDropped (nexus :Nexus, eclass :Class[_], key :Comparable[_]) {
      // expected
    }
  }
}
