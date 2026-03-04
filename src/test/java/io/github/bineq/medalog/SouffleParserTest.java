package io.github.bineq.medalog;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Parameterised tests for {@link SouffleParser}.
 *
 * <p>Each parameter is a valid Souffle program sourced from the official Souffle
 * test suite at https://github.com/souffle-lang/souffle/tree/master/tests/.
 * The test verifies that the parser produces no syntax errors on these inputs.
 */
class SouffleParserTest extends ProcessorTestBase {

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "parser/aggregates.dl",
        "parser/aliases.dl",
        "parser/arithm.dl",
        "parser/binop.dl",
        "parser/cat.dl",
        "parser/comp_override.dl",
        "parser/components.dl",
        "parser/components1.dl",
        "parser/components2.dl",
        "parser/components3.dl",
        // components_generic.dl excluded: uses #define CPP macros requiring preprocessing

        "parser/contains.dl",
        "parser/count.dl",
        "parser/counter.dl",
        "parser/empty_relations.dl",
        "parser/eqrel.dl",
        "parser/existential.dl",
        "parser/facts.dl",
        "parser/facts2.dl",
        "parser/fib.dl",
        "parser/float_equality.dl",
        "parser/grammar.dl",
        "parser/hex.dl",
        "parser/independent_body.dl",
        "parser/inline_negation1.dl",
        "parser/inline_nqueens.dl",
        "parser/inline_records.dl",
        "parser/inline_underscore.dl",
        "parser/inline_unification.dl",
        "parser/list.dl",
        "parser/match.dl",
        "parser/match2.dl",
        "parser/multiple_heads.dl",
        "parser/mutrecursion.dl",
        "parser/neg1.dl",
        "parser/neg2.dl",
        "parser/neg3.dl",
        "parser/neg5.dl",
        "parser/number_constants.dl",
        "parser/provenance_path.dl",
        "parser/recursion.dl",
        "parser/string_ops.dl",
        "parser/subtype.dl",
    })
    void parsesWithoutErrors(String resource) throws IOException {
        String source = loadResource(resource);

        SouffleLexer lexer = new SouffleLexer(CharStreams.fromString(source));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SouffleParser parser = new SouffleParser(tokens);

        lexer.removeErrorListeners();
        parser.removeErrorListeners();

        List<String> errors = new ArrayList<>();
        BaseErrorListener errorCollector = new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                    int line, int charPositionInLine,
                                    String msg, RecognitionException e) {
                errors.add("line " + line + ":" + charPositionInLine + " " + msg);
            }
        };
        lexer.addErrorListener(errorCollector);
        parser.addErrorListener(errorCollector);

        parser.program();

        assertEquals(List.of(), errors, "Unexpected parse errors in " + resource);
    }
}
