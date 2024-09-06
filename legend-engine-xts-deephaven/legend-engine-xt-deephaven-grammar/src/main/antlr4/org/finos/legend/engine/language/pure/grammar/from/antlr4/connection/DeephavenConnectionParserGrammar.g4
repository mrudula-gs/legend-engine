parser grammar DeephavenConnectionParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = DeephavenConnectionLexerGrammar;
}

identifier:                             VALID_STRING | STRING | SERVER_URL | AUTHENTICATION
;

deephavenConnectionDefinition:         (
                                            connectionStore
                                            | serverUrl
                                            | authentication
                                        )*
                                        EOF
;


connectionStore:                        STORE COLON qualifiedName SEMI_COLON
;

serverUrl:                              SERVER_URL COLON islandDefinition SEMI_COLON
;

authentication:                         AUTHENTICATION COLON islandDefinition SEMI_COLON
;