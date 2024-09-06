parser grammar DeephavenParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = DeephavenLexerGrammar;
}

// -------------------------------------- IDENTIFIER --------------------------------------

unquotedIdentifier:                         VALID_STRING
                                            | DEEPHAVEN
                                            | IMPORT
                                            | TABLE | COLUMNS
;

identifier:                                 unquotedIdentifier | STRING
;

columnTypes:                                DATETIME |
                                            STRING |
                                            INT |
                                            BOOLEAN
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

tableDefinition:                            COLUMNS
                                                BRACE_OPEN
                                                    (
                                                        columnName COLON columnType COMMA
                                                    )*
                                                BRACE_CLOSE
;

columnName:                                 VALID_STRING | STRING
;

columnType:                                 DATETIME | STRING | INT | BOOLEAN
;