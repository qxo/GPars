// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.dataflow.stream

import groovyx.gpars.dataflow.DataFlowQueue
import groovyx.gpars.dataflow.Select
import static groovyx.gpars.dataflow.DataFlow.operator
import static groovyx.gpars.dataflow.DataFlow.select
import static groovyx.gpars.dataflow.DataFlow.selector
import static groovyx.gpars.dataflow.DataFlow.task

class DataFlowStreamOperatorTest extends GroovyTestCase {
    public void testOperatorCommunication() {
        final DataFlowStream a = new DataFlowStream()
        final DataFlowStream b = new DataFlowStream()
        def aw = new DataFlowStreamWriteAdapter(a)
        def bw = new DataFlowStreamWriteAdapter(b)
        def ar = new DataFlowStreamReadAdapter(a)
        def br = new DataFlowStreamReadAdapter(b)

        def result = new DataFlowQueue()

        def op1 = operator(ar, bw) {
            bindOutput it
        }
        def op2 = selector([br], [result]) {
            result << it
        }

        aw << 1
        aw << 2
        aw << 3
        assert ([1, 2, 3] == [result.val, result.val, result.val])
        op1.stop()
        op2.stop()
        op1.join()
        op2.join()
    }

    public void testSelect() {
        final DataFlowStream a = new DataFlowStream()
        final DataFlowStream b = new DataFlowStream()
        def aw = new DataFlowStreamWriteAdapter(a)
        def bw = new DataFlowStreamWriteAdapter(b)
        def ar = new DataFlowStreamReadAdapter(a)
        def br = new DataFlowStreamReadAdapter(b)

        final Select<?> select = select(ar, br)
        task {
            aw << 1
            aw << 2
            aw << 3
        }
        assert 1 == select().value
        assert 2 == select().value
        assert 3 == select().value
        task {
            bw << 4
            aw << 5
            bw << 6
        }
        def result = (1..3).collect {select()}.sort {it.value}
        assert result*.value == [4, 5, 6]
        assert result*.index == [1, 0, 1]
    }
}
