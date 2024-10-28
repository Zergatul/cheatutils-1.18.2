package com.zergatul.cheatutils.mixins.common;

import com.zergatul.cheatutils.common.Events;
import com.zergatul.cheatutils.common.events.RenderWorldLayerEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// moved into separate class to fight conflict with sodium
@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer2 {

    @Inject(
            method = "renderSectionLayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;clearRenderState()V"),
            require = 0)
    private void onRenderChunkLayer(RenderType type, double p_172996_, double p_172997_, double p_172998_, Matrix4f pose, Matrix4f projection, CallbackInfo info) {
        if (type == RenderType.solid()) {
            Events.RenderSolidLayer.trigger(new RenderWorldLayerEvent(pose, projection, Minecraft.getInstance().gameRenderer.getMainCamera()));
        }
    }
}