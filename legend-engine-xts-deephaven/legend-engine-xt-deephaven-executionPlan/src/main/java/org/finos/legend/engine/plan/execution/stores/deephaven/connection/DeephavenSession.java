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

package org.finos.legend.engine.plan.execution.stores.deephaven.connection;

import io.deephaven.client.impl.BarrageSession;
import io.deephaven.client.impl.BarrageSessionFactoryConfig;
import io.deephaven.client.impl.ClientConfig;
import io.deephaven.client.impl.Session;
import io.deephaven.client.impl.SessionConfig;
import io.deephaven.uri.DeephavenTarget;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;

import org.finos.legend.engine.protocol.deephaven.metamodel.runtime.DeephavenConnection;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class DeephavenSession
{
    final Session clientSession;
    private final BarrageSession _barrageSession;
    private final ScheduledExecutorService _threadPool;

    public DeephavenSession(String deephavenTarget, String authTypeAndValue)
    {
        // TODO: tamimi - there should be a constructor(s) that accept params "mtls" and "explicit" - however, would need to incorporate these concepts into AuthenticationSpecification first
        RootAllocator bufferAllocator = new RootAllocator();
        // Create a scheduler thread pool. This is used by the Flight session.
        this._threadPool = Executors.newScheduledThreadPool(4);

        // ClientConfig describes the configuration to connect to the host
        ClientConfig clientConfig = ClientConfig.builder()
                .target(DeephavenTarget.of(URI.create(deephavenTarget)))
                .build();

        // SessionConfig describes the configuration needed to create a session
        SessionConfig sessionConfig = SessionConfig.builder()
                // TODO: make this neater
                .authenticationTypeAndValue(authTypeAndValue)
                .build();

        // Create a FlightSessionFactory. This stitches together the above components to create the real, live
        // API session with the server.
        BarrageSessionFactoryConfig.Factory barrageSessionFactory = BarrageSessionFactoryConfig.builder()
                .clientConfig(clientConfig)
                .allocator(bufferAllocator)
                .scheduler(this._threadPool)
                .build()
                .factory();

        this._barrageSession = barrageSessionFactory.newBarrageSession(sessionConfig);
        this.clientSession = this._barrageSession.session();
    }

    public void close() throws Exception
    {
        this._barrageSession.close();
        this.clientSession.close();
        this._threadPool.shutdown();
    }
}