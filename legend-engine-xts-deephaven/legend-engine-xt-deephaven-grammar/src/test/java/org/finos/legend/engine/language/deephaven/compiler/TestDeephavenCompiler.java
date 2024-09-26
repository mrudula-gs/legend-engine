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

import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.junit.Test;

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
}
