package com.zergatul.cheatutils.scripting.monaco;

import com.zergatul.scripting.InterfaceHelper;
import com.zergatul.scripting.TextRange;
import com.zergatul.scripting.binding.BinderOutput;
import com.zergatul.scripting.binding.nodes.*;
import com.zergatul.scripting.compiler.CompilationParameters;
import com.zergatul.scripting.compiler.CompilerContext;
import com.zergatul.scripting.compiler.VisibilityChecker;
import com.zergatul.scripting.parser.NodeType;
import com.zergatul.scripting.symbols.*;
import com.zergatul.scripting.type.*;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CompletionProvider {

    private final DocumentationProvider documentationProvider;

    public CompletionProvider(DocumentationProvider documentationProvider) {
        this.documentationProvider = documentationProvider;
    }

    public List<Suggestion> get(CompilationParameters parameters, BinderOutput output, int line, int column) {
        BoundCompilationUnitNode unit = output.unit();
        CompletionContext completionContext = getCompletionContext(unit, line, column);
        return get(parameters, output, completionContext, line, column);
    }

    private List<Suggestion> get(CompilationParameters parameters, BinderOutput output, CompletionContext completionContext, int line, int column) {
        BoundCompilationUnitNode unit = output.unit();
        List<Suggestion> suggestions = new ArrayList<>();

        boolean canStatic = false;
        boolean canVoid = false;
        boolean canType = false;
        boolean canStatement = false;
        boolean canExpression = false;
        if (completionContext.entry == null) {
            switch (completionContext.type) {
                case NO_CODE -> {
                    canStatic = true;
                    canVoid = canType = true;
                    canStatement = true;
                }
                case BEFORE_FIRST -> {
                    canStatic = true;
                    canVoid = canType = unit.variables.variables.isEmpty();
                    canStatement = unit.variables.variables.isEmpty() && unit.functions.functions.isEmpty();
                }
                case AFTER_LAST -> {
                    canStatic = unit.functions.functions.isEmpty() && unit.statements.statements.isEmpty();
                    canVoid = canType = unit.statements.statements.isEmpty();
                    canStatement = true;
                }
            }
        } else {
            switch (completionContext.entry.node.getNodeType()) {
                case COMPILATION_UNIT -> {
                    if (completionContext.prev == null) {
                        canStatic = true;
                        canVoid = canType = true;
                        if (completionContext.next == null || completionContext.next.getNodeType() == NodeType.STATEMENTS_LIST) {
                            canStatement = true;
                        }
                    } else if (completionContext.prev.getNodeType() == NodeType.STATIC_VARIABLES_LIST) {
                        canStatic = true;
                        canVoid = canType = true;
                        if (completionContext.next == null || completionContext.next.getNodeType() == NodeType.STATEMENTS_LIST) {
                            canStatement = true;
                        }
                    } else if (completionContext.prev.getNodeType() == NodeType.FUNCTIONS_LIST) {
                        canVoid = canType = true;
                        canStatement = true;
                    }
                }
                case STATEMENTS_LIST, BLOCK_STATEMENT -> {
                    // check if we are at the end of unfinished statement
                    if (completionContext.prev != null) {
                        BoundNode unfinished = getUnfinished(completionContext.prev, line, column);
                        if (unfinished != null) {
                            CompletionContext ctx = new CompletionContext(new SearchEntry(completionContext.entry, unfinished), line, column);
                            suggestions.addAll(get(parameters, output, ctx, line, column));
                            break;
                        }
                    }
                    canStatement = true;
                }
                case IF_STATEMENT -> {
                    if (completionContext.prev != null) {
                        BoundNode unfinished = getUnfinished(completionContext.prev, line, column);
                        if (unfinished != null) {
                            CompletionContext ctx = new CompletionContext(new SearchEntry(completionContext.entry, unfinished), line, column);
                            suggestions.addAll(get(parameters, output, ctx, line, column));
                        }
                    } else {
                        BoundIfStatementNode statement = (BoundIfStatementNode) completionContext.entry.node;
                        // if (<here>)
                        if (TextRange.isBetween(line, column, statement.lParen.getRange(), statement.rParen.getRange())) {
                            CompletionContext ctx = new CompletionContext(new SearchEntry(completionContext.entry.parent, completionContext.entry.parent.node), line, column);
                            suggestions.addAll(get(parameters, output, ctx, line, column));
                        }
                    }
                }
                case RETURN_STATEMENT -> {
                    if (completionContext.prev != null) {
                        BoundNode unfinished = getUnfinished(completionContext.prev, line, column);
                        if (unfinished != null) {
                            CompletionContext ctx = new CompletionContext(new SearchEntry(completionContext.entry, unfinished), line, column);
                            suggestions.addAll(get(parameters, output, ctx, line, column));
                        }
                    }
                }
                case ARGUMENTS_LIST -> {
                    // check if we are at the end of unfinished statement
                    if (completionContext.prev != null) {
                        BoundNode unfinished = getUnfinished(completionContext.prev, line, column);
                        if (unfinished != null) {
                            CompletionContext ctx = new CompletionContext(new SearchEntry(completionContext.entry, unfinished), line, column);
                            suggestions.addAll(get(parameters, output, ctx, line, column));
                            break;
                        }
                    }
                    canExpression = true;
                }
                case PROPERTY_ACCESS_EXPRESSION -> {
                    BoundPropertyAccessExpressionNode node = (BoundPropertyAccessExpressionNode) completionContext.entry.node;
                    SType type = node.callee.type;
                    String partial = ""; // return all properties/methods and vscode handles the rest
                    /*if (node.property.getRange().isAfter(line, column)) {
                        partial = "";
                    } else {
                        partial = "";
                    }*/

                    type.getInstanceProperties().stream()
                            .filter(p -> p.getName().toLowerCase().startsWith(partial.toLowerCase()))
                            .forEach(p -> suggestions.add(documentationProvider.getPropertySuggestion(p)));
                    type.getInstanceMethods().stream()
                            .filter(m -> m.getName().toLowerCase().startsWith(partial.toLowerCase()))
                            .filter(m -> {
                                if (m instanceof NativeMethodReference nativeRef) {
                                    VisibilityChecker checker = parameters.getChecker();
                                    if (checker != null) {
                                        return checker.isVisible(nativeRef.getUnderlying());
                                    } else {
                                        return true;
                                    }
                                } else {
                                    return true;
                                }
                            })
                            .forEach(m -> suggestions.add(documentationProvider.getMethodSuggestion(m)));
                }
                case METHOD_INVOCATION_EXPRESSION -> {
                    BoundMethodInvocationExpressionNode node = (BoundMethodInvocationExpressionNode) completionContext.entry.node;
                    SType type = node.objectReference.type;
                    String partial = "";
                    type.getInstanceProperties().stream()
                            .filter(p -> p.getName().toLowerCase().startsWith(partial.toLowerCase()))
                            .forEach(p -> suggestions.add(documentationProvider.getPropertySuggestion(p)));
                    type.getInstanceMethods().stream()
                            .filter(m -> m.getName().toLowerCase().startsWith(partial.toLowerCase()))
                            .forEach(m -> suggestions.add(documentationProvider.getMethodSuggestion(m)));
                }
                case VARIABLE_DECLARATION -> {
                    BoundVariableDeclarationNode node = (BoundVariableDeclarationNode) completionContext.entry.node;
                    if (node.expression == null) {
                        if (node.type.getRange().containsOrEnds(line, column)) {
                            canStatement = true;
                        }
                    } else {
                        // should be after "=" token, but let be this for now
                        if (node.name.getRange().isBefore(line, column)) {
                            canExpression = true;
                        }
                    }
                }
                case FOREACH_LOOP_STATEMENT, ASSIGNMENT_STATEMENT, AUGMENTED_ASSIGNMENT_STATEMENT -> {
                    BoundNode unfinished = getUnfinished(completionContext.entry.node, line, column);
                    if (unfinished != null) {
                        CompletionContext ctx = new CompletionContext(new SearchEntry(completionContext.entry, unfinished), line, column);
                        suggestions.addAll(get(parameters, output, ctx, line, column));
                    }
                }
                case BINARY_EXPRESSION -> {
                    BoundNode unfinished = getUnfinished(completionContext.entry.node, line, column);
                    if (unfinished != null) {
                        CompletionContext ctx = new CompletionContext(new SearchEntry(completionContext.entry, unfinished), line, column);
                        suggestions.addAll(get(parameters, output, ctx, line, column));
                    }
                }
                case INVALID_EXPRESSION -> {
                    BoundInvalidExpressionNode invalid = (BoundInvalidExpressionNode) completionContext.entry.node;
                    if (invalid.getRange().getLength() == 0) {
                        canExpression = true;
                    }
                }
                case STRING_LITERAL, INTEGER_LITERAL, FLOAT_LITERAL -> {
                    // do nothing
                }
                default -> {
                    canExpression = true; // good fallback?
                    //throw new InternalException();
                }
            }
        }

        canExpression |= canStatement;
        canType |= canStatement;

        if (canStatic) {
            suggestions.add(documentationProvider.getStaticKeywordSuggestion());
        }
        if (canVoid) {
            suggestions.add(documentationProvider.getVoidKeywordSuggestion());
        }
        if (canType | canExpression) {
            for (SType type : new SType[] { SBoolean.instance, SInt.instance, SInt64.instance, SChar.instance, SFloat.instance, SString.instance }) {
                suggestions.addAll(documentationProvider.getTypeSuggestion(type));
            }
            for (Class<?> clazz : parameters.getCustomTypes()) {
                suggestions.add(documentationProvider.getCustomTypeSuggestion(clazz));
            }
        }
        if (canStatement) {
            suggestions.add(documentationProvider.getLetKeywordSuggestion());
        }
        if (canExpression) {
            suggestions.addAll(getSymbols(parameters, output, completionContext));
            if (parameters.isAsync()) {
                suggestions.add(documentationProvider.getAwaitKeywordSuggestion());
            }
        }
        if (canStatement) {
            // TODO: break/continue
            suggestions.addAll(documentationProvider.getCommonStatementStartSuggestions());
        }

        suggestions.removeIf(Objects::isNull);
        return suggestions;
    }

    private BoundNode getUnfinished(BoundNode node, int line, int column) {
        if (node instanceof BoundForEachLoopStatementNode loop) {
            if (loop.iterable.getRange().endsWith(line, column)) {
                return getUnfinished(loop.iterable, line, column);
            } else {
                return null;
            }
        }
        if (node instanceof BoundAssignmentStatementNode assignment) {
            if (assignment.right.getRange().endsWith(line, column)) {
                return getUnfinished(assignment.right, line, column);
            } else {
                return null;
            }
        }
        if (node instanceof BoundAugmentedAssignmentStatementNode assignment) {
            if (assignment.right.getRange().endsWith(line, column)) {
                return getUnfinished(assignment.right, line, column);
            } else {
                return null;
            }
        }

        if (node instanceof BoundStatementNode) {
            // check if cursor at the end of statement
            if (!node.getRange().endsWith(line, column)) {
                return null;
            }

            if (node instanceof BoundExpressionStatementNode expressionStatement) {
                return getUnfinished(expressionStatement.expression, line, column);
            }

            if (node instanceof BoundIfStatementNode ifStatement) {
                if (ifStatement.elseStatement != null) {
                    return getUnfinished(ifStatement.elseStatement, line, column);
                } else {
                    return getUnfinished(ifStatement.thenStatement, line, column);
                }
            }

            if (node instanceof BoundVariableDeclarationNode declaration) {
                if (declaration.expression != null && declaration.expression.getRange().endsWith(line, column)) {
                    return getUnfinished(declaration.expression, line, column);
                }
            }

            if (node instanceof BoundAssignmentStatementNode assignment) {
                if (assignment.right.getRange().containsOrEnds(line, column)) {
                    return getUnfinished(assignment.right, line, column);
                }
            }

            if (node instanceof BoundReturnStatementNode returnStatement) {
                if (returnStatement.expression != null && returnStatement.expression.getRange().endsWith(line, column)) {
                    return getUnfinished(returnStatement.expression, line, column);
                }
            }
        }

        if (node instanceof BoundExpressionNode) {
            if (node instanceof BoundPropertyAccessExpressionNode propertyAccess) {
                if (propertyAccess.property.getRange().endsWith(line, column)) {
                    return propertyAccess;
                }
            }
            if (node instanceof BoundImplicitCastExpressionNode implicitCast) {
                return getUnfinished(implicitCast.operand, line, column);
            }
            if (node instanceof BoundAwaitExpressionNode awaitExpression) {
                return getUnfinished(awaitExpression.expression, line, column);
            }
            if (node instanceof BoundBinaryExpressionNode binary) {
                if (binary.left.getRange().containsOrEnds(line, column)) {
                    return getUnfinished(binary.left, line, column);
                }
                if (binary.right.getRange().containsOrEnds(line, column)) {
                    return getUnfinished(binary.right, line, column);
                }
                if (binary.right.getRange().isBefore(line, column)) { // there can be space
                    return getUnfinished(binary.right, line, column);
                }
            }
            if (node instanceof BoundMethodInvocationExpressionNode invocation) {
                if (invocation.arguments.getRange().containsOrEnds(line, column)) {
                    for (BoundExpressionNode argument : invocation.arguments.arguments) {
                        return getUnfinished(argument, line, column);
                    }
                }
            }
            if (node instanceof BoundNameExpressionNode name) {
                return name;
            }
            if (node instanceof BoundInvalidExpressionNode) {
                return node;
            }
        }

        return null;
    }

    private CompletionContext getCompletionContext(BoundCompilationUnitNode unit, int line, int column) {
        SearchEntry entry = find(null, unit, line, column);
        if (entry == null) {
            if (unit.getRange().isAfter(line, column)) {
                return new CompletionContext(ContextType.BEFORE_FIRST, line, column);
            }
            if (unit.getRange().isBefore(line, column)) {
                if (unit.getRange().endsWith(line, column)) {
                    return getAtLastContext(unit, line, column);
                } else {
                    return new CompletionContext(ContextType.AFTER_LAST, line, column);
                }
            }
            return new CompletionContext(ContextType.NO_CODE, line, column);
        } else {
            return new CompletionContext(entry, line, column);
        }
    }

    private CompletionContext getAtLastContext(BoundCompilationUnitNode unit, int line, int column) {
        if (unit.statements.statements.isEmpty()) {
            return new CompletionContext(ContextType.AFTER_LAST, line, column);
        }

        SearchEntry root = new SearchEntry(null, unit);
        SearchEntry child = new SearchEntry(root, unit.statements);

        return new CompletionContext(child, line, column);
    }

    private List<Suggestion> getSymbols(CompilationParameters parameters, BinderOutput output, CompletionContext context) {
        List<Suggestion> list = new ArrayList<>();

        if (context.entry == null) {
            addStaticConstants(list, output.context());
            if (context.type == ContextType.AFTER_LAST) {
                addStaticVariables(list, output.unit().variables);
                addFunctions(list, output.unit().functions);
                addLocalVariables(list, output.unit().statements.statements);
            }
            return list;
        }

        while (context != null) {
            if (context.entry == null) {
                break;
            }
            switch (context.entry.node.getNodeType()) {
                case COMPILATION_UNIT -> {
                    addStaticConstants(list, output.context());
                    if (context.prev != null) {
                        if (context.prev.getNodeType() == NodeType.STATIC_VARIABLES_LIST) {
                            addStaticVariables(list, output.unit().variables);
                            addFunctions(list, output.unit().functions);
                        } else if (context.prev.getNodeType() == NodeType.FUNCTIONS_LIST) {
                            addStaticVariables(list, output.unit().variables);
                            addFunctions(list, output.unit().functions);

                            for (Parameter parameter : InterfaceHelper.getFuncInterfaceMethod(parameters.getFunctionalInterface()).getParameters()) {
                                list.add(documentationProvider.getLocalVariableSuggestion(parameter.getName(), SType.fromJavaType(parameter.getType())));
                            }
                        }
                    }
                }
                case FUNCTION -> {
                    BoundFunctionNode function = (BoundFunctionNode) context.entry.node;
                    for (var x : function.parameters.parameters) {
                        list.add(documentationProvider.getLocalVariableSuggestion((LocalVariable) x.getName().symbol));
                    }
                }
                default -> {
                    addLocalVariables(list, getStatementsPriorTo(context.entry.node, context.prev));
                }
            }

            context = context.up();

            if (context != null && context.entry != null) {
                // ForLoop has VariableDeclaration node inside
                // we need to handle ForEachLoop separately
                if (Objects.requireNonNull(context.entry.node.getNodeType()) == NodeType.FOREACH_LOOP_STATEMENT) {
                    BoundForEachLoopStatementNode node = (BoundForEachLoopStatementNode) context.entry.node;
                    list.add(documentationProvider.getLocalVariableSuggestion((LocalVariable) node.name.symbol));
                }
            }
        }

        return list;
    }

    private List<BoundStatementNode> getStatementsPriorTo(BoundNode parent, BoundNode prev) {
        if (prev == null) {
            return List.of();
        }

        List<BoundStatementNode> nodes = new ArrayList<>();
        List<BoundNode> children = parent.getChildren();
        for (BoundNode node : children) {
            if (node instanceof BoundStatementNode statement) {
                nodes.add(statement);
            }
            if (node == prev) {
                break;
            }
        }

        return nodes;
    }

    private void addStaticConstants(List<Suggestion> suggestions, CompilerContext context) {
        for (Symbol symbol : context.getStaticSymbols()) {
            if (symbol instanceof StaticFieldConstantStaticVariable constant) {
                suggestions.add(documentationProvider.getStaticConstantSuggestion(constant));
            }
        }
    }

    private void addStaticVariables(List<Suggestion> suggestions, BoundStaticVariablesListNode node) {
        for (BoundVariableDeclarationNode declaration : node.variables) {
            suggestions.add(documentationProvider.getStaticVariableSuggestion((StaticVariable) declaration.name.symbol));
        }
    }

    private void addFunctions(List<Suggestion> suggestions, BoundFunctionsListNode node) {
        for (BoundFunctionNode function : node.functions) {
            suggestions.add(documentationProvider.getFunctionSuggestion((Function) function.name.symbol));
        }
    }

    private void addLocalVariables(List<Suggestion> suggestions, List<BoundStatementNode> statements) {
        for (BoundStatementNode statement : statements) {
            if (statement instanceof BoundVariableDeclarationNode declaration) {
                if (declaration.name.symbol instanceof LocalVariable local) {
                    if (local.getName() != null) {
                        suggestions.add(documentationProvider.getLocalVariableSuggestion((LocalVariable) declaration.name.symbol));
                    }
                }
                // can be lifted variable?
            }
        }
    }

    private SearchEntry find(SearchEntry parent, BoundNode node, int line, int column) {
        if (node.getRange().contains(line, column)) {
            SearchEntry entry = new SearchEntry(parent, node);
            for (BoundNode child : node.getChildren()) {
                if (child.getRange().contains(line, column)) {
                    return find(entry, child, line, column);
                }
            }
            return entry;
        } else {
            return null;
        }
    }

    private record SearchEntry(SearchEntry parent, BoundNode node) {}

    private static class CompletionContext {

        public final ContextType type;
        public final SearchEntry entry;
        public final BoundNode prev;
        public final BoundNode next;
        public final int line;
        public final int column;

        public CompletionContext(ContextType type, int line, int column) {
            this.type = type;
            this.entry = null;
            this.prev = null;
            this.next = null;
            this.line = line;
            this.column = column;
        }

        public CompletionContext(SearchEntry entry, int line, int column) {
            this.type = ContextType.WITHIN;
            this.entry = entry;

            BoundNode prev = null;
            BoundNode next = null;
            List<BoundNode> children = entry.node.getChildren();
            for (int i = -1; i < children.size(); i++) {
                if (i < 0 || children.get(i).getRange().isBefore(line, column)) {
                    if (i + 1 >= children.size() || children.get(i + 1).getRange().isAfter(line, column)) {
                        prev = i >= 0 ? children.get(i) : null;
                        next = i < children.size() - 1 ? children.get(i + 1) : null;
                        break;
                    }
                    if (i + 1 >= children.size() || children.get(i + 1).getRange().contains(line, column)) {
                        prev = i >= 0 ? children.get(i) : null;
                        next = i < children.size() - 2 ? children.get(i + 2) : null;
                        break;
                    }
                }
            }

            this.prev = prev;
            this.next = next;
            this.line = line;
            this.column = column;
        }

        public CompletionContext up() {
            if (this.type != ContextType.WITHIN) {
                return null;
            }
            if (this.entry == null || this.entry.parent == null) {
                return null;
            }
            return new CompletionContext(this.entry.parent, line, column);
        }
    }

    private enum ContextType {
        NO_CODE,
        BEFORE_FIRST,
        AFTER_LAST,
        WITHIN
    }
}