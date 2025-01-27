package com.zergatul.cheatutils.scripting;

@FunctionalInterface
public interface HitboxSizeFunction {
    boolean shouldApply(int id);
}