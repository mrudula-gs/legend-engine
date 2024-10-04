// Copyright 2024 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.finos.legend.engine.plan.execution.stores.deephaven.plugin;

import org.finos.legend.engine.plan.execution.result.TDSResult;

import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNodeVisitor;

import org.finos.legend.engine.shared.core.identity.Identity;

public class DeephavenExecutionNodeExecutor implements ExecutionNodeVisitor<TDSResult>
{
    private final Identity identity;
    //TODO: tamimi - create these
    //private final ExecutionState executionState;
    //private final DeephavenStoreState state;

    // TODO: tamimi - check if can use TDS result in future
    public DeephavenExecutionNodeExecutor(Identity identity, ExecutionState executionState, DeephavenStoreState state)
    {
        this.identity = identity;
        this.executionState = executionState;
        this.state = state;
    }

}
