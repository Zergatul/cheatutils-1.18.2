package com.zergatul.cheatutils.webui;

import net.minecraft.client.Minecraft;

public class UserApi extends ApiBase {

    @Override
    public String getRoute() {
        return "user";
    }

    @Override
    public String get() {
        return Minecraft.getInstance().getUser().getName();
    }
}
