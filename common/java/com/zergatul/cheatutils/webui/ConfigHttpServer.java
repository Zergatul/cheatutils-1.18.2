package com.zergatul.cheatutils.webui;

import com.sun.net.httpserver.HttpServer;
import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.scripting.monaco.Integration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public class ConfigHttpServer {

    public static ConfigHttpServer instance = new ConfigHttpServer();

    private final Logger logger = LogManager.getLogger(ConfigHttpServer.class);
    private int basePort;
    private HttpServer server;

    private ConfigHttpServer() {
        basePort = 5005;
    }

    public synchronized void onConfigUpdated() {
        int port = ConfigStore.instance.getConfig().coreConfig.port;
        if (basePort != port) {
            basePort = port;

            if (server != null) {
                server.stop(1);
                start();
            }
        }
    }

    public synchronized void start() {
        int port = 0;
        for (int i = 0; i < 100; i++) {
            if (isAvailable(basePort + i)) {
                port = basePort + i;
                break;
            }
        }

        if (port == 0) {
            logger.error("Cannot find free port for HTTP server. BasePort={}.", basePort);
            return;
        }

        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        }
        catch (IOException e) {
            logger.error("Cannot start HttpServer on port {}.", port);
            logger.error(e);
            return;
        }

        new Integration().attach(server, "/api/code/");
        server.createContext("/api/", new ApiHandler());
        server.createContext("/assets/", new AssetsHandler());
        server.createContext("/textures/", new TexturesHandler());
        server.createContext("/local/", new LocalFilesHandler());
        server.createContext("/", new StaticFilesHandler());

        server.setExecutor(null);
        server.start();

        logger.info("HTTP server started at port {}", port);
    }

    private static boolean isAvailable(int port) {
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(port);
            socket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    /* should not be thrown */
                }
            }
        }

        return false;
    }
}