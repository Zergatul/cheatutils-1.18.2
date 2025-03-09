package com.zergatul.cheatutils.webui;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class WebHelper {

    public static void sendException(HttpExchange exchange, Throwable throwable) throws IOException {
        sendException(exchange, HttpResponseCodes.INTERNAL_SERVER_ERROR, throwable);
    }

    public static void sendException(HttpExchange exchange, int code, Throwable throwable) throws IOException {
        StringBuilder builder = new StringBuilder();

        Throwable current = throwable;
        while (true) {
            builder.append(current.getClass().getName()).append("\n");
            builder.append(current.getMessage()).append("\n");
            builder.append("**********").append("\n");

            for (StackTraceElement element : current.getStackTrace())
                builder.append("\tat ").append(element).append("\n");

            current = current.getCause();
            if (current != null) {
                builder.append("\n");
                builder.append("Caused by:\n");
            } else {
                break;
            }
        }

        byte[] bytes = builder.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(code, bytes.length);
        OutputStream stream = exchange.getResponseBody();
        stream.write(bytes);
        stream.close();
        exchange.close();
    }
}