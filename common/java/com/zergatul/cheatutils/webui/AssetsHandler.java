package com.zergatul.cheatutils.webui;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.client.Minecraft;

import java.io.*;

public class AssetsHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String filename = exchange.getRequestURI().getPath();
        try (InputStream stream = Minecraft.class.getResourceAsStream(filename)) {
            if (stream == null) {
                exchange.sendResponseHeaders(HttpResponseCodes.NOT_FOUND, 0);
                return;
            }

            HttpHelper.setContentType(exchange, filename);
            HttpHelper.setCacheControl(exchange);

            byte[] bytes = org.apache.commons.io.IOUtils.toByteArray(stream);

            exchange.sendResponseHeaders(HttpResponseCodes.OK, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
        catch (Throwable e) {
            exchange.sendResponseHeaders(HttpResponseCodes.INTERNAL_SERVER_ERROR, 0);
        }
        finally {
            exchange.close();
        }
    }
}