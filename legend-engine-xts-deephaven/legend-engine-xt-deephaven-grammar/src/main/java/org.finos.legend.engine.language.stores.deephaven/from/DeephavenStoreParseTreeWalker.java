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

    // TODO: ask beyraf - why does DefinitionContext have imports? This is Generated from Antlr
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

    private void visit(List<DeephavenParserGrammar.deephavenStoreDefinitionContext> deephavenStoreDefinition, Consumer<PackageableElement> elementConsumer)
    {
        deephavenStoreDefinition.stream().map(this::visitDeephavenStore).forEach(elementConsumer);
    }

    private DeephavenStore visitDeephavenStore(DeephavenParserGrammar.deephavenStoreDefinitionContext ctx)
    {
        DeephavenStore store = new DeephavenStore();
        // required fields for all stores
        store.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        // TODO: ask beyraf or renu - double check why ElasticSearch wouldn't check for packagePath == null but mongo and relational do check that; when would it be null?
        store._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        store.sourceInformation = this.parserInfo.walkerSourceInformation.getSourceInformation(ctx);

        // fields defined for deephaven
        store.tables = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.tables(), )

    }

    private Table visitTable(DeephavenParserGrammar.TableContext ctx)
    {
        Table table = new Table();
        table.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        table.name = ctx.relationalIdentifier().QUOTED_STRING() == null ? ctx.relationalIdentifier().unquotedIdentifier().getText() : ctx.relationalIdentifier().QUOTED_STRING().getText();
        List<String> primaryKeys = new ArrayList<>();
        table.columns = ListIterate.collect(ctx.columnDefinition(), columnDefinitionContext -> this.visitColumnDefinition(columnDefinitionContext, primaryKeys));
        table.primaryKey = primaryKeys;
        if (ctx.milestoneSpec() != null)
        {
            table.milestoning = ListIterate.collect(ctx.milestoneSpec().milestoning(), this::visitMilestoning);
        }
        return table;
    }
}