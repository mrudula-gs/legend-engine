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

package org.finos.legend.engine.generation;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.generation.extension.Artifact;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ArtifactGenerationExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.pure.generated.Root_meta_external_powerbi_transformation_fromPure_PowerBIArtifactGenerationOutput;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_DataSpace;
import org.finos.legend.pure.generated.core_external_format_powerbi_transformation_fromPure_pureToPBIP;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

import java.util.ArrayList;
import java.util.List;

public class PowerBIArtifactGenerationExtension implements ArtifactGenerationExtension
{

    private static final String ROOT_PATH = "powerbi_artifacts";

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Generation", "Artifact", "PowerBI");
    }

    @Override
    public String getKey()
    {
        return ROOT_PATH;
    }

    @Override
    public boolean canGenerate(PackageableElement element)
    {
        return element instanceof Root_meta_pure_metamodel_dataSpace_DataSpace && checkIfPowerBIArtifactGenerationProfileIsPresent(element);
    }

    private boolean checkIfPowerBIArtifactGenerationProfileIsPresent(PackageableElement element)
    {
        return element._stereotypes() != null && element._stereotypes().anySatisfy(stereotype ->
                stereotype._profile()._name().equals("PowerBIArtifactGeneration") &&
                        stereotype._profile()._p_stereotypes().anySatisfy(s -> s.getName().equals("DirectQuery") || s.getName().equals("Import"))
        );
    }

    @Override
    public List<Artifact> generate(PackageableElement element, PureModel pureModel, PureModelContextData data, String clientVersion)
    {
        List<Artifact> powerBIArtifacts = new ArrayList<>();
        Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions = (PureModel p) -> PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(p.getExecutionSupport()));
        RichIterable<? extends Root_meta_external_powerbi_transformation_fromPure_PowerBIArtifactGenerationOutput> powerBIArtifactGenerationOutputs = core_external_format_powerbi_transformation_fromPure_pureToPBIP.Root_meta_external_powerbi_transformation_fromPure_generatePowerBIArtifacts_DataSpace_1__Extension_MANY__PowerBIArtifactGenerationOutput_MANY_((Root_meta_pure_metamodel_dataSpace_DataSpace) element, routerExtensions.apply(pureModel), pureModel.getExecutionSupport());

        powerBIArtifactGenerationOutputs.forEach(artifactGenerationOutput -> powerBIArtifacts.add(convertToArtifact(artifactGenerationOutput)));
        return powerBIArtifacts;
    }

    private Artifact convertToArtifact(Root_meta_external_powerbi_transformation_fromPure_PowerBIArtifactGenerationOutput artifactGenerationOutput)
    {
        return new Artifact(artifactGenerationOutput._content(), artifactGenerationOutput._fileName(), artifactGenerationOutput._format());
    }
}
