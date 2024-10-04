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
                .target(DeephavenTarget.of(URI.create(connStr.get_dhtarget())))
                .build();

        // SessionConfig describes the configuration needed to create a session
        SessionConfig sessionConfig = SessionConfig.builder()
                // TODO: make this neater
                .authenticationTypeAndValue("io.deephaven.authentication.psk.PskAuthenticationHandler " + connStr.get_psk())
                .build();

        // Create a FlightSessionFactory. This stitches together the above components to create the real, live
        // API session with the server.
        BarrageSessionFactoryConfig.Factory barrageSessionFactory = BarrageSessionFactoryConfig.builder()
                .clientConfig(clientConfig)
                .allocator(bufferAllocator)
                .scheduler(this._threadPool)
                .build()
                .factory();

        this._barrageSession = barrageSessionfactory.newBarrageSession(this._sessionConfig);
        this.clientSession = this._barrageSession.session();

    }

    public void close() throws Exception
    {
        this._barrageSession.close();
        this.clientSession.close();
        this._threadPool.shutdown();
    }
}
