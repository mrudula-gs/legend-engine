//// Copyright 2025 Goldman Sachs
////
//// Licensed under the Apache License, Version 2.0 (the "License");
//// you may not use this file except in compliance with the License.
//// You may obtain a copy of the License at
////
////       http://www.apache.org/licenses/LICENSE-2.0
////
//// Unless required by applicable law or agreed to in writing, software
//// distributed under the License is distributed on an "AS IS" BASIS,
//// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//// See the License for the specific language governing permissions and
//// limitations under the License.
//
//package org.finos.legend.engine.plan.execution.stores.deephaven;
//
//import io.deephaven.client.impl.*;
//import io.deephaven.client.impl.script.Changes;
//import io.deephaven.qst.table.LabeledTable;
//import io.deephaven.qst.table.LabeledTables;
//import io.deephaven.qst.table.TableSpec;
//import io.deephaven.qst.table.TicketTable;
//import io.deephaven.uri.DeephavenTarget;
////import io.deephaven.engine.sql.*;
////import io.deephaven.engine.table.
//import org.apache.arrow.flight.FlightStream;
//import org.apache.arrow.memory.RootAllocator;
//import org.apache.arrow.vector.VectorSchemaRoot;
//import org.eclipse.collections.api.RichIterable;
//import org.eclipse.collections.api.factory.Lists;
//import org.finos.legend.engine.language.pure.compiler.Compiler;
//import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
//import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
//import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
//import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
//import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
//import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
//import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
//import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
//import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
//import org.finos.legend.pure.generated.Root_meta_pure_runtime_ExecutionContext_Impl;
////import org.finos.legend.pure.generated.core_deephaven_pure_contract_store_contract;
////import org.finos.legend.pure.generated.core_external_execution_execution;
//import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
//import org.junit.AfterClass;
//import org.junit.BeforeClass;
//import org.junit.Test;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.stream.Collectors;
//
//public class DeephavenExecutionTest
//{
//    private final String testGrammarPath = ("/engineGrammar_simple._pure_");
//    private final String authType = "io.deephaven.authentication.psk.PskAuthenticationHandler ";
//    private final String targetPrefix = "dh+plain://";
//    private ScheduledExecutorService _threadPool;
//    Session clientSession;
//    private BarrageSession _barrageSession;
//    //private final String testDataPath;
//
//    @BeforeClass
//    public static void beforeClass() throws Exception
//    {
//        // set up the deephaven instance with testdata
//
//    }
//
//    @AfterClass
//    public static void afterClass() throws Exception
//    {
//        // clean up connections here
//    }
//
//    @Test
//    public void deephavenSelectWhere()
//    {
//        String testGrammar;
//        try
//        {
//            try (BufferedReader buffer = new BufferedReader(new InputStreamReader(DeephavenExecutionTest.class.getResourceAsStream(this.testGrammarPath))))
//            {
//                testGrammar = buffer.lines().collect(Collectors.joining("\n"));
//            }
//        }
//        catch (IOException e)
//        {
//            throw new RuntimeException(e);
//        }
//
//        // TODO: ask beyraf - why does it want to add to xt-grammar classpath? shouldn't it detect based on the dependencies in pom?
////        MutableList<PureGrammarParserExtension> extensions = Lists.mutable.empty();
////        extensions.add(new DeephavenGrammarParserExtension());
//        PureModelContextData pmcd = PureGrammarParser.newInstance(PureGrammarParserExtensions.fromAvailableExtensions()).parseModel(testGrammar);
//        Lambda lambda = PureGrammarParser.newInstance().parseLambda("|#>{test::DeephavenStore.nds_desktops}#->select()->from(test::DeephavenRuntime)");
//        PureModel pureModel = Compiler.compile(pmcd, DeploymentMode.TEST, "user");
//        LambdaFunction<?> lambdaFunction = HelperValueSpecificationBuilder.buildLambda(lambda, pureModel.getContext());
//        RichIterable<? extends Root_meta_pure_extension_Extension> pureExtensionsOld = PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport()));
//        RichIterable<? extends Root_meta_pure_extension_Extension> pureExtensions = Lists.immutable.of(core_deephaven_pure_contract_store_contract.Root_meta_external_store_deephaven_extension_deephavenExtension__Extension_1_(pureModel.getExecutionSupport()));
////                PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport()))
////                core_deephaven_pure_contract_store_contract.Root_meta_external_store_deephaven_extension_deephavenExtension__Extension_1_
////                        (pureModel.getExecutionSupport());
////       .add(core_deephaven_pure_contract_store_contract.Root_meta_external_store_deephaven_extension_deephavenExtension__Extension_1_
////               (pureModel.getExecutionSupport()));
//
////        CompiledSupport.toPureCollection(_config._extension())
//        String result = core_external_execution_execution.Root_meta_legend_execute_FunctionDefinition_1__Pair_MANY__ExecutionContext_1__Extension_MANY__String_1_(
//                lambdaFunction,
//                Lists.mutable.empty(),
//                new Root_meta_pure_runtime_ExecutionContext_Impl(""),
//                pureExtensions,
//                pureModel.getExecutionSupport()
//        );
//
//        // TODO - add the expected data here to assert JSON equals for the select
//        // TODO - add a test to filter by column
//        System.out.println("TODO DELETE ME");
//
//
//        // TODO - check with beyraf - his refactored doesn't work - not sure why it doesn't find symbol
////        PureModelContextData pmcd = PureGrammarParser.newInstance(PureGrammarParserExtensions.fromAvailableExtensions()).parseModel(testGrammar);
////        Lambda lambda = PureGrammarParser.newInstance().parseLambda("|#>{test::DeephavenStore.nds_desktops}#->select()->from(test::DeephavenRuntime)");
////        PureModel pureModel = Compiler.compile(pmcd, DeploymentMode.TEST, "user");
////        LambdaFunction<?> lambdaFunction = HelperValueSpecificationBuilder.buildLambda(lambda, pureModel.getContext());
////        RichIterable<? extends Root_meta_pure_extension_Extension> pureExtensions = PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport()));
////        String result = core_external_execution_execution.Root_meta_legend_execute_FunctionDefinition_1__Pair_MANY__ExecutionContext_1__Extension_MANY__String_1_(
////                lambdaFunction,
////                Lists.mutable.empty(),
////                new Root_meta_pure_runtime_ExecutionContext_Impl(""),
////                pureExtensions,
////                pureModel.getExecutionSupport()
////        );
////
//        //TODO: ask beyraf or check later - doesn't work as can't find symbol pureExtensions(CompiledExecutionSupport)
//        // PureCoreExtensionLoader.pureExtensions(pureModel.getExecutionSupport()),
////        // TODO - add the expected data here to assert JSON equals for the select
////        // TODO - add a test to filter by column
//    }
//
//    @Test
//    public void deephavenSession()
//    {
//        try
//        {
//            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
//            DeephavenTarget deephavenTarget = DeephavenTarget.builder().host("localhost").port(10000).isSecure(false).build();
//            ClientConfig clientConfig = ClientConfig.builder().target(deephavenTarget).build();
//            SessionConfig sessionConfig = SessionConfig.builder()
//                    .authenticationTypeAndValue("io.deephaven.authentication.psk.PskAuthenticationHandler " + "5au9q4eu3yu8")
//                    .build();
//            RootAllocator bufferAllocator = new RootAllocator();
//            BarrageSessionFactoryConfig.Factory barrageSessionFactory = BarrageSessionFactoryConfig.builder()
//                    .clientConfig(clientConfig)
//                    .allocator(bufferAllocator)
//                    .scheduler(scheduler)
//                    .build()
//                    .factory();
////            SessionFactoryConfig.Factory factory = SessionFactoryConfig.builder()
////                    .clientConfig(clientConfig).scheduler(scheduler).sessionConfig(sessionConfig).build().factory();
////            try(Session session = factory.newSession())
//            try (BarrageSession session = barrageSessionFactory.newBarrageSession(sessionConfig))
//            {
//                System.out.println("Connected to Deephaven Server !");
////                try (ConsoleSession consoleSession = session.console("python").get())
////                {
////                    String dummyTable = "from deephaven import new_table\n" +
////                            "from deephaven.column import string_col, int_col\n" +
////                            "\n" +
////                            "source_table = new_table([\n" +
////                            "   int_col(\"Age\", [25, 30, 35])\n" +
////                            "])";
////                    consoleSession.executeCode(dummyTable);
////                }
////                catch (Exception i)
////                {
////                    i.printStackTrace();
////                }
////                String dummyTable = "def t = table('Age', [25, 30, 35])";
////                System.out.println("Attempting to execute: " + dummyTable);
////                session.console(dummyTable);
////                System.out.println("Dummy table created on the server !");
//
//                ConsoleSession pyConsole = session.session().console("python").get();
//                String readTableQuery =
////                        "from deephaven import read_table\n" +
//                                        "resultTable2 = nds_desktops";
//                Changes ch = pyConsole.executeCode(readTableQuery);
//
//                TicketId tickID = ch.changes().created().get(0).ticket();
//                try (FlightStream stream = session.stream(tickID))
//                {
//                    while (stream.next())
//                    {
//                        VectorSchemaRoot root = stream.getRoot();
//                        // Process the root which contains the rows for the current batch
//                        int rowCount = root.getRowCount();
//                        for (int i = 0; i < rowCount; i++)
//                        {
//                            String colValue = root.getVector("Data").getObject(i).toString();
//                            System.out.println("Row " + i + ": " + colValue);
//                        }
//                    }
//                }
//                System.out.println("End of Query");
//            }
//            catch (Exception e)
//            {
//                e.printStackTrace();
//            }
//        }
//        catch (Exception e)
//        {
//            e.printStackTrace();
//        }
//    }
//}
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
