// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.language.stores.deephaven;

public class DeephavenStoreParseTreeWalker
{
    private final SourceCodeParserInfo parserInfo;
    private final PropertyDefinitionParseTreeWalker propertyDefinitionParseTreeWalker = new PropertyDefinitionParseTreeWalker();
    private final PureGrammarParserExtensions extension;

    public DeephavenStoreParseTreeWalker(PureGrammarParserExtensions extension, SourceCodeParserInfo parserInfo)
    {
        this.parserInfo = parserInfo;
        this.extension = extension;
    }

    // TODO: ask beyraf - why does DefinitionContext have imports? generated from Pure code but what is purpose and where is that code generated?
    // e.g. what generates this code? "public static class ComplexPropertyTypesContext extends ParserRuleContext "?
    public Section visit(String sectionType, Consumer<PackageableElement> elementConsumer, DeephavenParserGrammar.DefinitionContext definitionContext)
    {
        ImportAwareCodeSection section = new ImportAwareCodeSection();
        section.parserName = sectionType;
        section.sourceInformation = parserInfo.sourceInformation;
        section.imports = ListIterate.collect(definitionContext.imports().importStatement(), importCtx -> PureGrammarParserUtility.fromPath(importCtx.packagePath().identifier()));

        Consumer<PackageableElement> sectionConsumer = x ->
        {
            section.elements.add(x.getPath());
            elementConsumer.accept(x);
        };

        this.visit(definitionContext.deephavenStoreDefinition(), sectionConsumer);

        return section;

    }
}