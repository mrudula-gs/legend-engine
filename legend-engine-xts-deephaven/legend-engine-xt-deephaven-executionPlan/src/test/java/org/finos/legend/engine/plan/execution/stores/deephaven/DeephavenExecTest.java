// Copyright 2025 Goldman Sachs
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

import io.deephaven.client.impl.*;
import io.deephaven.qst.table.TableSpec;
import io.deephaven.qst.table.TicketTable;
import io.deephaven.uri.DeephavenTarget;
import org.apache.arrow.flight.FlightStream;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.finos.legend.engine.shared.javaCompiler.EngineJavaCompiler;
import org.finos.legend.engine.shared.javaCompiler.JavaCompileException;
import org.finos.legend.engine.shared.javaCompiler.StringJavaSource;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class DeephavenExecTest
{
    @Test
    public void deephavenExecJava() throws JavaCompileException, ClassNotFoundException
    {
        String pkgName = "org.finos.legend.engine.generated";
        String fullClassName = "org.finos.legend.engine.generated.DeephavenGeneratedQuery";
        String className = "DeephavenGeneratedQuery";
        String sourceCode = "package org.finos.legend.engine.generated;\n\n" +
                "import io.deephaven.client.impl.BarrageSession;\n" +
                "import io.deephaven.client.impl.TableHandle;\n" +
                "import io.deephaven.qst.table.TableSpec;\n" +
                "import io.deephaven.qst.table.TicketTable;\n" +
                "public class DeephavenGeneratedQuery {\n" +
                "    public static TableHandle execute(BarrageSession session) throws TableHandle.TableHandleException, InterruptedException {\n" +
                "        TableSpec ts = TicketTable.fromQueryScopeField(\"nds_desktops\").select(\"Desktop_ID\", \"City_Name\");\n" +
                "        return session.session().execute(ts);\n" +
                "        }\n" +
                "}\n";
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
        Class<?> queryClass = compiler.getClassLoader().loadClass(className);
    }


    @Test
    public void deephavenSession()
    {
        try
        {
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
//                TableSpec ts = TicketTable.fromQueryScopeField("nds_desktops").select("City_Name");
//                TableSpec ts = TicketTable.fromQueryScopeField("nds_desktops").where("City_Name == `Dallas`");
//                TableSpec ts = TicketTable.fromQueryScopeField("nds_desktops").where("City_Name == `Dallas` && Desktop_ID == 1");
//                TableSpec ts = TicketTable.fromQueryScopeField("nds_desktops").where("City_Name == `Dallas` || Desktop_ID == 2");
//                TableSpec ts = TicketTable.fromQueryScopeField("nds_desktops").where("City_Name in `Dallas`, `New York`");
                TableSpec ts = TicketTable.fromQueryScopeField("nds_desktops").where("City_Name != `Dallas`");
//                TableSpec ts = TicketTable.fromQueryScopeField("nds_desktops").sort();
                TableHandle th = session.session().execute(ts);
                TicketId tickID = th.ticketId();

                try (FlightStream stream = session.stream(tickID))
                {
                    while (stream.next())
                    {
                        VectorSchemaRoot root = stream.getRoot();
                        // Process the root which contains the rows for the current batch
                        int rowCount = root.getRowCount();
                        for (int i = 0; i < rowCount; i++)
                        {
                            for (FieldVector vector : root.getFieldVectors())
                            {
                                String columnName = vector.getName();
                                Object value = vector.getObject(i);
                                System.out.println("Row : " + i + ", " + "Column : " + columnName + " == " + value);
                            }
                        }
                    }
                }
                System.out.println("End of Query");
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
