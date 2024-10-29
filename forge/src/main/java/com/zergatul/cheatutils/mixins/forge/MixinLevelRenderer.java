package com.zergatul.cheatutils.mixins.forge;

import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zergatul.cheatutils.entities.FakePlayer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer {

    @Shadow
    @Final
    private RenderBuffers renderBuffers;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    protected abstract void renderEntity(Entity p_109518_, double p_109519_, double p_109520_, double p_109521_, float p_109522_, PoseStack p_109523_, MultiBufferSource p_109524_);

    @Inject(
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endLastBatch()V", ordinal = 0),
            method = "lambda$addMainPass$1",
            require = 0)
    private void onAfterRenderEntities(
            FogParameters fog,
            DeltaTracker deltaTracker,
            Camera camera,
            ProfilerFiller profiler,
            Matrix4f pose,
            Matrix4f projection,
            ResourceHandle<?> handle1,
            ResourceHandle<?> handle2,
            ResourceHandle<?> handle3,
            ResourceHandle<?> handle4,
            Frustum frustum,
            boolean p_362593_,
            ResourceHandle<?> handle5,
            CallbackInfo ci
    ) {
        FakePlayer.render(this.minecraft, camera, renderBuffers, this::renderEntity);
    }
}