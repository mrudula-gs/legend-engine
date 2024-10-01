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

package org.finos.legend.engine.plan.execution.stores.deephaven;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;

import org.finos.legend.engine.language.deephaven.from.DeephavenGrammarParserExtension;

import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_ExecutionContext_Impl;
import org.finos.legend.pure.generated.core_external_execution_execution;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.junit.Test;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class DeephavenExecutionTest
{
    private final String testGrammarPath = ("/engineGrammar._pure_");
    //private final String testDataPath;

    @BeforeClass
    public static void beforeClass() throws Exception
    {
        // set up the deephaven instance with testdata

    }

    @AfterClass
    public static void afterClass() throws Exception
    {
        // clean up connections here
    }

    @Test
    public void deephavenSelectWhere()
    {
        String testGrammar;
        try
        {
            try (BufferedReader buffer = new BufferedReader(new InputStreamReader(DeephavenExecutionTest.class.getResourceAsStream(this.testGrammarPath))))
            {
                testGrammar = buffer.lines().collect(Collectors.joining("\n"));
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        // TODO: ask beyraf - why does it want to add to xt-grammar classpath? shouldn't it detect based on the dependencies in pom?
        MutableList<PureGrammarParserExtension> extensions = Lists.mutable.empty();
        extensions.add(new DeephavenGrammarParserExtension());
        PureModelContextData pmcd = PureGrammarParser.newInstance(PureGrammarParserExtensions.fromAvailableExtensions()).parseModel(testGrammar);
        Lambda lambda = PureGrammarParser.newInstance().parseLambda("|#>{test::DeephavenStore.nds_desktops}#->select()->from(test::DeephavenRuntime)");
        PureModel pureModel = Compiler.compile(pmcd, DeploymentMode.TEST, "user");
        LambdaFunction<?> lambdaFunction = HelperValueSpecificationBuilder.buildLambda(lambda, pureModel.getContext());
        RichIterable<? extends Root_meta_pure_extension_Extension> pureExtensions = PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport()));
        String result = core_external_execution_execution.Root_meta_legend_execute_FunctionDefinition_1__Pair_MANY__ExecutionContext_1__Extension_MANY__String_1_(
                lambdaFunction,
                Lists.mutable.empty(),
                new Root_meta_pure_runtime_ExecutionContext_Impl(""),
                // TODO: ask beyraf - does this imply we need a dependency on alloy project? seems wrong
                // import com.gs.alloy.execution.configuration.GsPureExtension;
                pureExtensions,
                pureModel.getExecutionSupport()
        );

        // TODO - add the expected data here to assert JSON equals for the select
        // TODO - add a test to filter by column
        System.out.println("TODO DELETE ME");


        // TODO - check with beyraf - his refactored doesn't work - not sure why it doesn't find symbol
//        PureModelContextData pmcd = PureGrammarParser.newInstance(PureGrammarParserExtensions.fromAvailableExtensions()).parseModel(testGrammar);
//        Lambda lambda = PureGrammarParser.newInstance().parseLambda("|#>{test::DeephavenStore.nds_desktops}#->select()->from(test::DeephavenRuntime)");
//        PureModel pureModel = Compiler.compile(pmcd, DeploymentMode.TEST, "user");
//        LambdaFunction<?> lambdaFunction = HelperValueSpecificationBuilder.buildLambda(lambda, pureModel.getContext());
//        String result = core_external_execution_execution.Root_meta_legend_execute_FunctionDefinition_1__Pair_MANY__ExecutionContext_1__Extension_MANY__String_1_(
//                lambdaFunction,
//                Lists.mutable.empty(),
//                new Root_meta_pure_runtime_ExecutionContext_Impl(""),
//                //TODO: ask beyraf or check later - doesn't work as can't find symbol pureExtensions(CompiledExecutionSupport)
//                // PureCoreExtensionLoader.pureExtensions(pureModel.getExecutionSupport()),
//                pureModel.getExecutionSupport()
//        );
//
//        // TODO - add the expected data here to assert JSON equals for the select
//        // TODO - add a test to filter by column
    }
}
