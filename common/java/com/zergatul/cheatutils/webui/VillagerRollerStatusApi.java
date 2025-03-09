package com.zergatul.cheatutils.webui;

import com.zergatul.cheatutils.modules.automation.VillagerRoller;

public class VillagerRollerStatusApi extends ApiBase {

    @Override
    public String getRoute() {
        return "villager-roller-status";
    }

    @Override
    public String post(String body) {
        Request request = gson.fromJson(body, Request.class);
        if (request.start) {
            VillagerRoller.instance.start();
        }
        if (request.stop) {
            VillagerRoller.instance.stop();
        }
        return "{}";
    }

    public record Request(boolean start, boolean stop) {}
}