package com.zergatul.cheatutils.modules.visuals;

import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.controllers.ClientTickController;
import com.zergatul.cheatutils.controllers.NetworkPacketsController;
import com.zergatul.cheatutils.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;

public class FakeWeather implements Module {

    public static FakeWeather instance = new FakeWeather();

    private static final Minecraft mc = Minecraft.getInstance();

    private FakeWeather() {
        NetworkPacketsController.instance.addServerPacketHandler(this::onServerPacket);
    }

    public void setTime(int value) {
        ClientTickController.instance.run(() -> {
            if (mc.level == null) {
                return;
            }
            mc.level.getLevelData().setDayTime(value);
        });
    }

    public void setRain(float value) {
        ClientTickController.instance.run(() -> {
            if (mc.level == null) {
                return;
            }
            if (value <= 0) {
                mc.level.getLevelData().setRaining(false);
                mc.level.setRainLevel(0);
            } else {
                mc.level.getLevelData().setRaining(true);
                mc.level.setRainLevel(Math.min(value, 1));
            }
        });
    }

    private void onServerPacket(NetworkPacketsController.ServerPacketArgs args) {
        if (ConfigStore.instance.getConfig().fakeWeatherConfig.enabled) {
            if (args.packet instanceof ClientboundSetTimePacket) {
                args.skip = true;
            }
            if (args.packet instanceof ClientboundGameEventPacket packet) {
                if (packet.getEvent() == ClientboundGameEventPacket.START_RAINING) {
                    args.skip = true;
                }
                if (packet.getEvent() == ClientboundGameEventPacket.RAIN_LEVEL_CHANGE) {
                    args.skip = true;
                }
                if (packet.getEvent() == ClientboundGameEventPacket.STOP_RAINING) {
                    args.skip = true;
                }
            }
        }
    }
}