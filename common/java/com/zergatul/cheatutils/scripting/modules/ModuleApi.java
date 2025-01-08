package com.zergatul.cheatutils.scripting.modules;

import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.configs.ModuleConfig;
import com.zergatul.cheatutils.scripting.ApiVisibility;
import com.zergatul.cheatutils.scripting.ApiType;
import com.zergatul.scripting.MethodDescription;

public abstract class ModuleApi<T extends ModuleConfig> {

    @MethodDescription("""
            Checks if module is enabled
            """)
    public boolean isEnabled() {
        return getConfig().enabled;
    }

    @MethodDescription("""
            Enables module
            """)
    @ApiVisibility(ApiType.UPDATE)
    public void enable() {
        var config = getConfig();
        if (!config.enabled) {
            config.enabled = true;
            onEnableChanged();
            ConfigStore.instance.requestWrite();
        }
    }

    @MethodDescription("""
            Disables module
            """)
    @ApiVisibility(ApiType.UPDATE)
    public void disable() {
        var config = getConfig();
        if (config.enabled) {
            config.enabled = false;
            onEnableChanged();
            ConfigStore.instance.requestWrite();
        }
    }

    @MethodDescription("""
            Sets module enabled status
            """)
    @ApiVisibility(ApiType.UPDATE)
    public void setEnabled(boolean value) {
        var config = getConfig();
        if (config.enabled != value) {
            config.enabled = value;
            onEnableChanged();
            ConfigStore.instance.requestWrite();
        }
    }

    @MethodDescription("""
            Toggles module enabled state
            """)
    @ApiVisibility(ApiType.UPDATE)
    public void toggle() {
        var config = getConfig();
        config.enabled = !config.enabled;
        onEnableChanged();
        ConfigStore.instance.requestWrite();
    }

    protected void onEnableChanged() {

    }

    protected abstract T getConfig();
}