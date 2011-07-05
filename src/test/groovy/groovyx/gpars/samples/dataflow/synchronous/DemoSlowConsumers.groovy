// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
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

package groovyx.gpars.samples.dataflow.synchronous

import groovyx.gpars.dataflow.SyncDataflowBroadcast
import groovyx.gpars.group.NonDaemonPGroup

/**
 * Shows how synchronous dataflow broadcasts can be used to throttle fast producer when serving data to slow consumers.
 * Unlike when using asynchronous channels, synchronous channels block both the writer and the readers until all parties are ready to exchange messages.
 */

def group = new NonDaemonPGroup()

final SyncDataflowBroadcast channel = new SyncDataflowBroadcast(2)  //We will synchronize on two readers

def producer = group.task {
    (1..30).each {
        channel << it
        println "Just sent $it"
    }
    channel << -1
}

def fastConsumer = group.task {
    def mySubscription = channel.createReadChannel()
    while (true) {
        sleep 10  //simulating a fast consumer
        final Object msg = mySubscription.val
        if (msg == -1) return
        println "Fast consumer received $msg"
    }
}

def slowConsumer = group.task {
    def mySubscription = channel.createReadChannel()
    while (true) {
        sleep 500  //simulating a slow consumer
        final Object msg = mySubscription.val
        if (msg == -1) return
        println "Slow consumer received $msg"
    }
}

[fastConsumer, slowConsumer]*.join()

group.shutdown()
