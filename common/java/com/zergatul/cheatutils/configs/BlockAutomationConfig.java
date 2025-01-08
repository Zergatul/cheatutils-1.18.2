package com.zergatul.cheatutils.configs;

public class BlockAutomationConfig extends BlockPlacerConfig implements ValidatableConfig {

    public String code;
    public boolean debugMode;

    @Override
    public void copyTo(BlockAutomationConfig other) {
        super.copyTo(other);
        other.debugMode = debugMode;
    }
}