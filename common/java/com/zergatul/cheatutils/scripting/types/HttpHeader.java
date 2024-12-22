package com.zergatul.cheatutils.scripting.types;

import com.zergatul.scripting.Getter;
import com.zergatul.scripting.type.CustomType;

@CustomType(name = "HttpHeader")
public class HttpHeader {

    private final String name;
    private final String value;

    public HttpHeader(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Getter(name = "name")
    public String getName() {
        return name;
    }

    @Getter(name = "value")
    public String getValue() {
        return value;
    }
}