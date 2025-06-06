// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.targetshape;

import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.deduplication.DeduplicationStrategy;

import java.util.List;

public class MultiFlatTargetPart
{
    public String modelProperty;
    public String targetName;
    public List<String> partitionFields;
    public DeduplicationStrategy deduplicationStrategy;
    public SourceInformation sourceInformation;
}