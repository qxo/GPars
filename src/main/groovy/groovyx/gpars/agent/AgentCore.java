// GPars (formerly GParallelizer)
//
// Copyright © 2008-10  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.agent;

import groovyx.gpars.util.PoolUtils;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Vaclav Pech
 *         Date: 13.4.2010
 */
public abstract class AgentCore {

    /**
     * A thread pool shared by all agents
     */
    private static final ExecutorService pool = Executors.newFixedThreadPool(PoolUtils.retrieveDefaultPoolSize(), new AgentThreadFactory());

    /**
     * Incoming messages
     */
    private final Queue<Object> queue = new ConcurrentLinkedQueue<Object>();

    /**
     * Indicates, whether there's an active thread handling a message inside the agent's body
     */
    private final AtomicBoolean active = new AtomicBoolean(false);

    /**
     * Adds the message to the agent\s message queue
     *
     * @param message A value or a closure
     */
    public final void send(final Object message) {
        queue.add(message);
        schedule();
    }

    /**
     * Adds the message to the agent\s message queue
     *
     * @param message A value or a closure
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public final void leftShift(final Object message) {
        send(message);
    }

    /**
     * Handles a single message from the message queue
     */
    final void perform() {
        try {
            final Object message = queue.poll();
            if (message != null) this.handleMessage(message);
        } finally {
            active.set(false);
            schedule();
        }
    }

    /**
     * Dynamically dispatches the method call
     *
     * @param message A value or a closure
     */
    abstract void handleMessage(final Object message);

    /**
     * Schedules processing of a next message, if there are some and if there isn't an active thread handling a message at the moment
     */
    void schedule() {
        if (!queue.isEmpty() && active.compareAndSet(false, true)) {
            pool.submit(new Runnable() {
                @SuppressWarnings({"CatchGenericClass"})
                public void run() {
                    try {
                        AgentCore.this.perform();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}