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
import org.finos.legend.engine.protocol.deephaven.metamodel.type.BooleanType;
import org.finos.legend.engine.protocol.deephaven.metamodel.type.ByteType;
import org.finos.legend.engine.protocol.deephaven.metamodel.type.CharType;
import org.finos.legend.engine.protocol.deephaven.metamodel.type.CustomType;
import org.finos.legend.engine.protocol.deephaven.metamodel.type.DoubleType;
import org.finos.legend.engine.protocol.deephaven.metamodel.type.FloatType;
import org.finos.legend.engine.protocol.deephaven.metamodel.type.IntType;
import org.finos.legend.engine.protocol.deephaven.metamodel.type.LongType;
import org.finos.legend.engine.protocol.deephaven.metamodel.type.ShortType;
import org.finos.legend.engine.protocol.deephaven.metamodel.type.StringType;
import org.finos.legend.engine.protocol.deephaven.metamodel.type.TimeType;
import org.finos.legend.engine.protocol.deephaven.metamodel.type.TypeVisitor;
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
        // TODO - for things that aren't packageable elements we need to add sourceinfo back in future - sourceinfo is only avail for packageable element equivalents in pure
        return new Root_meta_external_store_deephaven_metamodel_store_Table_Impl(srcTable.name, sourceInformation, classifier)
                ._name(srcTable.name)
                ._columns(columns);
    }

    public static Root_meta_external_store_deephaven_metamodel_store_Column buildColumn(Column srcColumn, org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInformation, CompileContext context)
    {
        Class<?> colClassifier = context.pureModel.getClass("meta::external::store::deephaven::metamodel::store::Column");
        // TODO - for things that aren't packageable elements we need to add sourceinfo back in future - sourceinfo is only avail for packageable element equivalents in pure
        return new Root_meta_external_store_deephaven_metamodel_store_Column_Impl(srcColumn.name, sourceInformation, colClassifier)
                ._name(srcColumn.name)
                ._type(srcColumn.type.accept(new DeephavenTypeVisitor(context)));
    }

    private static class DeephavenTypeVisitor implements TypeVisitor<Root_meta_external_store_deephaven_metamodel_type_Type>
    {
        private final CompileContext context;

        private DeephavenTypeVisitor(CompileContext context)
        {
            this.context = context;
        }

        @Override
        public Root_meta_external_store_deephaven_metamodel_type_BooleanType visit(BooleanType val)
        {
            return new Root_meta_external_store_deephaven_metamodel_type_BooleanType_Impl(val.getClass().getName(), null, this.context.pureModel.getClass("meta::external::store::deephaven::metamodel::type::BooleanType"));
        }

        @Override
        public Root_meta_external_store_deephaven_metamodel_type_ByteType visit(ByteType val)
        {
            return new Root_meta_external_store_deephaven_metamodel_type_ByteType_Impl(val.getClass().getName(), null, this.context.pureModel.getClass("meta::external::store::deephaven::metamodel::type::ByteType"));
        }

        @Override
        public Root_meta_external_store_deephaven_metamodel_type_CharType visit(CharType val)
        {
            return new Root_meta_external_store_deephaven_metamodel_type_CharType_Impl(val.getClass().getName(), null, this.context.pureModel.getClass("meta::external::store::deephaven::metamodel::type::CharType"));
        }

        @Override
        public Root_meta_external_store_deephaven_metamodel_type_CustomType visit(CustomType val)
        {
            return new Root_meta_external_store_deephaven_metamodel_type_CustomType_Impl(val.getClass().getName(), null, this.context.pureModel.getClass("meta::external::store::deephaven::metamodel::type::CustomType"));
        }

        @Override
        public Root_meta_external_store_deephaven_metamodel_type_DoubleType visit(DoubleType val)
        {
            return new Root_meta_external_store_deephaven_metamodel_type_DoubleType_Impl(val.getClass().getName(), null, this.context.pureModel.getClass("meta::external::store::deephaven::metamodel::type::DoubleType"));
        }

        @Override
        public Root_meta_external_store_deephaven_metamodel_type_FloatType visit(FloatType val)
        {
            return new Root_meta_external_store_deephaven_metamodel_type_FloatType_Impl(val.getClass().getName(), null, this.context.pureModel.getClass("meta::external::store::deephaven::metamodel::type::FloatType"));
        }

        @Override
        public Root_meta_external_store_deephaven_metamodel_type_IntType visit(IntType val)
        {
            return new Root_meta_external_store_deephaven_metamodel_type_IntType_Impl(val.getClass().getName(), null, this.context.pureModel.getClass("meta::external::store::deephaven::metamodel::type::IntType"));
        }

        @Override
        public Root_meta_external_store_deephaven_metamodel_type_LongType visit(LongType val)
        {
            return new Root_meta_external_store_deephaven_metamodel_type_LongType_Impl(val.getClass().getName(), null, this.context.pureModel.getClass("meta::external::store::deephaven::metamodel::type::LongType"));
        }

        @Override
        public Root_meta_external_store_deephaven_metamodel_type_ShortType visit(ShortType val)
        {
            return new Root_meta_external_store_deephaven_metamodel_type_ShortType_Impl(val.getClass().getName(), null, this.context.pureModel.getClass("meta::external::store::deephaven::metamodel::type::ShortType"));
        }

        @Override
        public Root_meta_external_store_deephaven_metamodel_type_StringType visit(StringType val)
        {
            return new Root_meta_external_store_deephaven_metamodel_type_StringType_Impl(val.getClass().getName(), null, this.context.pureModel.getClass("meta::external::store::deephaven::metamodel::type::StringType"));
        }

        @Override
        public Root_meta_external_store_deephaven_metamodel_type_TimeType visit(TimeType val)
        {
            return new Root_meta_external_store_deephaven_metamodel_type_TimeType_Impl(val.getClass().getName(), null, this.context.pureModel.getClass("meta::external::store::deephaven::metamodel::type::TimeType"));
        }
    }
}
