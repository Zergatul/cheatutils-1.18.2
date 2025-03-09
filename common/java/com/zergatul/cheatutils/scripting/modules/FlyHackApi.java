package com.zergatul.cheatutils.scripting.modules;

import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.configs.FlyHackConfig;

@SuppressWarnings("unused")
public class FlyHackApi extends ModuleApi<FlyHackConfig> {

    public double getFlySpeed() {
        return getConfig().flyingSpeed;
    }

    public void setFlySpeed(double speed) {
        getConfig().flyingSpeed = (float)speed;
        ConfigStore.instance.requestWrite();
    }

    @Override
    protected FlyHackConfig getConfig() {
        return ConfigStore.instance.getConfig().flyHackConfig;
    }
}