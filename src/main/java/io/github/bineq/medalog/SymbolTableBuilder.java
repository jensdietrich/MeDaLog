package io.github.bineq.medalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * First-pass ANTLR4 listener that populates the {@link SymbolTable} by collecting
 * all predicate declarations ({@code .decl}) in the program.
 */
public class SymbolTableBuilder extends MeDaLogBaseListener {

    private static final Logger LOG = LoggerFactory.getLogger(SymbolTableBuilder.class);

    private final SymbolTable symbolTable;

    public SymbolTableBuilder(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    @Override
    public void enterPredDeclaration(MeDaLogParser.PredDeclarationContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        int line = ctx.getStart().getLine();

        List<PredicateInfo.Parameter> params = new ArrayList<>();
        if (ctx.paramList() != null) {
            for (MeDaLogParser.ParamContext p : ctx.paramList().param()) {
                String paramName = p.IDENTIFIER().getText();
                String paramType = p.souffleType().getText();
                params.add(new PredicateInfo.Parameter(paramName, paramType));
            }
        }

        PredicateInfo info = new PredicateInfo(name, params, line);
        if (info.isAlreadyHasIdSlot()) {
            LOG.warn("Line {}: predicate '{}' already declares 'id symbol' as first parameter; "
                    + "no id slot will be added.", line, name);
        }
        symbolTable.registerPredicate(info);
        LOG.info("Registered predicate '{}' with {} parameter(s) at line {}.",
                name, params.size(), line);
    }
}
