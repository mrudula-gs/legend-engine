// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.test;

import java.util.ServiceLoader;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.stores.relational.SnowflakeConnectionExtension;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionExtension;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.strategic.StrategicConnectionExtension;
import org.junit.Assert;
import org.junit.Test;

public class TestSnowflakeConnectionExtensionsAvailable
{
    @Test
    public void testConnectionExtensionsAvailable()
    {
        MutableList<Class<?>> connectionExtensions =
                Lists.mutable.withAll(ServiceLoader.load(ConnectionExtension.class))
                        .collect(Object::getClass);
        Assert.assertTrue(connectionExtensions.contains(SnowflakeConnectionExtension.class));

        MutableList<Class<?>> strategicConnectionExtensions =
                Lists.mutable.withAll(ServiceLoader.load(StrategicConnectionExtension.class))
                        .collect(Object::getClass);
        Assert.assertTrue(strategicConnectionExtensions.contains(SnowflakeConnectionExtension.class));
    }
}
