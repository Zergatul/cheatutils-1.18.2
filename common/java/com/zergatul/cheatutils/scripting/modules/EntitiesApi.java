package com.zergatul.cheatutils.scripting.modules;

import com.zergatul.cheatutils.collections.ImmutableList;
import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.configs.EntityEspConfig;
import com.zergatul.cheatutils.scripting.ApiVisibility;
import com.zergatul.cheatutils.scripting.ApiType;
import com.zergatul.cheatutils.utils.ColorUtils;
import com.zergatul.cheatutils.wrappers.ClassRemapper;
import com.zergatul.scripting.MethodDescription;

import java.awt.*;

@SuppressWarnings("unused")
public class EntitiesApi {

    @MethodDescription("""
            Checks if Entity ESP is enabled for specified entity class
            """)
    public boolean isEnabled(String className) {
        EntityEspConfig config = getConfig(className);
        if (config == null) {
            return false;
        }
        return config.enabled;
    }

    @MethodDescription("""
            Toggles enabled state for specified entity class
            """)
    @ApiVisibility(ApiType.UPDATE)
    public void toggle(String className) {
        EntityEspConfig config = getConfig(className);
        if (config == null) {
            return;
        }
        config.enabled = !config.enabled;
        ConfigStore.instance.requestWrite();
    }

    @ApiVisibility(ApiType.UPDATE)
    public void setTracerColor(String className, String color) {
        Color colorValue = ColorUtils.parseColor2(color);
        if (colorValue == null) {
            return;
        }

        EntityEspConfig config = getConfig(className);
        if (config == null) {
            return;
        }

        config.tracerColor = colorValue;
        ConfigStore.instance.requestWrite();
    }

    @ApiVisibility(ApiType.UPDATE)
    public void setOutlineColor(String className, String color) {
        Color colorValue = ColorUtils.parseColor2(color);
        if (colorValue == null) {
            return;
        }

        EntityEspConfig config = getConfig(className);
        if (config == null) {
            return;
        }

        config.glowColor = colorValue;
        ConfigStore.instance.requestWrite();
    }

    @ApiVisibility(ApiType.UPDATE)
    public void setCollisionBoxColor(String className, String color) {
        Color colorValue = ColorUtils.parseColor2(color);
        if (colorValue == null) {
            return;
        }

        EntityEspConfig config = getConfig(className);
        if (config == null) {
            return;
        }

        config.outlineColor = colorValue;
        ConfigStore.instance.requestWrite();
    }

    @ApiVisibility(ApiType.UPDATE)
    public void setOverlayColor(String className, String color) {
        Color colorValue = ColorUtils.parseColor2(color);
        if (colorValue == null) {
            return;
        }

        EntityEspConfig config = getConfig(className);
        if (config == null) {
            return;
        }

        config.overlayColor = colorValue;
        ConfigStore.instance.requestWrite();
    }

    private EntityEspConfig getConfig(String className) {
        ImmutableList<EntityEspConfig> list = ConfigStore.instance.getConfig().entities.configs;
        return list.stream()
                .filter(c -> c.clazz.getName().equals(ClassRemapper.toObf(className)))
                .findFirst()
                .orElse(null);
    }
}