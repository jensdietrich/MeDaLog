
## Refine Tests

reorganise the tests to improve readability as follows: input data and oracles  should go into files in src/test/resources . This should follow the convention              
<testname>-input.mdl for input and <testname>-oracle.dl for oracles.  To compare oracles with compiler output, create a helper method qssertEquivalent that compares the     
file content but ignores irrelevant text like whitespaces, new lines and comments.

## Example

Create a comprehensive example in the example/ folder. This consists of the following:

1. a program daleq.dl, with rules from https://github.com/binaryeq/daleq/tree/main/src/main/resources/rules 
   2. Rules should be organised as follows: a top level module `daleq` containing modules reflecting the folder structure in https://github.com/binaryeq/daleq/tree/main/src/main/resources/rules
   3. The `daleq` folder has the following annotations: @author="jens" , @reviewed="behnaz", @date=<commit date of latest change>. 
   4. Use some input files from the EDBs found in local folder <prompt for input> . Put those facts in a module `facts`, Annotate this module with `@note:"EDB extracted by daleq"`
5. A shell script `run-souffle.sh` that runs souffle for this program, and prints out inferred facts for the following predicates: 
   6. IDB_VERSION 
   7. _annotation
   8. _module
9. `run-souffle.sh` can assume that `souffle` is in the path
10. A readme.md file describing the example
