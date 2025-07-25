// Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.AggColSpec;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.AggColSpecArray;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared.AggregationShared;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared.TDSCoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.SortDirection;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.SortInfo;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.TestTDS;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class GroupBy extends AggregationShared
{
    public GroupBy(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        super(functionExecution, repository);
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance returnGenericType = getReturnGenericType(resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionCallStack, processorSupport);

        TestTDS tds = getTDS(params, 0, processorSupport);

        RelationType<?> relationType = getRelationType(params, 0);
        boolean containsGroupByCols = params.size() == 3;
        int aggSpecParamIndex = containsGroupByCols ? 2 : 1;

        // Build TDS
        Pair<TestTDS, MutableList<Pair<Integer, Integer>>> orderedSource = getOrderedSource(params, processorSupport, tds, containsGroupByCols);
        TestTDS result = orderedSource.getOne()._distinct(orderedSource.getTwo());

        // Aggregations
        CoreInstance aggSpec = Instance.getValueForMetaPropertyToOneResolved(params.get(aggSpecParamIndex), M3Properties.values, processorSupport);
        if (aggSpec instanceof AggColSpec)
        {
            result.addColumn(processOneAggColSpec(orderedSource, Lists.fixedSize.empty(), null, (AggColSpec<?, ?, ?>) aggSpec, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionCallStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, true, false, null));
        }
        else if (aggSpec instanceof AggColSpecArray)
        {
            result = ((AggColSpecArray<?, ?, ?>) aggSpec)._aggSpecs().injectInto(result, (accResult, aggColSpec) -> accResult.addColumn(processOneAggColSpec(orderedSource, Lists.fixedSize.empty(), null, aggColSpec, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionCallStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, true, false, null)));
        }
        else
        {
            throw new RuntimeException("Not Possible");
        }

        return ValueSpecificationBootstrap.wrapValueSpecification(new TDSCoreInstance(result, returnGenericType, repository, processorSupport), false, processorSupport);
    }

    private Pair<TestTDS, MutableList<Pair<Integer, Integer>>> getOrderedSource(ListIterable<? extends CoreInstance> params, ProcessorSupport processorSupport, TestTDS tds, boolean containsGroupByCols)
    {
        if (containsGroupByCols)
        {
            // Col Ids
            Object cols = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport);
            ListIterable<String> ids = getColumnIds(cols);

            // Build TDS
            return tds.sort(ids.collect(c -> new SortInfo(c, SortDirection.ASC)));
        }
        return tds.wrapFullTDS();
    }

}
