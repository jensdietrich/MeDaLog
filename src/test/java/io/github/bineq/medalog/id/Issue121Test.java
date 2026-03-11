package io.github.bineq.medalog.id;

import io.github.bineq.medalog.ProcessorTestBase;
import org.junit.jupiter.api.Test;
import java.io.IOException;


public class Issue121Test extends ProcessorTestBase  {

    private final IdentityAnnotationProcessor proc = new IdentityAnnotationProcessor();

    @Test
        public void test() throws IOException {
        String input = loadResource("id/issue121-input.dl");
        String oracle = loadResource("id/issue121-oracle.dl");
        String output = proc.process(input);
        assertEquivalent(oracle, output);
    }
}
