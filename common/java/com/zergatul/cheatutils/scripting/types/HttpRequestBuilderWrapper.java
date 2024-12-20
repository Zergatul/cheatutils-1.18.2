package com.zergatul.cheatutils.scripting.types;

import com.zergatul.scripting.type.CustomType;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;

@CustomType(name = "HttpRequestBuilder")
public class HttpRequestBuilderWrapper {

    private final HttpRequest.Builder builder = HttpRequest.newBuilder();
    private boolean invalid = false;

    public HttpRequestBuilderWrapper() {

    }

    public HttpRequestBuilderWrapper get() {
        builder.GET();
        return this;
    }

    public HttpRequestBuilderWrapper post() {
        builder.POST(HttpRequest.BodyPublishers.noBody());
        return this;
    }

    public HttpRequestBuilderWrapper post(String body) {
        builder.POST(HttpRequest.BodyPublishers.ofString(body));
        return this;
    }

    public HttpRequestBuilderWrapper delete() {
        builder.DELETE();
        return this;
    }

    public HttpRequestBuilderWrapper put() {
        builder.PUT(HttpRequest.BodyPublishers.noBody());
        return this;
    }

    public HttpRequestBuilderWrapper put(String body) {
        builder.PUT(HttpRequest.BodyPublishers.ofString(body));
        return this;
    }

    public HttpRequestBuilderWrapper url(String url) {
        URI uri = null;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            invalid = true;
        }

        if (uri != null) {
            builder.uri(uri);
        }

        return this;
    }

    public HttpRequestBuilderWrapper header(String name, String value) {
        try {
            builder.header(name, value);
        } catch (IllegalArgumentException e) {
            invalid = true;
        }
        return this;
    }

    public HttpRequestWrapper build() {
        if (invalid) {
            return HttpRequestWrapper.INVALID;
        }

        HttpRequest request;
        try {
            request = builder.build();
        } catch (IllegalStateException e) {
            return HttpRequestWrapper.INVALID;
        }

        return new HttpRequestWrapper(request);
    }
}