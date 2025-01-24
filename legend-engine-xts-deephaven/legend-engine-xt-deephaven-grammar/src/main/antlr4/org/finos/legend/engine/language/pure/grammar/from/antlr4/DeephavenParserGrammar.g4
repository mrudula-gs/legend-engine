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
                                            | TABLE
                                            | TABLES | COLUMNS | COLUMNDEFINITION | columnType
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
                                                PAREN_OPEN
                                                    (
                                                        tables
                                                    )*
                                                PAREN_CLOSE
;

tables:                                     tableDefinition (tableDefinition)*
;

tableDefinition:                            TABLE tableName
                                            PAREN_OPEN
                                                (
                                                    columns
                                                )*
                                            PAREN_CLOSE
;

columns:                                    columnDefinition (COMMA columnDefinition)*
;

columnDefinition:                           columnName COLON columnType
;

// TODO: anumam - fix this - for some reason it doesn't allow single quotes - causes Unexpected Token exception
// we should allow this for columnnames that have spaces or identifiers that are doubling as names
columnName:                           VALID_STRING | STRING
;

tableName:                            VALID_STRING | STRING
;

