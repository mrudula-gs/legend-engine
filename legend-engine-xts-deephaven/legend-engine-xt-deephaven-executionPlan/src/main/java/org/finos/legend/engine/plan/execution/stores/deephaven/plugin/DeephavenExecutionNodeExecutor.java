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

import org.apache.arrow.flight.FlightStream;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.ExecutionNodeJavaPlatformHelper;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.JavaHelper;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.builder.stream.StreamBuilder;
import org.finos.legend.engine.plan.execution.result.object.StreamingObjectResult;
import org.finos.legend.engine.plan.execution.stores.deephaven.connection.DeephavenSession;
import org.finos.legend.engine.plan.execution.stores.deephaven.result.DeephavenStreamingResult;
import org.finos.legend.engine.plan.execution.stores.deephaven.specifics.IDeephavenExecutionNodeSpecifics;
import org.finos.legend.engine.protocol.deephaven.metamodel.executionPlan.DeephavenExecutionNode;
import org.finos.legend.engine.protocol.deephaven.metamodel.runtime.DeephavenConnection;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNodeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.JavaPlatformImplementation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.PSKAuthenticationSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionCategory;
import io.deephaven.client.impl.BarrageSession;
import io.deephaven.client.impl.TableHandle;

// get rid of * and add specific imports
import java.util.*;
import java.util.stream.Stream;

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
            return executeDeephavenNode((DeephavenExecutionNode) executionNode);
        }
        throw new IllegalStateException("DEEPHAVEN: Unexpected node type");
    }

    private Result executeDeephavenNode(DeephavenExecutionNode node)
    {
        try
        {
            String specificsClassName = JavaHelper.getExecutionClassFullName((JavaPlatformImplementation) node.implementation);
            Class<?> specificsClass = ExecutionNodeJavaPlatformHelper.getClassToExecute(node, specificsClassName, executionState, identity);
            IDeephavenExecutionNodeSpecifics specifics = (IDeephavenExecutionNodeSpecifics) specificsClass.getConstructor().newInstance();
            DeephavenSession deephavenSession = createDeephavenSession(node.connection);
            try (BarrageSession session = deephavenSession.getBarrageSession())
            {
                System.out.println("Connected to Deephaven Server !");
                TableHandle table = specifics.execute(session);
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
                }
                Stream<Map<String, Object>> rowStream = rows.stream();
                return new StreamingObjectResult<>(rowStream, new StreamBuilder(), new DeephavenStreamingResult(rows));
            }
            finally
            {
                deephavenSession.close();
            }
        }
        catch (Exception e)
        {
            throw new EngineException("Error executing Deephaven Query", e, ExceptionCategory.USER_EXECUTION_ERROR);
        }
    }

    private DeephavenSession createDeephavenSession(DeephavenConnection connection)
    {
        return this.state.getProviders().stream()
                .map(x -> x.provide((PSKAuthenticationSpecification) connection.authSpec, connection.sourceSpec))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findAny()
                .orElseThrow(() -> new EngineException("Unable to create a Deephaven Session", ExceptionCategory.USER_CREDENTIALS_ERROR));
    }
}
