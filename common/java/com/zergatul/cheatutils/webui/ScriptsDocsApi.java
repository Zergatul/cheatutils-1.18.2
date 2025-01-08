package com.zergatul.cheatutils.webui;

import com.zergatul.cheatutils.scripting.*;
import com.zergatul.scripting.InterfaceHelper;
import com.zergatul.scripting.type.CustomType;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.HttpException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class ScriptsDocsApi extends ApiBase {

    @Override
    public String getRoute() {
        return "scripts-doc";
    }

    @Override
    public String get(String id) throws HttpException {
        ScriptType type = ScriptType.valueOf(id);
        List<String> refs = new ArrayList<>();
        refs.add(formatFuncMethod(InterfaceHelper.getFuncInterfaceMethod(type.createParameters().getFunctionalInterface())));
        refs.add("**********");
        refs.addAll(generateRootRefs(type.getApis()));
        return gson.toJson(refs);
    }

    private List<String> generateRootRefs(ApiType[] types) {
        List<String> refs = new ArrayList<>();

        getFields(Root.class, false).sorted(Comparator.comparing(Field::getName)).forEach(field -> {
            getMethods(field.getType(), types).forEach(method -> {
                refs.add(generateHtml(field.getName(), method));
            });
            getFields(field.getType(), true).forEach(f -> {
                String prefix = field.getName() + "." + f.getName();
                refs.addAll(generateChildRefs(prefix, f.getType(), types));
            });
        });

        return refs;
    }

    private List<String> generateChildRefs(String prefix, Class<?> clazz, ApiType[] types) {
        List<String> refs = new ArrayList<>();

        getMethods(clazz, types).forEach(method -> {
            refs.add(generateHtml(prefix, method));
        });

        Field[] fields = clazz.getDeclaredFields();
        Arrays.stream(fields).filter(f -> Modifier.isPublic(f.getModifiers())).sorted(Comparator.comparing(Field::getName)).forEach(field -> {
            getFields(field.getType(), true).forEach(f -> {
                String prefixInner = prefix + "." + field.getName() + "." + f.getName();
                refs.addAll(generateChildRefs(prefixInner, f.getType(), types));
            });
        });

        return refs;
    }

    private Stream<Method> getMethods(Class<?> clazz, ApiType[] types) {
        Method[] methods = clazz.getMethods();
        return Arrays.stream(methods).filter(m -> {
            if (m.getDeclaringClass() == Object.class) {
                return false; // skip Object methods
            }
            if (!VisibilityCheck.isOk(m, types)) {
                return false;
            }
            return Modifier.isPublic(m.getModifiers());
        }).sorted(Comparator.comparing(Method::getName));
    }

    private Stream<Field> getFields(Class<?> clazz, boolean isInstance) {
        return Arrays.stream(clazz.getFields())
                .filter(f -> Modifier.isPublic(f.getModifiers()))
                .filter(f -> isInstance ^ Modifier.isStatic(f.getModifiers()));
    }

    private String generateHtml(String prefix, Method method) {
        final String space = "&nbsp;";
        Parameter[] parameters = method.getParameters();
        String paramsStr = Arrays.stream(parameters).map(p -> formatClass(p.getType()) + space + p.getName()).reduce((s1, s2) -> s1 + "," + space + s2).orElse("");
        String returnStr = "";
        if (method.getReturnType() != void.class) {
            returnStr = space + "→" + space + formatClass(method.getReturnType());
        }

        return prefix + ".<span class=\"method\">" + method.getName() + "</span>(" + paramsStr + ")" + returnStr;
    }

    private String formatClass(Class<?> clazz) {
        if (clazz.isArray()) {
            return formatClass(clazz.getComponentType()) + "[]";
        }

        String name;
        boolean shouldWrap = true;
        if (clazz == void.class) {
            name = "void";
        } else if (clazz == String.class) {
            name = "string";
        } else if (clazz == double.class) {
            name = "float";
        } else if (clazz == CompletableFuture.class) {
            name = "Future";
        } else if (InterfaceHelper.isFuncInterface(clazz)) {
            Method method = InterfaceHelper.getFuncInterfaceMethod(clazz);
            name = "Lambda<" + formatFuncMethod(method) + ">";
            shouldWrap = false;
        } else if (clazz.getName().startsWith("com.zergatul.cheatutils.scripting.modules.")) {
            name = clazz.getName().substring("com.zergatul.cheatutils.scripting.modules.".length());
        } else if (clazz.isAnnotationPresent(CustomType.class)) {
            name = clazz.getAnnotation(CustomType.class).name();
        } else {
            name = clazz.getName();
        }

        return shouldWrap ? "<span class=\"class\">" + name + "</span>" : name;
    }

    private String formatFuncMethod(Method method) {
        final String space = "&nbsp;";
        String parameters = String.join("," + space, Arrays.stream(method.getParameters()).map(p -> {
            return formatClass(p.getType()) + space + p.getName();
        }).toList());
        return "(" + parameters + ")" + space + "=>" + space + formatClass(method.getReturnType());
    }

    private String formatComment(String text) {
        return "<span class=\"comment\">&nbsp;" + StringEscapeUtils.escapeHtml4(text) + "</span>";
    }
}