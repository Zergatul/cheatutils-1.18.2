package com.zergatul.cheatutils.mixins.common;

import com.zergatul.cheatutils.configs.ConfigStore;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WeatherEffectRenderer.class)
public abstract class MixinWeatherEffectRenderer {

    @Inject(
            at = @At("HEAD"),
            method = "render(Lnet/minecraft/world/level/Level;Lnet/minecraft/client/renderer/LightTexture;IFLnet/minecraft/world/phys/Vec3;)V",
            cancellable = true)
    private void onBeforeRender(Level level, LightTexture light, int p_365872_, float p_365795_, Vec3 p_361547_, CallbackInfo info) {
        if (ConfigStore.instance.getConfig().noWeatherConfig.enabled) {
            info.cancel();
        }
    }
}