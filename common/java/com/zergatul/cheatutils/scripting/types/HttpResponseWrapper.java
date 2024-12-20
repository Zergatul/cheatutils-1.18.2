package com.zergatul.cheatutils.scripting.types;

import com.zergatul.scripting.Getter;
import com.zergatul.scripting.MethodDescription;
import com.zergatul.scripting.type.CustomType;

import javax.net.ssl.SSLSession;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

@CustomType(name = "HttpResponse")
public class HttpResponseWrapper {

    public static final HttpResponseWrapper INVALID_REQUEST = new HttpResponseWrapper(new HttpResponse<>() {

        @Override
        public int statusCode() {
            return 0;
        }

        @Override
        public HttpRequest request() {
            return null;
        }

        @Override
        public Optional<HttpResponse<String>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(new HashMap<>(), (name, value) -> true);
        }

        @Override
        public String body() {
            return "<INVALID REQUEST>";
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return null;
        }

        @Override
        public HttpClient.Version version() {
            return null;
        }
    });

    private final HttpResponse<String> response;

    public HttpResponseWrapper(HttpResponse<String> response) {
        this.response = response;
    }

    @Getter(name = "statusCode")
    public int getStatusCode() {
        return response.statusCode();
    }

    public boolean hasHeader(String name) {
        return response.headers().map().containsKey(name);
    }

    @MethodDescription("""
            Returns header value or empty string if headers is not present
            """)
    public String header(String name) {
        return response.headers().firstValue(name).orElse("");
    }

    @Getter(name = "headers")
    public HttpHeader[] getHeaders() {
        List<HttpHeader> list = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : response.headers().map().entrySet()) {
            for (String value : entry.getValue()) {
                list.add(new HttpHeader(entry.getKey(), value));
            }
        }
        return list.toArray(HttpHeader[]::new);
    }

    @Getter(name = "body")
    public String getBody() {
        return response.body();
    }
}