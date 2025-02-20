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


package org.finos.legend.engine.plan.execution.test.integration;

import com.github.dockerjava.api.model.Ulimit;
import org.finos.legend.engine.protocol.deephaven.metamodel.runtime.DeephavenConnection;
import org.finos.legend.engine.protocol.deephaven.metamodel.runtime.DeephavenSourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.PSKAuthenticationSpecification;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;

public class DeephavenTestConnectionIntegration implements TestDeephavenConnectionIntegration, TestServerResource
{
    private static final Integer DEEPHAVEN_PORT = 10_000;
    private GenericContainer<?> deephavenContainer;

    @Override
    public void setup() throws Exception
    {
        System.out.println("Starting setup of Deephaven test container");
        long start = System.currentTimeMillis();

//      String registry = System.getProperty("legend.engine.testcontainer.registry", "ghcr.io");
        String registry = "dockerhub.site.gs.com:8413";
        DockerImageName imageName = DockerImageName.parse(registry + "/deephaven/server:latest"); //.asCompatibleSubstituteFor("ghcr.io/deephaven/server:latest");

//      this.deephavenContainer = new FixedHostPortGenericContainer(imageName.toString()).withFixedExposedPort(DEEPHAVEN_PORT, DEEPHAVEN_PORT);
        this.deephavenContainer = new GenericContainer<>(imageName)
                .withExposedPorts(DEEPHAVEN_PORT)
                .withCreateContainerCmdModifier(cmd ->
                {
                    cmd.getHostConfig().withUlimits(new Ulimit[] { new Ulimit("host", 2448L, 6592L) });
                })
//              .withStartupTimeout(Duration.ofMinutes(5))
                .withLogConsumer(outputFrame ->
                {
//                  System.out.println(x.getUtf8StringWithoutLineEnding());
                    String log = outputFrame.getUtf8String();
                    System.out.println("Container: " + log);
                    // Modify the URL with correct mapped port when we see the PSK
                    if (log.contains("Connect automatically to Web UI with"))
                    {
                        String psk = log.substring(log.indexOf("psk=") + 4).trim();
                        int mappedPort = deephavenContainer.getMappedPort(DEEPHAVEN_PORT);
                        System.out.println("\n=== USE THIS URL TO CONNECT ===");
                        System.out.println("http://localhost:" + mappedPort + "/?psk=" + psk);
                        System.out.println("=============================\n");
                    }
                })
        ;
        this.deephavenContainer.start();

//      Thread.currentThread().sleep(100000L);

        long end = System.currentTimeMillis();
        System.out.println("Started Deephaven container on host: " + deephavenContainer.getHost() +
                                                         " port: " + deephavenContainer.getMappedPort(DEEPHAVEN_PORT) +
                                                         " time taken (ms): " + (end - start));
    }

    @Override
    public DeephavenConnection getConnection() throws Exception
    {
        if (!deephavenContainer.isRunning())
        {
            this.setup();
        }
        DeephavenSourceSpecification sourceSpec = new DeephavenSourceSpecification();
        String url = String.format("http://%s:%d", this.deephavenContainer.getHost(), this.deephavenContainer.getMappedPort(DEEPHAVEN_PORT));
        sourceSpec.url = URI.create(url); // "http://localhost:10000"

        PSKAuthenticationSpecification authSpec = new PSKAuthenticationSpecification();
        authSpec.psk = "5au9q4eu3yu8";

        DeephavenConnection conn = new DeephavenConnection();
        conn.sourceSpec = sourceSpec;
        conn.authSpec = authSpec;
        return conn;
    }

    @Override
    public void cleanup() throws Exception
    {
        if (this.deephavenContainer != null)
        {
            this.deephavenContainer.stop();
        }
    }

    @Override
    public void start() throws Exception
    {
        this.setup();
    }

    @Override
    public void shutDown() throws Exception
    {
        this.cleanup();
    }
}
