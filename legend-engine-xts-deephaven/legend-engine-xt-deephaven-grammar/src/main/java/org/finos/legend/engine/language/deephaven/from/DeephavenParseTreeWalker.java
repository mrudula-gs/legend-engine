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

package org.finos.legend.engine.language.deephaven.from;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.SourceCodeParserInfo;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.DeephavenLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.DeephavenParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.DeephavenParserGrammarBaseVisitor;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.DeephavenConnectionLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.DeephavenConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;

import org.finos.legend.engine.protocol.deephaven.metamodel.store.DeephavenStore;
import org.finos.legend.engine.protocol.deephaven.metamodel.store.Table;
import org.finos.legend.engine.protocol.deephaven.metamodel.store.Column;

import org.finos.legend.engine.protocol.deephaven.metamodel.type.BooleanType;
import org.finos.legend.engine.protocol.deephaven.metamodel.type.IntType;
import org.finos.legend.engine.protocol.deephaven.metamodel.type.StringType;
import org.finos.legend.engine.protocol.deephaven.metamodel.type.TimeType;
import org.finos.legend.engine.protocol.deephaven.metamodel.type.Type;
import org.finos.legend.engine.protocol.deephaven.metamodel.type.TypeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;

import java.util.stream.Collectors;
import java.util.List;
import java.util.function.Consumer;

public class DeephavenParseTreeWalker
{
    private final SourceCodeParserInfo parserInfo;
    private final PureGrammarParserExtensions extension;

    public DeephavenParseTreeWalker(PureGrammarParserExtensions extension, SourceCodeParserInfo parserInfo)
    {
        this.parserInfo = parserInfo;
        this.extension = extension;
    }

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

        this.visit(definitionContext.deephavenDefinition(), sectionConsumer);

        return section;
    }

    private void visit(List<DeephavenParserGrammar.DeephavenDefinitionContext> deephavenStoreDefinition, Consumer<PackageableElement> elementConsumer)
    {
        deephavenStoreDefinition.stream().map(this::visitDeephavenStore).forEach(elementConsumer);
    }

    private DeephavenStore visitDeephavenStore(DeephavenParserGrammar.DeephavenDefinitionContext ctx)
    {
        DeephavenStore store = new DeephavenStore();
        // required fields for all stores
        store.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        store._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        store.sourceInformation = this.parserInfo.walkerSourceInformation.getSourceInformation(ctx);

        // fields defined for deephaven
        // TODO: ask beyraf/check undertanding - validateAndExtractRequiredField only does "return contexts.get(0);" so what about the rest of the items? or because tables is the entire set of tableDefinitions (by the way it's defined in parser grammar so there is only 1)?
        store.tables = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.tables(), DeephavenLexerGrammar.VOCABULARY.getLiteralName(DeephavenLexerGrammar.TABLES), store.sourceInformation)
                .tableDefinition()
                .stream()
                .map(this::visitTableDefinition)
                .collect(Collectors.toList());
        return store;
    }

    private Table visitTableDefinition(DeephavenParserGrammar.TableDefinitionContext tableDefinitionContext)
    {
        Table table = new Table();
        table.columns = PureGrammarParserUtility.validateAndExtractRequiredField(
                    tableDefinitionContext.columns(),
                    DeephavenLexerGrammar.VOCABULARY.getLiteralName(DeephavenLexerGrammar.COLUMNS),
                    this.parserInfo.walkerSourceInformation.getSourceInformation(tableDefinitionContext)
                )
                .columnDefinition()
                .stream()
                .map(this::visitColumn)
                .collect(Collectors.toList());
        return table;
    }

    private Column visitColumn(DeephavenParserGrammar.ColumnDefinitionContext columnDefinitionContext)
    {
        Column column = new Column();
        column.name = PureGrammarParserUtility.fromIdentifier(columnDefinitionContext.columnName());
        column.type = new ColumnDefinitionParseTreeWalker().visitColumnType(columnDefinitionContext.columnType());
        return column;
    }

    private class ColumnDefinitionParseTreeWalker extends DeephavenParserGrammarBaseVisitor<Type>
    {
        @Override
        public Type visitColumnType(DeephavenParserGrammar.ColumnTypeContext columnTypeContext)
        {
            TerminalNode type = columnTypeContext.getChild(TerminalNode.class, 0);

            Type columnType = null;

            // TODO: tamimi - consider... should we raise exception for unknown type (null type)
            switch (type.getSymbol().getType())
            {
                case DeephavenParserGrammar.DATE_TIME:
                    columnType = new TimeType();
                    break;
                case DeephavenParserGrammar.STRING:
                    columnType = new StringType();
                    break;
                case DeephavenParserGrammar.INT:
                    columnType = new IntType();
                    break;
                case DeephavenParserGrammar.BOOLEAN:
                    columnType = new BooleanType();
                    break;
            }

            return columnType;
        }
    }
}