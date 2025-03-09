package com.zergatul.cheatutils.webui;

import com.zergatul.cheatutils.controllers.ClientTickController;
import net.minecraft.client.Minecraft;

public class DimensionApi extends ApiBase {

    @Override
    public String getRoute() {
        return "dimension";
    }

    @Override
    public String get() {
        return gson.toJson(ClientTickController.instance.getResult(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) {
                return null;
            }
            return mc.level.dimension().location().toString();
        }, 1000));
    }
}