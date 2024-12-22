package com.zergatul.cheatutils.scripting.types;

import com.zergatul.cheatutils.scripting.HiddenMethod;
import com.zergatul.scripting.type.CustomType;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;

@CustomType(name = "HttpRequest")
public class HttpRequestWrapper {

    public static final HttpRequestWrapper INVALID;

    private final HttpRequest request;

    static {
        try {
            INVALID = new HttpRequestWrapper(HttpRequest.newBuilder(new URI("http://not-used")).build());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException();
        }
    }

    public HttpRequestWrapper(HttpRequest request) {
        this.request = request;
    }

    public static HttpRequestBuilderWrapper createBuilder() {
        return new HttpRequestBuilderWrapper();
    }

    @HiddenMethod
    public HttpRequest getRequest() {
        return request;
    }
}