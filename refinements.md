
## Refine Tests

reorganise the tests to improve readability as follows: input data and oracles  should go into files in src/test/resources . This should follow the convention              
<testname>-input.mdl for input and <testname>-oracle.dl for oracles.  To compare oracles with compiler output, create a helper method qssertEquivalent that compares the     
file content but ignores irrelevant text like whitespaces, new lines and comments.

