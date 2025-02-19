// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.deephaven.compiler;

import io.deephaven.client.impl.BarrageSession;
import io.deephaven.client.impl.TableHandle;
import io.deephaven.qst.table.TableSpec;
import io.deephaven.qst.table.TicketTable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.ExecutionPlanJavaCompilerExtension;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.stores.deephaven.specifics.IDeephavenExecutionNodeSpecifics;
import org.finos.legend.engine.shared.javaCompiler.ClassPathFilter;
import org.finos.legend.engine.shared.javaCompiler.ClassPathFilters;

import java.util.LinkedHashMap;
import java.util.Map;

public class DeephavenJavaCompilerExtension implements ExecutionPlanJavaCompilerExtension
{
    static final Map<String, Class<?>> DEPENDENCIES = new LinkedHashMap<>();
    private static final String PURE_PACKAGE = "meta::external::store::deephaven::executionPlan::platformBinding::legendJava::";


    static
    {
        DEPENDENCIES.put("io.deephaven.client.impl.BarrageSession", BarrageSession.class);
        DEPENDENCIES.put("io.deephaven.client.impl.TableHandle", TableHandle.class);
        DEPENDENCIES.put("io.deephaven.qst.table.TableSpec", TableSpec.class);
        DEPENDENCIES.put("io.deephaven.qst.table.TicketTable", TicketTable.class);
//        DEPENDENCIES.put("org.finos.legend.engine.plan.execution.result.Result", Result.class);
        DEPENDENCIES.put(PURE_PACKAGE + "IDeephavenExecutionNodeSpecifics", IDeephavenExecutionNodeSpecifics.class);
    }

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Deephaven");
    }

    @Override
    public ClassPathFilter getExtraClassPathFilter()
    {
        return ClassPathFilters.fromClasses(DEPENDENCIES.values());
    }
}
