package com.zergatul.cheatutils.mixins.common;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zergatul.cheatutils.common.Events;
import com.zergatul.cheatutils.common.events.GetFieldOfViewEvent;
import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.modules.esp.FreeCam;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {

    @Inject(at = @At("HEAD"), method = "pick(F)V")
    private void onBeforePick(float vec33, CallbackInfo info) {
        FreeCam.instance.onBeforeGameRendererPick();
    }

    @Inject(at = @At("TAIL"), method = "pick(F)V")
    private void onAfterPick(float vec33, CallbackInfo info) {
        FreeCam.instance.onAfterGameRendererPick();
    }

    @Redirect(
            method = "renderItemInHand",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/CameraType;isFirstPerson()Z", ordinal = 0))
    private boolean onRenderItemInHandIsFirstPerson(CameraType cameraType) {
        return FreeCam.instance.onRenderItemInHandIsFirstPerson(cameraType);
    }

    @Inject(
            at = @At(value = "RETURN", ordinal = 1),
            method = "getFov",
            cancellable = true)
    private void onGetFov(Camera camera, float partialTicks, boolean p_109144_, CallbackInfoReturnable<Float> info) {
        double fov = info.getReturnValue();
        GetFieldOfViewEvent event = new GetFieldOfViewEvent();
        event.set(fov);
        Events.GetFieldOfView.trigger(event);
        if (fov != event.get()) {
            info.setReturnValue((float) event.get());
        }
    }

    @Inject(at = @At("HEAD"), method = "render")
    private void onBeforeRender(DeltaTracker delta, boolean p_109096_, CallbackInfo info) {
        Events.RenderTickStart.trigger(delta);
    }

    @Inject(at = @At("HEAD"), method = "bobHurt(Lcom/mojang/blaze3d/vertex/PoseStack;F)V", cancellable = true)
    private void onBobHurt(PoseStack poseStack, float partialTicks, CallbackInfo info) {
        if (ConfigStore.instance.getConfig().bobHurtConfig.enabled) {
            info.cancel();
        }
    }
}