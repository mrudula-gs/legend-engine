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

import io.deephaven.uri.DeephavenTarget;
import org.finos.legend.engine.protocol.deephaven.metamodel.runtime.DeephavenSourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.PSKAuthenticationSpecification;

import java.util.Optional;

public class DeephavenSessionPSKProvider implements DeephavenSessionProvider
{
    // TODO: anumam - move target prefix to be a part of the SourceSpecification - and accommodate different types
    private final String targetPrefix = "dh+plain://";
    private final String authType = "io.deephaven.authentication.psk.PskAuthenticationHandler ";

    @Override
    public Optional<DeephavenSession> provide(PSKAuthenticationSpecification pskAuth, DeephavenSourceSpecification sourceSpec)
    {
        // FIXME: anumam - fix the below
        DeephavenSessionContext deephavenSessionContext = new DeephavenSessionContext(pskAuth, sourceSpec);
        return Optional.of(deephavenSessionContext)
                .filter(dsc -> dsc.pskAuth instanceof PSKAuthenticationSpecification)
                .filter(dsc -> dsc.sourceSpec instanceof DeephavenSourceSpecification)
                .map(this::createDeephavenSession);
    }

    private DeephavenSession createDeephavenSession(DeephavenSessionContext deephavenSessionContext)
    {
        // TODO: anumam - should this include target prefix
        DeephavenTarget target = DeephavenTarget.builder().host(deephavenSessionContext.sourceSpec.url.getHost()).port(deephavenSessionContext.sourceSpec.url.getPort()).isSecure(false).build(); // dh+plain://localhost:10000
        // String authTypeAndPSK = this.authType + deephavenSessionContext.pskAuth.psk;
        String authTypeAndPSK = this.authType + "122wi2fgkp76s";
        return new DeephavenSession(target, authTypeAndPSK);
    }

    // FIXME: anumam - fix this, this is a hack
    public class DeephavenSessionContext
    {
        private final PSKAuthenticationSpecification pskAuth;
        private final DeephavenSourceSpecification sourceSpec;

        private DeephavenSessionContext(PSKAuthenticationSpecification pskAuth, DeephavenSourceSpecification sourceSpec)
        {
            this.pskAuth = pskAuth;
            this.sourceSpec = sourceSpec;
        }
    }
}


