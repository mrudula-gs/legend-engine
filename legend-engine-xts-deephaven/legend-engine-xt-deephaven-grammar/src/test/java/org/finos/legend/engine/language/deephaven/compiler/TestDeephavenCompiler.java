// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.language.deephaven.compiler;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_store_Column;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_store_DeephavenStore;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_store_Table;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_BooleanType;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_IntType;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_StringType;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_TimeType;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestDeephavenCompiler extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{

    @Override
    protected String getDuplicatedElementTestCode()
    {
        return "###Deephaven\n" +
                "import abc::abc::*;\n" +
                "Deephaven test::Store::foo\n" +
                "{\n" +
                "    Table xyz\n" +
                "    {\n" +
                "        prop1: string,\n" +
                "        prop2: int\n" +
                "    }\n" +
                "}\n" +
                "Deephaven test::Store::foo\n" +
                "{\n" +
                "    Table ijk\n" +
                "    {\n" +
                "        prop2: int,\n" +
                "        prop3: boolean\n" +
                "    }\n" +
                "}";
    }

    @Override
    protected String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [11:1-18:1]: Duplicated element 'test::Store::foo'";
    }

    @Test
    public void testCompileStore() throws Exception
    {
        Pair<PureModelContextData, PureModel> result = test("###Deephaven\n" +
                "Deephaven test::Store::foo\n" +
                "{\n" +
                "    Table xyz\n" +
                "    {\n" +
                "        prop1: string,\n" +
                "        prop2: int,\n" +
                "        prop3: boolean,\n" +
                "        prop4: datetime\n" +
                "    }\n" +
                "}\n"
        );

        Store store = result.getTwo().getStore("test::Store::foo");
        Assert.assertTrue(store instanceof Root_meta_external_store_deephaven_metamodel_store_DeephavenStore);
        RichIterable<? extends Root_meta_external_store_deephaven_metamodel_store_Table> tables = ((Root_meta_external_store_deephaven_metamodel_store_DeephavenStore) store)._tables();
        Assert.assertEquals(tables.size(), 1);
        RichIterable<? extends Root_meta_external_store_deephaven_metamodel_store_Column> columns = tables.getAny()._columns();
        Assert.assertEquals(columns.size(), 4);

        Map<String, Class<? extends Root_meta_external_store_deephaven_metamodel_type_Type>> actualCols = new HashMap<String, Class<? extends Root_meta_external_store_deephaven_metamodel_type_Type>>();
        columns.forEach(x -> actualCols.put(x._name(), x._type().getClass()));

        Assert.assertTrue(Root_meta_external_store_deephaven_metamodel_type_StringType.class.isAssignableFrom(actualCols.get("prop1")));
        Assert.assertTrue(Root_meta_external_store_deephaven_metamodel_type_IntType.class.isAssignableFrom(actualCols.get("prop2")));
        Assert.assertTrue(Root_meta_external_store_deephaven_metamodel_type_BooleanType.class.isAssignableFrom(actualCols.get("prop3")));
        Assert.assertTrue(Root_meta_external_store_deephaven_metamodel_type_TimeType.class.isAssignableFrom(actualCols.get("prop4")));
    }
}
