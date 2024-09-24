parser grammar DeephavenParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = DeephavenLexerGrammar;
}

columnType:                                 DATE_TIME | STRING | INT | BOOLEAN
;

unquotedIdentifier:                         VALID_STRING
                                            | DEEPHAVEN
                                            | IMPORT
                                            | TABLE | COLUMNS | columnType
;

identifier:                                 unquotedIdentifier | STRING
;

imports:                                    (importStatement)*
;

importStatement:                            IMPORT packagePath PATH_SEPARATOR STAR SEMI_COLON
;

definition:                                 imports
                                                (deephavenDefinition)*
                                            EOF
;

deephavenDefinition:                        DEEPHAVEN qualifiedName
                                                BRACE_OPEN
                                                    (
                                                        tableDefinition (COMMA tableDefinition)*
                                                    )*
                                                BRACE_CLOSE
;

tableDefinition:                            TABLE tableName
                                            BRACE_OPEN
                                                (
                                                    COLUMNS
                                                    BRACE_OPEN
                                                        (
                                                            columnName COLON columnType COMMA
                                                        )*
                                                    BRACE_CLOSE
                                                )*
                                            BRACE_CLOSE
;

columnName:                           VALID_STRING | STRING | QUOTED_STRING
;

tableName:                            VALID_STRING | STRING | QUOTED_STRING
;

