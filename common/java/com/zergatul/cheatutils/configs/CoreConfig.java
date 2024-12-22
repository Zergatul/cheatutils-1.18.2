package com.zergatul.cheatutils.configs;

import com.zergatul.cheatutils.utils.MathUtils;

public class CoreConfig implements ValidatableConfig {

    public int port;
    public boolean advancedScripting;

    public CoreConfig() {
        port = 5005;
    }

    @Override
    public void validate() {
        port = MathUtils.clamp(port, 1, 65535);
    }
}