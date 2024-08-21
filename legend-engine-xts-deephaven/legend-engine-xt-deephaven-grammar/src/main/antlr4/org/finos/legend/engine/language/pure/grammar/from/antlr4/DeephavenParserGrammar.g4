parser grammar DeephavenParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = DeephavenLexerGrammar;
}

// -------------------------------------- IDENTIFIER --------------------------------------

unquotedIdentifier:                         VALID_STRING
                                            | DEEPHAVEN
                                            | TABLE | COLUMNS
;

identifier:                                 unquotedIdentifier | STRING
;

columnTypes:                                DATETIME |
                                            STRING |
                                            INT |
                                            BOOLEAN
;

definition:                                 imports
                                                (deephavenDefinition)*
                                            EOF
;

deephavenDefinition:                        Deephaven qualifiedName
                                                BRACE_OPEN
                                                    (
                                                        tableDefinition (COMMA tableDefinition)*
                                                    )*
                                                BRACE_CLOSE
;

tableDefinition                             columns
                                                BRACE_OPEN
                                                    (
                                                        columnName : columnType COMMA
                                                    )*
                                                BRACE_CLOSE
;

columnName                                  VALID_STRING | STRING
;

columnType                                  DATETIME | STRING | INT | BOOLEAN