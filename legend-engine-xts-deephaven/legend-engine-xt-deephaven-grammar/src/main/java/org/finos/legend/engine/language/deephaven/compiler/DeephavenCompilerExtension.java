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

package org.finos.legend.engine.language.deephaven.compiler;

import java.util.Collections;
import java.util.List;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;

import org.finos.legend.engine.protocol.deephaven.metamodel.runtime.DeephavenConnection;
import org.finos.legend.engine.protocol.deephaven.metamodel.store.DeephavenStore;

import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.relation.RelationStoreAccessor;
import org.finos.legend.engine.shared.core.function.Function4;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Connection;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_ExecutionContext;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;

public class DeephavenCompilerExtension implements CompilerExtension
{

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Deephaven");
    }

    @Override
    public CompilerExtension build()
    {
        return this;
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.immutable.with(Processor.newProcessor(DeephavenStore.class, HelperDeephavenStoreBuilder::buildStoreFirstPass));
    }

    @Override
    public List<Function2<Connection, CompileContext, Root_meta_core_runtime_Connection>> getExtraConnectionValueProcessors()
    {
        return Lists.fixedSize.with(
                (connectionValue, context) ->
                {
                    if (connectionValue instanceof DeephavenConnection)
                    {
                        return HelperDeephavenStoreBuilder.buildConnection((DeephavenConnection) connectionValue, context);
                    }
                    return null;
                }
        );
    }

    public List<Function4<RelationStoreAccessor, Store, CompileContext, ProcessingContext, ValueSpecification>>  getExtraRelationStoreAccessorProcessors()
    {
        return Collections.singletonList((rsa, s, compileContext, processingContext) ->
        {
            // todo TAMIMI look at RelationalCompilerExtension
            return null;
        });
    }
}
