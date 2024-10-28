package com.zergatul.cheatutils.mixins.common;

import com.zergatul.cheatutils.common.Events;
import com.zergatul.cheatutils.common.events.SetupFogEvent;
import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.configs.FogConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public abstract class MixinFogRenderer {

    @Inject(at = @At("HEAD"), method = "setupFog", cancellable = true)
    private static void onBeforeSetupFog(Camera camera, FogRenderer.FogMode mode, Vector4f vector, float p_234175_, boolean p_234176_, float p_234177_, CallbackInfoReturnable<FogParameters> info) {
        FogConfig config = ConfigStore.instance.getConfig().fogConfig;
        if (config.enabled && FogConfig.METHOD_SKIP_SETUP_FOG.equals(config.method)) {
            info.setReturnValue(FogParameters.NO_FOG);
        }
    }

    @Inject(at = @At("RETURN"), method = "setupFog", cancellable = true)
    private static void onAfterSetupFog(Camera camera, FogRenderer.FogMode mode, Vector4f vector, float p_234175_, boolean p_234176_, float p_234177_, CallbackInfoReturnable<FogParameters> info) {
        FogParameters parameters = info.getReturnValue();
        SetupFogEvent event = new SetupFogEvent(parameters.start(), parameters.end());
        Events.SetupFog.trigger(event);
        if (parameters.start() != event.getFogStart() || parameters.end() != event.getFogEnd()) {
            info.setReturnValue(new FogParameters(
                    event.getFogStart(),
                    event.getFogEnd(),
                    parameters.shape(),
                    parameters.red(),
                    parameters.green(),
                    parameters.blue(),
                    parameters.alpha()));
        }
    }
}