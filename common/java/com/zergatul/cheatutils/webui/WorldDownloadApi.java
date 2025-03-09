package com.zergatul.cheatutils.webui;

import com.zergatul.cheatutils.controllers.WorldDownloadController;

public class WorldDownloadApi extends ApiBase {

    @Override
    public String getRoute() {
        return "world-download";
    }

    @Override
    public String get() {
        return gson.toJson(new Status());
    }

    @Override
    public String post(String body) throws ApiException {
        body = gson.fromJson(body, String.class);

        if (body.startsWith("start:")) {
            WorldDownloadController.instance.start(body.substring(6));
            return get();
        }

        if (body.equals("stop")) {
            WorldDownloadController.instance.stop();
            return get();
        }

        throw new ApiException("Invalid body", HttpResponseCodes.BAD_REQUEST);
    }

    public static class Status {

        public boolean active;

        public Status() {
            active = WorldDownloadController.instance.isActive();
        }
    }
}