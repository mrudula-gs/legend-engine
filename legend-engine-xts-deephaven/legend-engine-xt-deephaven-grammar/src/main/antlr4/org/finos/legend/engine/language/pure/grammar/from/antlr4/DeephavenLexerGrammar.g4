lexer grammar DeephavenLexerGrammar;

import CoreLexerGrammar;

// -------------------------------------- KEYWORD --------------------------------------

IMPORT:                                     'import';
DEEPHAVEN:                                  'Deephaven';
TABLE:                                      'Table';
COLUMNS:                                    'Columns';

// -------------------------------------- Column Types --------------------------------------

DATETIME:                                  'datetime';
STRING:                                    'string';
INT:                                       'int';
BOOLEAN:                                   'boolean';