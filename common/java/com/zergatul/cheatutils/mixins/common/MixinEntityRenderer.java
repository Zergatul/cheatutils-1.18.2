package com.zergatul.cheatutils.mixins.common;

import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.configs.EntityEspConfig;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

    @Shadow
    protected abstract boolean shouldShowName(Entity entity, double distanceSquared);

    @Redirect(
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;shouldShowName(Lnet/minecraft/world/entity/Entity;D)Z"),
            method = "extractRenderState")
    private boolean onInvokeShouldShowName(EntityRenderer renderer, Entity entity, double p_363875_) {
        if (!this.shouldShowName(entity, 1)) {
            return false;
        }
        for (EntityEspConfig entityConfig : ConfigStore.instance.getConfig().entities.configs) {
            if (entityConfig.enabled && entityConfig.drawTitles && entityConfig.isValidEntity(entity)) {
                return false;
            }
        }
        return true;
    }
}