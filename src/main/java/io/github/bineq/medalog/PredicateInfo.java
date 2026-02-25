package io.github.bineq.medalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Metadata about a declared predicate collected during the first compilation pass.
 */
public class PredicateInfo {

    /** Parameter record: name and type. */
    public record Parameter(String name, String type) {}

    private final String name;
    private final List<Parameter> parameters;
    private final boolean alreadyHasIdSlot;
    private final int declarationLine;

    public PredicateInfo(String name, List<Parameter> parameters, int declarationLine) {
        this.name = name;
        this.parameters = Collections.unmodifiableList(new ArrayList<>(parameters));
        this.declarationLine = declarationLine;
        this.alreadyHasIdSlot = !parameters.isEmpty()
                && "id".equals(parameters.get(0).name())
                && "symbol".equals(parameters.get(0).type());
    }

    public String getName() {
        return name;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    /** Returns true if the first parameter is already declared as {@code id symbol}. */
    public boolean isAlreadyHasIdSlot() {
        return alreadyHasIdSlot;
    }

    public int getDeclarationLine() {
        return declarationLine;
    }
}
