package com.zergatul.cheatutils.mixins.common.accessors;

import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.component.ChargedProjectiles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CrossbowItem.class)
public interface CrossbowItemAccessor {

    @Invoker("getShootingPower")
    static float getShootingPower_CU(ChargedProjectiles projectiles) {
        throw new AssertionError();
    }
}