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

package org.finos.legend.engine.plan.execution.stores.deephaven;

import io.deephaven.client.impl.BarrageSession;
import io.deephaven.client.impl.TableHandle;
import io.deephaven.qst.table.TableSpec;
import io.deephaven.qst.table.TicketTable;
//import io.deephaven.core.client.TableOperation;
//import io.deephaven.client.api.Table;
//import io.deephaven.engine.table.Table;
//import io.deephaven.qst.TableCreator;
//import io.deephaven.qst.table.TableSchema;
//import io.deephaven.qst.table.TableSpec;
//import io.deephaven.qst.table.TableSpecFunction;
//import io.deephaven.query.QueryTables;

public class TestJavaGenCode
{
    public static TableHandle execute(BarrageSession session) throws TableHandle.TableHandleException, InterruptedException
    {

        TableSpec ts = TicketTable.fromQueryScopeField("nds_desktops").select("Desktop_IDd", "City_Name");
        return session.session().execute(ts);
//                TableHandle tmp = session.session().ticket("nds_desktops").select("City_Name");
//                TicketId tickID = tmp.ticketId();
//
//                TicketTable tmp = TicketTable.fromQueryScopeField("nds_desktops");
//                        TableSpec tmp1 = tmp.select("City_Name");
//                        TableHandle tmp2 = session.session().execute(tmp1);
//                        TicketId tickID = tmp2.ticketId();
//        TicketTable tmp = TicketTable.of("nds_desktops");
//        TableQuery query = new TableQuery().from("nds_desktops").select(Fields.col("City_Name"));
//        return query.execute(session);
    }
}
