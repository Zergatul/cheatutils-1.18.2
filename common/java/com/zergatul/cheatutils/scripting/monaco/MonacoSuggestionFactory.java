package com.zergatul.cheatutils.scripting.monaco;

import com.zergatul.scripting.InternalException;
import com.zergatul.scripting.completion.SuggestionFactory;
import com.zergatul.scripting.lexer.TokenType;
import com.zergatul.scripting.symbols.DeclaredStaticVariable;
import com.zergatul.scripting.symbols.Function;
import com.zergatul.scripting.symbols.LocalVariable;
import com.zergatul.scripting.symbols.StaticFieldConstantStaticVariable;
import com.zergatul.scripting.type.*;

import java.util.List;

public class MonacoSuggestionFactory implements SuggestionFactory<Suggestion> {

    private final DocumentationProvider provider;

    public MonacoSuggestionFactory(DocumentationProvider provider) {
        this.provider = provider;
    }

    @Override
    public Suggestion getKeywordSuggestion(TokenType type) {
        String text = type.toString().toLowerCase();
        return new Suggestion(
                text,
                null,
                null,
                text,
                CompletionItemKind.KEYWORD);
    }

    @Override
    public List<Suggestion> getTypeSuggestion(SType type) {
        if (type == SInt.instance) {
            return List.of(
                    new Suggestion(
                            "int",
                            null,
                            provider.getTypeDocs(type),
                            "int",
                            CompletionItemKind.CLASS),
                    new Suggestion(
                            "int32",
                            null,
                            provider.getTypeDocs(type),
                            "int32",
                            CompletionItemKind.CLASS));
        }
        if (type == SInt64.instance) {
            return List.of(
                    new Suggestion(
                            "long",
                            null,
                            provider.getTypeDocs(type),
                            "long",
                            CompletionItemKind.CLASS),
                    new Suggestion(
                            "int64",
                            null,
                            provider.getTypeDocs(type),
                            "int64",
                            CompletionItemKind.CLASS));
        }
        if (type instanceof SPredefinedType) {
            return List.of(new Suggestion(
                    type.toString(),
                    null,
                    provider.getTypeDocs(type),
                    type.toString(),
                    CompletionItemKind.CLASS));
        }
        return List.of();
    }

    @Override
    public Suggestion getCustomTypeSuggestion(Class<?> clazz) {
        CustomType type = clazz.getAnnotation(CustomType.class);
        return new Suggestion(
                type.name(),
                null,
                null,
                type.name(),
                CompletionItemKind.CLASS);
    }

    @Override
    public Suggestion getPropertySuggestion(PropertyReference property) {
        return new Suggestion(
                property.getName(),
                type(property.getType()),
                null,
                property.getName(),
                CompletionItemKind.PROPERTY);
    }

    @Override
    public Suggestion getMethodSuggestion(MethodReference method) {
        if (method instanceof UnknownMethodReference) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(type(method.getReturn()));
        sb.append(' ');
        sb.append(type(method.getOwner()));
        sb.append('.');
        sb.append(method.getName());
        sb.append('(');
        List<MethodParameter> parameters = method.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            sb.append(type(parameters.get(i).type()));
            sb.append(' ');
            sb.append(parameters.get(i).name());
            if (i < parameters.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(')');

        return new Suggestion(
                method.getName(),
                sb.toString(),
                provider.getMethodDocumentation(method).orElse(null),
                method.getName(),
                CompletionItemKind.METHOD);
    }

    @Override
    public Suggestion getStaticConstantSuggestion(StaticFieldConstantStaticVariable variable) {
        return new Suggestion(
                variable.getName(),
                type(variable.getType()),
                null,
                variable.getName(),
                CompletionItemKind.VALUE);
    }

    @Override
    public Suggestion getStaticFieldSuggestion(DeclaredStaticVariable variable) {
        return new Suggestion(
                variable.getName(),
                type(variable.getType()),
                null,
                variable.getName(),
                CompletionItemKind.VARIABLE);
    }

    @Override
    public Suggestion getFunctionSuggestion(Function function) {
        return new Suggestion(
                function.getName(),
                function.getFunctionType().toString(),
                null,
                function.getName(),
                CompletionItemKind.FUNCTION);
    }

    @Override
    public Suggestion getLocalVariableSuggestion(LocalVariable variable) {
        return new Suggestion(
                variable.getName(),
                type(variable.getType()),
                null,
                variable.getName(),
                CompletionItemKind.VARIABLE);
    }

    @Override
    public Suggestion getInputParameterSuggestion(String name, SType type) {
        return new Suggestion(
                name,
                type(type),
                null,
                name,
                CompletionItemKind.VARIABLE);
    }

    private String type(SType type) {
        if (type instanceof SClassType classType) {
            Class<?> clazz = classType.getJavaClass();
            if (clazz.getName().startsWith("com.zergatul.cheatutils.scripting")) {
                return clazz.getSimpleName();
            } else {
                return clazz.getName();
            }
        } else {
            return type.toString();
        }
    }
}