//
// $Id$
//
// Nexus Server - server-side support for Nexus distributed application framework
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.server;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;

import static com.threerings.nexus.util.Log.log;

/**
 * Contains an execution queue for one or more Nexus entities. Distributed events, actions and
 * requests are all executed in order, one at a time, by no more than a single thread (for a given
 * context) for all entities registered with a particular context. In general, there is a one
 * entity to one context relationship, meaning that everything is run in parallel. However, if an
 * object is registered as a child of another entity, that object will share the entity's context.
 */
public class EntityContext
{
    /**
     * Creates an entity context that will use the supplied executor for executions.
     */
    public EntityContext (Executor exec) {
        _exec = exec;
    }

    /**
     * Queues the supplied operation for execution on this context. If the context is currently
     * being executed, the operation will simply be added to its queue. If it is not being
     * executed, it will additionally be queued up on the supplied executor, so that its pending
     * operations will be executed.
     */
    public synchronized void postOp (final Runnable op) {
        _ops.offer(new Runnable() {
            public void run () {
                try {
                    op.run();
                } catch (Throwable t) {
                    log.warning("Entity operation failed: " + op, t);
                } finally {
                    scheduleNext();
                }
            }
        });
        if (_active == null) {
            scheduleNext();
        }
    }

    protected synchronized void scheduleNext () {
        if ((_active = _ops.poll()) != null) {
            _exec.execute(_active);
        }
    }

    /** The executor to which we delegate our execution. */
    protected final Executor _exec;

    /** The currently active operation, or null. */
    protected Runnable _active;

    /** The queue of operations pending on this context. */
    protected final Queue<Runnable> _ops = new ArrayDeque<Runnable>();
}
