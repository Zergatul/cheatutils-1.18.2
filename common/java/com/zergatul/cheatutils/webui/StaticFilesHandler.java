package com.zergatul.cheatutils.webui;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zergatul.cheatutils.utils.ResourceHelper;
import com.zergatul.cheatutils.wrappers.ModEnvironment;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class StaticFilesHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String filename;
        if (exchange.getRequestURI().getPath().equals("/")) {
            filename = "/index.html";
        } else {
            filename = exchange.getRequestURI().getPath();
        }

        byte[] bytes;
        InputStream stream = ResourceHelper.get("web" + filename);
        try (stream) {
            if (stream == null) {
                exchange.sendResponseHeaders(HttpResponseCodes.NOT_FOUND, 0);
                exchange.close();
                return;
            }

            bytes = org.apache.commons.io.IOUtils.toByteArray(stream);
        }
        catch (Throwable e) {
            exchange.sendResponseHeaders(HttpResponseCodes.INTERNAL_SERVER_ERROR, 0);
            exchange.close();
            return;
        }

        HttpHelper.setContentType(exchange, filename);

        exchange.sendResponseHeaders(HttpResponseCodes.OK, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
        exchange.close();
    }
}