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

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.SourceCodeParserInfo;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.DeephavenLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.DeephavenParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.DeephavenParserGrammarBaseVisitor;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.DeephavenConnectionLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.DeephavenConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;

import java.util.List;
import java.util.function.Consumer;

public class DeephavenParseTreeWalker
{
    private final SourceCodeParserInfo parserInfo;
    //private final PropertyDefinitionParseTreeWalker propertyDefinitionParseTreeWalker = new PropertyDefinitionParseTreeWalker();
    private final PureGrammarParserExtensions extension;

    public DeephavenParseTreeWalker(PureGrammarParserExtensions extension, SourceCodeParserInfo parserInfo)
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

        this.visit(definitionContext.deephavenDefinition(), sectionConsumer);

        return section;
    }

    private void visit(List<DeephavenParserGrammar.DeephavenDefinitionContext> deephavenStoreDefinition, Consumer<PackageableElement> elementConsumer)
    {
        // TODO: ask beyraf - is the elementConsumer of the same type as deephavenStoreDefinitionContext? It is not clear from looking at the code
        //deephavenStoreDefinition.stream().map(this::visitDeephavenStore).forEach(elementConsumer);
    }

    // TODO: tamimi FIXME Hack to compile grammar module
    private String visitDeephavenStore(DeephavenParserGrammar.DeephavenDefinitionContext ctx)
    {
//        DeephavenStore store = new DeephavenStore();
//        // required fields for all stores
//        store.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
//        // TODO: ask beyraf why does he use map(PackagePathContext::identifier) instead of just using packagePath().identifier()?
//        store._package = ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().map(DeephavenParserGrammar.PackagePathContext::identifier));
//        store.sourceInformation = this.parserInfo.walkerSourceInformation.getSourceInformation(ctx);

        // fields defined for deephaven
        //store.tables = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.tables(), )

        return "";

    }

//    private class PropertyDefinitionParseTreeWalker extends DeephavenParserGrammarBaseVisitor<Property>
//    {
//        @Override
//        public Property visitScalarPropertyDefinition(DeephavenParserGrammar.ScalarPropertyDefinitionContext ctx)
//        {
//            DeephavenParserGrammar.ScalarPropertyTypesContext scalarPropertyTypesContext = ctx.scalarPropertyTypes();
//            TerminalNode type = scalarPropertyTypesContext.getChild(TerminalNode.class, 0);
//
//            Property property = new Property();
//
//            switch (type.getSymbol().getType())
//            {
//                case DeephavenParserGrammar.KEYWORD:
//                    property.keyword = new KeywordProperty();
//                    DeephavenParseTreeWalker.this.processScalarPropertyContent(ctx.scalarPropertyContent(), property.keyword);
//                    break;
//                case DeephavenParserGrammar.TEXT:
//                    property.text = new TextProperty();
//                    DeephavenParseTreeWalker.this.processScalarPropertyContent(ctx.scalarPropertyContent(), property.text);
//                    break;
//                case DeephavenParserGrammar.DATE:
//                    property.date = new DateProperty();
//                    DeephavenParseTreeWalker.this.processScalarPropertyContent(ctx.scalarPropertyContent(), property.date);
//                    break;
//                case DeephavenParserGrammar.SHORT:
//                    property._short = new ShortNumberProperty();
//                    DeephavenParseTreeWalker.this.processScalarPropertyContent(ctx.scalarPropertyContent(), property._short);
//                    break;
//                case DeephavenParserGrammar.BYTE:
//                    property._byte = new ByteNumberProperty();
//                    DeephavenParseTreeWalker.this.processScalarPropertyContent(ctx.scalarPropertyContent(), property._byte);
//                    break;
//                case DeephavenParserGrammar.INTEGER:
//                    property.integer = new IntegerNumberProperty();
//                    DeephavenParseTreeWalker.this.processScalarPropertyContent(ctx.scalarPropertyContent(), property.integer);
//                    break;
//                case DeephavenParserGrammar.LONG:
//                    property._long = new LongNumberProperty();
//                    DeephavenParseTreeWalker.this.processScalarPropertyContent(ctx.scalarPropertyContent(), property._long);
//                    break;
//                case DeephavenParserGrammar.FLOAT:
//                    property._float = new FloatNumberProperty();
//                    DeephavenParseTreeWalker.this.processScalarPropertyContent(ctx.scalarPropertyContent(), property._float);
//                    break;
//                case DeephavenParserGrammar.HALF_FLOAT:
//                    property.half_float = new HalfFloatNumberProperty();
//                    DeephavenParseTreeWalker.this.processScalarPropertyContent(ctx.scalarPropertyContent(), property.half_float);
//                    break;
//                case DeephavenParserGrammar.DOUBLE:
//                    property._double = new DoubleNumberProperty();
//                    DeephavenParseTreeWalker.this.processScalarPropertyContent(ctx.scalarPropertyContent(), property._double);
//                    break;
//                case DeephavenParserGrammar.BOOLEAN:
//                    property._boolean = new BooleanProperty();
//                    DeephavenParseTreeWalker.this.processScalarPropertyContent(ctx.scalarPropertyContent(), property._boolean);
//                    break;
//            }
//
//            return property;
//        }
//    }
//
//    private DeephavenTable visitTable(DeephavenParserGrammar.TableContext ctx)
//    {
//        Table table = new Table();
//        table.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
//        table.name = ctx.relationalIdentifier().QUOTED_STRING() == null ? ctx.relationalIdentifier().unquotedIdentifier().getText() : ctx.relationalIdentifier().QUOTED_STRING().getText();
//        List<String> primaryKeys = new ArrayList<>();
//        table.columns = ListIterate.collect(ctx.columnDefinition(), columnDefinitionContext -> this.visitColumnDefinition(columnDefinitionContext, primaryKeys));
//        table.primaryKey = primaryKeys;
//        if (ctx.milestoneSpec() != null)
//        {
//            table.milestoning = ListIterate.collect(ctx.milestoneSpec().milestoning(), this::visitMilestoning);
//        }
//        return table;
//    }
}