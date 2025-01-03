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

package org.finos.legend.engine.plan.execution.stores.deephaven.plugin;

import io.deephaven.client.impl.*;
import io.deephaven.client.impl.script.Changes;
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
import org.finos.legend.engine.protocol.deephaven.metamodel.runtime.DeephavenConnection;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNodeVisitor;

import org.finos.legend.engine.shared.core.identity.Identity;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class DeephavenExecutionNodeExecutor implements ExecutionNodeVisitor<Result>
{
    private final Identity identity;
    private final ExecutionState executionState;
    private final DeephavenStoreState state;

    // TODO: abcdef - check if can use TDS result in future
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
//            System.out.println(System.getProperty("java.class.path"));
            DeephavenExecutionNode deephavenNode = (DeephavenExecutionNode) executionNode;
            DeephavenConnection connection = deephavenNode.connection;
            String deephavenCommand = deephavenNode.deepHavenQuery;

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
                ConsoleSession pyConsole = session.session().console("python").get();
//                        "from deephaven import read_table\n" +
                String uniqueResVar = "resultTable_" + UUID.randomUUID().toString().replace("-", "");
                String readTableQuery = uniqueResVar + " = " + deephavenCommand;
                Changes ch = pyConsole.executeCode(readTableQuery);

                if (!("Optional.empty".equals(String.valueOf(ch.errorMessage())))) //  && !ch.errorMessage().isEmpty()
                {
//                    System.err.println("Deephaven Query Execution Error: " + ch.errorMessage());
                    throw new IllegalStateException("Deephaven Query Execution Error: " + ch.errorMessage());
                }

                TicketId tickID = ch.changes().created().get(0).ticket();
                List<Map<String, Object>> rows = new ArrayList<>();
//                List<Object[]> rowList = new ArrayList<>();
                try (FlightStream flightStream = session.stream(tickID))
                {

//                    Stream<Map<String, Object>> resultStream = StreamSupport.stream(new DeephavenRowSpliterator(flightStream), false);
//                    System.out.println("End of Query, Streaming Results...");
//                    return new DeephavenStreamingResult(resultStream);
//                    return new StreamingObjectResult<>(resultStream, mongoDBResult.getResultBuilder(), mongoDBResult);

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

//                    List<FieldVector> fieldVectors = root.getFieldVectors();
//                    long rowCount = root.getRowCount();
//                    for (int i = 0; i < rowCount; i++)
//                    {
//                        Map<String, Object> rowMap = new HashMap<>();
//                        Object[] row = new Object[fieldVectors.size()];
//                        for (int j = 0; j < fieldVectors.size(); j++)
//                        {
//                            FieldVector fieldVector = fieldVectors.get(j);
//                            String columnName = fieldVector.getField().getName();
//                            Object value = fieldVector.getObject(i);
//
//                            row[j] = value;
//                            rowMap.put(columnName, value);
//                        }
//                        rows.add(rowMap);
//                        rowList.add(row);
//                    }
//                    Stream<Object[]> rowStream = rowList.stream();
//                    return new StreamingObjectResult<>(rowStream, new StreamBuilder(), new DeephavenStreamingResult(rows));


//                    while (stream.next())
//                    {
//                        // Process the root which contains the rows for the current batch
////                        int rowCount = root.getRowCount();
////                        for (int i = 0; i < rowCount; i++) {
////                            String colValue = root.getVector("Data").getObject(i).toString();
////                            System.out.println("Row " + i + ": " + colValue);
//                        for (int i = 0; i < root.getRowCount(); i++)
//                        {
//                            Map<String, Object> row = new HashMap<>();
//                            for (FieldVector vector : root.getFieldVectors())
//                            {
//                                row.put(vector.getName(), vector.getObject(i));
//                            }
//                            rows.add(row);
//                        }
//                    }

//                    Stream<Object[]> rowStream = Stream.iterate(0, i -> i + 1)
//                            .limit(root.getRowCount())
//                            .map(i ->
//                            {
//                                Object[] row = new Object[fieldVectors.size()];
//                                Map<String, Object> rowMap = new HashMap<>();
//                                for (int j = 0; j < fieldVectors.size(); j++)
//                                {
//                                    FieldVector fieldVector = fieldVectors.get(j);
//                                    String columnName = fieldVector.getField().getName();
//                                    Object value = fieldVector.getObject(i);
//                                    row[j] = value;
//                                    rowMap.put(columnName, value);
//                                }
//                                rows.add(rowMap);
//                                return row;
//                            });
//                    return new StreamingObjectResult<>(rowStream, new StreamBuilder(), new DeephavenStreamingResult(rows));


                }
//                System.out.println("End of Query");
//                return new DeephavenStreamingResult(rows);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                throw new IllegalStateException("DEEPHAVEN: should not get here");
            }
        }
        throw new IllegalStateException("DEEPHAVEN: should not get here");
    }

//    private static class DeephavenRowSpliterator implements Spliterator<Map<String, Object>>
//    {
//        private final FlightStream flightStream;
//        private VectorSchemaRoot currentRoot;
//        private int currentRow;
//
//        private DeephavenRowSpliterator(FlightStream flightStream)
//        {
//            this.flightStream = flightStream;
//            this.currentRow = 0;
//        }
//
//        @Override
//        public boolean tryAdvance(Consumer<? super Map<String, Object>> action)
//        {
//            if (currentRoot == null || currentRow >= currentRoot.getRowCount())
//            {
//                if (!flightStream.next())
//                {
//                    return false; // No more rows
//                }
//                currentRoot = flightStream.getRoot();
//                currentRow = 0;
//            }
//            Map<String, Object> row = new HashMap<>();
//            for (FieldVector vector : currentRoot.getFieldVectors())
//            {
//                row.put(vector.getName(), vector.getObject(currentRow));
//            }
//            currentRow++;
//            action.accept(row);
//            return true;
//        }
//
//        @Override
//        public Spliterator<Map<String, Object>> trySplit()
//        {
//            return null;
//        }
//
//        @Override
//        public long estimateSize()
//        {
//            return Long.MAX_VALUE;
//        }
//
//        @Override
//        public int characteristics()
//        {
//            return ORDERED | NONNULL;
//        }
//    }
}

