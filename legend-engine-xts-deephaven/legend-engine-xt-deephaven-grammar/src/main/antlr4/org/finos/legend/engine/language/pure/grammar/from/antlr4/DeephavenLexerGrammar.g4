lexer grammar DeephavenLexerGrammar;

import CoreLexerGrammar;

// -------------------------------------- KEYWORD --------------------------------------

IMPORT:                                    'import';
DEEPHAVEN:                                 'Deephaven';
TABLE:                                     'Table';
COLUMNS:                                   'Columns';

// -------------------------------------- Column Types --------------------------------------

DATE_TIME:                                 'datetime';
STRING:                                    'string';
INT:                                       'int';
BOOLEAN:                                   'boolean';

// ----------------------------------- BUILDING BLOCK -----------------------------------

QUOTED_STRING:                              ('"' ( EscSeq | ~["\r\n] )*  '"');