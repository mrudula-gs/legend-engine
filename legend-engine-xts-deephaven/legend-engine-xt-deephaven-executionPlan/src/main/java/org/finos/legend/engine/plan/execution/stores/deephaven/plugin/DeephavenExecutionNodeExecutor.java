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
//

package org.finos.legend.engine.plan.execution.stores.deephaven.plugin;

import io.deephaven.client.impl.*;
import io.deephaven.uri.DeephavenTarget;
import org.apache.arrow.flight.FlightStream;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.builder.stream.StreamBuilder;
import org.finos.legend.engine.plan.execution.result.object.StreamingObjectResult;
import org.finos.legend.engine.plan.execution.stores.deephaven.result.DeephavenStreamingResult;
import org.finos.legend.engine.protocol.deephaven.metamodel.executionPlan.DeephavenExecutionNode;
import org.finos.legend.engine.protocol.deephaven.metamodel.executionPlan.DeephavenJavaCode;
import org.finos.legend.engine.protocol.deephaven.metamodel.runtime.DeephavenConnection;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNodeVisitor;
import org.finos.legend.engine.shared.core.identity.Identity;
import io.deephaven.client.impl.BarrageSession;
import org.finos.legend.engine.shared.javaCompiler.EngineJavaCompiler;
import org.finos.legend.engine.shared.javaCompiler.StringJavaSource;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.deephaven.client.impl.TableHandle;

public class DeephavenExecutionNodeExecutor implements ExecutionNodeVisitor<Result>
{
    private final Identity identity;
    private final ExecutionState executionState;
    private final DeephavenStoreState state;

    private static final String CLASS_TEMPLATE = "package org.finos.legend.engine.generated;\n\n" +
                                                 "%s\n" +
                                                 "public class DeephavenGeneratedQuery {\n" +
                                                 "    public static TableHandle execute(BarrageSession session) throws TableHandle.TableHandleException, InterruptedException {\n" +
                                                 "        %s;\n" +
                                                 "        return session.session().execute(ts);\n" +
                                                 "        }\n" +
                                                 "}\n";
    private static final String IMPORT_TEMPLATE = "import %s;";

    public DeephavenExecutionNodeExecutor(Identity identity, ExecutionState executionState, DeephavenStoreState state)
    {
        this.identity = identity;
        this.executionState = executionState;
        this.state = state;
    }

    @Override
    public Result visit(ExecutionNode executionNode)
    {
        if (executionNode instanceof DeephavenExecutionNode)
        {
            DeephavenExecutionNode deephavenNode = (DeephavenExecutionNode) executionNode;
            DeephavenConnection connection = deephavenNode.connection;
            DeephavenJavaCode dhQuery = deephavenNode.generatedJava;

            String imports = dhQuery.imports.stream().map(imp -> String.format(IMPORT_TEMPLATE, imp)).collect(Collectors.joining("\n"));
            String sourceCode = String.format(CLASS_TEMPLATE, imports, dhQuery.code);

            String fullClassName = "org.finos.legend.engine.generated.DeephavenGeneratedQuery";
            String pkgName = "org.finos.legend.engine.generated";
            String className = "DeephavenGeneratedQuery";

            try
            {
                StringJavaSource javaSource = new StringJavaSource(pkgName, className)
                {
                    @Override
                    public String getCode()
                    {
                        return sourceCode;
                    }

                    @Override
                    public int size()
                    {
                        return 0;
                    }
                };
                EngineJavaCompiler compiler = new EngineJavaCompiler();
                compiler.compile(Collections.singletonList(javaSource));
                Class<?> queryClass = compiler.getClassLoader().loadClass(fullClassName);

                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
                DeephavenTarget deephavenTarget = DeephavenTarget.builder().host("localhost").port(10000).isSecure(false).build();
                ClientConfig clientConfig = ClientConfig.builder().target(deephavenTarget).build();
                SessionConfig sessionConfig = SessionConfig.builder()
                        .authenticationTypeAndValue("io.deephaven.authentication.psk.PskAuthenticationHandler " + "122wi2fgkp76s")
                        .build();
                RootAllocator bufferAllocator = new RootAllocator();
                BarrageSessionFactoryConfig.Factory barrageSessionFactory = BarrageSessionFactoryConfig.builder()
                        .clientConfig(clientConfig)
                        .allocator(bufferAllocator)
                        .scheduler(scheduler)
                        .build()
                        .factory();
                try (BarrageSession session = barrageSessionFactory.newBarrageSession(sessionConfig))
                {
                    System.out.println("Connected to Deephaven Server !");
                    Method executeMethod = queryClass.getMethod("execute", BarrageSession.class);
                    TableHandle table = (TableHandle) executeMethod.invoke(null, session);

                    List<Map<String, Object>> rows = new ArrayList<>();
                    try (FlightStream flightStream = session.stream(table.ticketId()))
                    {
                        VectorSchemaRoot root = flightStream.getRoot();
                        while (flightStream.next())
                        {
                            for (int i = 0; i < root.getRowCount(); i++)
                            {
                                Map<String, Object> row = new HashMap<>();
                                for (FieldVector vector : root.getFieldVectors())
                                {
                                    String columnName = vector.getName();
                                    Object value = vector.getObject(i);
                                    row.put(columnName, value);
                                }
                                rows.add(row);
                            }
                        }
                        Stream<Map<String, Object>> rowStream = rows.stream();
                        return new StreamingObjectResult<>(rowStream, new StreamBuilder(), new DeephavenStreamingResult(rows));
                    }
                }
                finally
                {
                    scheduler.shutdown();
                    bufferAllocator.close();
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        throw new IllegalStateException("DEEPHAVEN: should not get here");
    }

//    @Override
//    public Result visit(ExecutionNode executionNode)
//    {
//        if (executionNode instanceof DeephavenExecutionNode)
//        {
//            DeephavenExecutionNode deephavenNode = (DeephavenExecutionNode) executionNode;
//            DeephavenConnection connection = deephavenNode.connection;
//            String composedQuery = deephavenNode.deepHavenQuery;
//
//            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
//            DeephavenTarget deephavenTarget = DeephavenTarget.builder().host("localhost").port(10000).isSecure(false).build();
//            ClientConfig clientConfig = ClientConfig.builder().target(deephavenTarget).build();
//            SessionConfig sessionConfig = SessionConfig.builder()
//                    .authenticationTypeAndValue("io.deephaven.authentication.psk.PskAuthenticationHandler " + "122wi2fgkp76s")
//                    .build();
//            RootAllocator bufferAllocator = new RootAllocator();
//            BarrageSessionFactoryConfig.Factory barrageSessionFactory = BarrageSessionFactoryConfig.builder()
//                    .clientConfig(clientConfig)
//                    .allocator(bufferAllocator)
//                    .scheduler(scheduler)
//                    .build()
//                    .factory();
//            try (BarrageSession session = barrageSessionFactory.newBarrageSession(sessionConfig))
//            {
//                System.out.println("Connected to Deephaven Server !");
//                ConsoleSession pyConsole = session.session().console("python").get();
//                String uniqueResVar = "resultTable_" + UUID.randomUUID().toString().replace("-", "");
//                String imports = "from deephaven import SortDirection\n" +
//                                 "\n";
//                String readTableQuery = imports + uniqueResVar + " = " + composedQuery;
//                Changes ch = pyConsole.executeCode(readTableQuery);
//                if (!("Optional.empty".equals(String.valueOf(ch.errorMessage())))) //  && !ch.errorMessage().isEmpty()
//                {
////                    System.err.println("Deephaven Query Execution Error: " + ch.errorMessage());
//                    throw new IllegalStateException("Deephaven Query Execution Error: " + ch.errorMessage());
//                }
//                TicketId tickID = ch.changes().created().get(0).ticket();
//                List<Map<String, Object>> rows = new ArrayList<>();
//                try (FlightStream flightStream = session.stream(tickID))
//                {
//                    VectorSchemaRoot root = flightStream.getRoot();
//                    while (flightStream.next())
//                    {
//                        for (int i = 0; i < root.getRowCount(); i++)
//                        {
//                            Map<String, Object> row = new HashMap<>();
//                            for (FieldVector vector : root.getFieldVectors())
//                            {
//                                String columnName = vector.getName();
//                                Object value = vector.getObject(i);
//                                row.put(columnName, value);
//                            }
//                            rows.add(row);
//                        }
//                    }
//                    Stream<Map<String, Object>> rowStream = rows.stream();
//                    return new StreamingObjectResult<>(rowStream, new StreamBuilder(), new DeephavenStreamingResult(rows));
//                }
//            }
//            catch (Exception e)
//            {
//                e.printStackTrace();
//                throw new IllegalStateException("DEEPHAVEN: should not get here");
//            }
//        }
//        throw new IllegalStateException("DEEPHAVEN: should not get here");
//    }
}