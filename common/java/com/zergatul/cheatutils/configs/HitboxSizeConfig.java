package com.zergatul.cheatutils.configs;

import com.zergatul.cheatutils.utils.MathUtils;

import java.util.Objects;

public class HitboxSizeConfig extends ModuleConfig implements ValidatableConfig {

    public static final String MODE_PERCENT = "PERCENT";
    public static final String MODE_ABSOLUTE = "ABSOLUTE";

    public String widthMode;
    public double widthPercent;
    public double widthAbsolute;

    public String heightMode;
    public double heightPercent;
    public double heightAbsolute;

    public boolean scriptEnabled;
    public String code;

    public HitboxSizeConfig() {
        widthMode = MODE_PERCENT;
        widthPercent = 25;
        widthAbsolute = 0.5;
        heightMode = MODE_PERCENT;
        heightPercent = 0;
        heightAbsolute = 0;
    }

    @Override
    public void validate() {
        if (!Objects.equals(widthMode, MODE_PERCENT) && !Objects.equals(widthMode, MODE_ABSOLUTE)) {
            widthMode = MODE_PERCENT;
        }

        if (!Objects.equals(heightMode, MODE_PERCENT) && !Objects.equals(heightMode, MODE_ABSOLUTE)) {
            heightMode = MODE_PERCENT;
        }

        widthPercent = MathUtils.clamp(widthPercent, -99, 1000);
        heightPercent = MathUtils.clamp(heightPercent, -99, 1000);
        widthAbsolute = MathUtils.clamp(widthAbsolute, 0, 100);
        heightAbsolute = MathUtils.clamp(heightAbsolute, 0, 100);
    }
}