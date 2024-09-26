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

import java.util.stream.Collectors;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.SourceInformationHelper;

import org.finos.legend.engine.protocol.deephaven.metamodel.store.DeephavenStore;
import org.finos.legend.engine.protocol.deephaven.metamodel.store.Table;
import org.finos.legend.engine.protocol.deephaven.metamodel.store.Column;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;

public class HelperDeephavenStoreBuilder
{
    public static Root_meta_external_store_deephaven_metamodel_store_DeephavenStore buildStoreFirstPass(DeephavenStore srcStore, CompileContext context)
    {
        org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInformation = SourceInformationHelper.toM3SourceInformation(srcStore.sourceInformation);
        MutableList<Root_meta_external_store_deephaven_metamodel_store_Table> tables = srcStore.tables.stream().map(x -> buildTable(x, sourceInformation, context)).collect(Collectors.toCollection(Lists.mutable::empty));
        Class<?> classifier = context.pureModel.getClass("meta::external::store::deephaven::metamodel::store::DeephavenStore");
        Root_meta_external_store_deephaven_metamodel_store_DeephavenStore store = new Root_meta_external_store_deephaven_metamodel_store_DeephavenStore_Impl(srcStore.name, sourceInformation, classifier)
                ._classifierGenericType(context.pureModel.getGenericType(classifier))
                ._tables(tables);
        return store._validate(true, sourceInformation, context.getExecutionSupport());
    }

    public static Root_meta_external_store_deephaven_metamodel_store_Table buildTable(Table srcTable, org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInformation, CompileContext context)
    {
        MutableList<Root_meta_external_store_deephaven_metamodel_store_Column> columns = srcTable.columns.stream().map(x -> buildColumn(x, sourceInformation, context)).collect(Collectors.toCollection(Lists.mutable::empty));
        Class<?> classifier = context.pureModel.getClass("meta::external::store::deephaven::metamodel::store::Table");
        // TODO - ask beyraf - does the pure graph actually need to know about sourceinfo ? probably not... ?
        return new Root_meta_external_store_deephaven_metamodel_store_Table_Impl(srcTable.name, sourceInformation, classifier)
                ._name(srcTable.name)
                ._columns(columns);
    }

    public static Root_meta_external_store_deephaven_metamodel_store_Column buildColumn(Column srcColumn, org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInformation, CompileContext context)
    {
        Class<?> colClassifier = context.pureModel.getClass("meta::external::store::deephaven::metamodel::store::Column");
        Class<?> typeClassifier = context.pureModel.getClass("meta::external::store::deephaven::metamodel::store::Type");
        // TODO - ask beyraf - does the pure graph actually need to know about sourceinfo ? probably not... ?
        return new Root_meta_external_store_deephaven_metamodel_store_Column_Impl(srcColumn.name, sourceInformation, colClassifier)
                ._name(srcColumn.name)
                ._type(new Root_meta_external_store_deephaven_metamodel_type_Type_Impl(srcColumn.type.getClass().getName(), sourceInformation, typeClassifier));
    }
}
