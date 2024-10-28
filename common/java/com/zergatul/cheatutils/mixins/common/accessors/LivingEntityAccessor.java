package com.zergatul.cheatutils.mixins.common.accessors;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {

    @Accessor("swimAmount")
    float getSwimAmount_CU();

    @Accessor("swimAmount")
    void setSwimAmount_CU(float value);

    @Accessor("swimAmountO")
    void setSwimAmount0_CU(float value);
}