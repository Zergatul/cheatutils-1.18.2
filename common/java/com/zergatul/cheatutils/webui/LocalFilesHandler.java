package com.zergatul.cheatutils.webui;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class LocalFilesHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            Path path = Path.of(
                    Minecraft.getInstance().gameDirectory.getPath(),
                    "mods",
                    exchange.getRequestURI().getPath().substring("/local/".length()));
            File file = path.toFile();
            if (file.exists() && file.isFile()) {
                byte[] bytes;
                try (InputStream stream = new FileInputStream(file)) {
                    bytes = org.apache.commons.io.IOUtils.toByteArray(stream);
                }

                HttpHelper.setContentType(exchange, path.toString());

                exchange.sendResponseHeaders(200, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.close();
                exchange.close();
            } else {
                byte[] bytes = "File not found.".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(404, bytes.length);
                OutputStream stream = exchange.getResponseBody();
                stream.write(bytes);
                stream.close();
                exchange.close();
            }
        } catch (Throwable throwable) {
            StringBuilder builder = new StringBuilder();
            builder.append(throwable.getMessage()).append("\n");
            builder.append("**********").append("\n");

            for (StackTraceElement element : throwable.getStackTrace())
                builder.append("\tat ").append(element).append("\n");

            // inner exceptions?

            byte[] bytes = builder.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(500, bytes.length);
            OutputStream stream = exchange.getResponseBody();
            stream.write(bytes);
            stream.close();
            exchange.close();
        }
    }
}