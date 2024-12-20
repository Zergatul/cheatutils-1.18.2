package com.zergatul.cheatutils.scripting.modules;

import com.zergatul.cheatutils.modules.automation.AimAssist;
import com.zergatul.cheatutils.scripting.ApiType;
import com.zergatul.cheatutils.scripting.ApiVisibility;
import net.minecraft.world.entity.Entity;

@SuppressWarnings("unused")
public class AimAssistApi {

    @ApiVisibility(ApiType.UPDATE)
    public void enable() {
        AimAssist.instance.enable();
    }

    @ApiVisibility(ApiType.UPDATE)
    public void disable() {
        AimAssist.instance.disable();
    }

    public boolean hasTarget() {
        return AimAssist.instance.getTargetEntity() != null;
    }

    public int getEntityId() {
        Entity target = AimAssist.instance.getTargetEntity();
        return target != null ? target.getId() : Integer.MIN_VALUE;
    }
}