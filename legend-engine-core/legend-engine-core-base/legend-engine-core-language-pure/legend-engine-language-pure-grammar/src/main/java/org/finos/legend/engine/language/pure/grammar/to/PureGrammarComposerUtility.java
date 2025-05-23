// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.to;

import org.apache.commons.text.StringEscapeUtils;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMapping;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PureGrammarComposerUtility
{
    private static final String PACKAGE_SEPARATOR = "::";
    private static final Pattern UNQUOTED_IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_$~]*"); // TODO

    public static final String TAB = "  ";

    public static int getTabSize()
    {
        return TAB.length();
    }

    public static int getTabSize(int repeat)
    {
        return repeat * getTabSize();
    }

    public static String getTabString()
    {
        return TAB;
    }

    public static String getTabString(int repeat)
    {
        return (repeat == 1) ? getTabString() : appendTabString(new StringBuilder(getTabSize(repeat)), repeat).toString();
    }

    public static StringBuilder appendTabString(StringBuilder builder)
    {
        return builder.append(getTabString());
    }

    /**
     * NOTE: This is a more efficient way than just chaining append() as it lessens the potential number of resize to the internal
     * array maintained by StringBuilder
     */
    public static StringBuilder appendTabString(StringBuilder builder, int repeat)
    {
        if (repeat == 1)
        {
            return appendTabString(builder);
        }
        builder.ensureCapacity(builder.length() + getTabSize(repeat));
        for (int i = 0; i < repeat; i++)
        {
            appendTabString(builder);
        }
        return builder;
    }

    /**
     * Convert string in the graph to string in the transformed grammar
     *
     * @param val    the string to be converted
     * @param escape whether to escape the string or not, usually we should escape the string as stuffs like `\r\n`
     *               if not escaped would become actual new line character in the string generated by the transformer
     *               but we also have to be mindful that there are strings which contain escaped quotes, such as JSON
     *               string. For these, if we escape them, we will accidentally add extra escape character to already
     *               escaped characters. Also remember that the transformer is symmetrical to the parser, if in the
     *               parser we do not unescape, we should not escape in the transformer as well.
     */
    public static String convertString(String val, boolean escape)
    {
        return convertString(val, escape, false);
    }

    public static String convertString(String val, boolean escape, boolean doubleQuotes)
    {
        StringBuilder builder = new StringBuilder();
        if (doubleQuotes)
        {
            builder.append("\"");

            if (escape)
            {
                // since Pure grammar is syntactically close to Java, we use `escapeJava`
                // so that `\r\n` in strings are properly escaped, but this method also escape quotes and because
                // Pure uses single quotes for string, we have to do some further processing
                val = StringEscapeUtils.escapeJava(val);
                // since Pure grammar uses single quotes to surround string, we need to handle quotes in a special way
                val = val.replace("'", "\\'");
            }
            builder.append(val);

            builder.append("\"");
        }
        else
        {
            builder.append("'");

            if (escape)
            {
                // since Pure grammar is syntactically close to Java, we use `escapeJava`
                // so that `\r\n` in strings are properly escaped, but this method also escape quotes and because
                // Pure uses single quotes for string, we have to do some further processing
                val = StringEscapeUtils.escapeJava(val);
                // since Pure grammar uses single quotes to surround string, we need to handle quotes in a special way
                val = val.replace("'", "\\'");
                val = val.replace("\\\"", "\"");
            }
            builder.append(val);

            builder.append("'");
        }

        return builder.toString();
    }

    public static String convertPath(String val)
    {
        return convertPath(val, false);
    }

    public static String convertPath(String val, boolean isPureGrammar)
    {
        return Arrays.stream(val.split(PACKAGE_SEPARATOR))
                .map(v -> convertIdentifier(v, false, isPureGrammar))
                .collect(Collectors.joining(PACKAGE_SEPARATOR));
    }

    public static String convertIdentifier(String val)
    {
        return convertIdentifier(val, false);
    }

    public static String convertIdentifier(String val, boolean doubleQuotes)
    {
        return convertIdentifier(val, doubleQuotes, false);
    }

    public static String convertIdentifier(String val, boolean doubleQuotes, boolean isPureGrammar)
    {
        if (val == null || val.isEmpty())
        {
            return "";
        }

        if (isPureGrammar)
        {
            return val;
        }
        else
        {
            return UNQUOTED_IDENTIFIER_PATTERN.matcher(val).matches() ? val : convertString(val, true, doubleQuotes);
        }
    }

    public static String unsupported(java.lang.Class<?> _class)
    {
        return unsupported(_class, null);
    }

    public static String unsupported(java.lang.Class<?> _class, String typeName)
    {
        Thread.dumpStack();
        return "/* Unsupported transformation for " + (typeName != null ? (typeName + " ") : "") + "'" + _class.getName() + "' */";
    }

    public static String renderObject(Object value)
    {
        if (value instanceof String)
        {
            return "'" + value + "'";
        }
        if (value instanceof List)
        {
            return ((List<?>) value).stream().map(PureGrammarComposerUtility::renderObject).collect(Collectors.joining(", ", "[", "]"));
        }
        if (value instanceof Map)
        {
            StringBuilder builder = new StringBuilder("{\n");
            ((Map<?, ?>) value).forEach((k, v) -> appendTabString(builder, 2).append(k).append(": ").append(renderObject(v)).append(";\n"));
            return appendTabString(builder).append('}').toString();
        }
        return String.valueOf(value);
    }

    public static String renderPossibleLocalMappingProperty(PropertyMapping propertyMapping)
    {
        return (propertyMapping.localMappingProperty != null ? "+" : "") + PureGrammarComposerUtility.convertIdentifier(propertyMapping.property.property) +
                (propertyMapping.localMappingProperty != null ? ": " + propertyMapping.localMappingProperty.type + "[" + HelperDomainGrammarComposer.renderMultiplicity(propertyMapping.localMappingProperty.multiplicity) + "]" : "");
    }

}
