package com.zergatul.cheatutils.mixins.common;

import com.mojang.authlib.GameProfile;
import com.zergatul.cheatutils.configs.*;
import com.zergatul.cheatutils.controllers.PlayerMotionController;
import com.zergatul.cheatutils.helpers.MixinLocalPlayerHelper;
import com.zergatul.cheatutils.modules.hacks.ElytraFly;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayer extends AbstractClientPlayer {

    private boolean flyHackOverride = false;
    private boolean oldFlying;
    private float oldFlyingSpeed;

    public MixinLocalPlayer(ClientLevel p_234112_, GameProfile p_234113_) {
        super(p_234112_, p_234113_);
    }

    @Inject(at = @At("HEAD"), method = "sendPosition()V")
    private void onBeforeSendPosition(CallbackInfo info) {
        PlayerMotionController.instance.triggerOnBeforeSendPosition();
    }

    @Inject(at = @At("TAIL"), method = "sendPosition()V")
    private void onAfterSendPosition(CallbackInfo info) {
        PlayerMotionController.instance.triggerOnAfterSendPosition();
    }

    @Inject(at = @At("HEAD"), method = "aiStep()V")
    private void onBeforeAiStep(CallbackInfo info) {
        MixinLocalPlayerHelper.insideAiStep = true;

        FlyHackConfig config = ConfigStore.instance.getConfig().flyHackConfig;
        if (config.enabled) {
            LocalPlayer player = (LocalPlayer) (Object) this;

            oldFlying = player.getAbilities().flying;
            oldFlyingSpeed = player.getAbilities().getFlyingSpeed();

            player.getAbilities().flying = true;
            if (config.overrideFlyingSpeed) {
                player.getAbilities().setFlyingSpeed(config.flyingSpeed);
            }

            flyHackOverride = true;
        }

        ElytraFly.instance.onBeforeAiStep();
    }

    @Inject(at = @At("TAIL"), method = "aiStep()V")
    private void onAfterAiStep(CallbackInfo info) {
        MixinLocalPlayerHelper.insideAiStep = false;

        if (flyHackOverride) {
            LocalPlayer player = (LocalPlayer) (Object) this;
            player.getAbilities().flying = oldFlying;
            player.getAbilities().setFlyingSpeed(oldFlyingSpeed);
            flyHackOverride = false;
        }

        ElytraFly.instance.onAfterAiStep();
    }

    @Inject(at = @At("HEAD"), method = "isUsingItem()Z", cancellable = true)
    private void onIsUsingItem(CallbackInfoReturnable<Boolean> info) {
        if (MixinLocalPlayerHelper.insideAiStep) {
            if (ConfigStore.instance.getConfig().movementHackConfig.disableSlowdownOnUseItem) {
                info.setReturnValue(false);
            }
        }
    }

    @ModifyArg(
            method = "aiStep()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/ClientInput;tick(ZF)V"),
            index = 0)
    private boolean onAiStepInputTick(boolean isMovingSlowly) {
        var config = ConfigStore.instance.getConfig().movementHackConfig;
        if (config.disableCrouchingSlowdown) {
            return false;
        }
        return isMovingSlowly;
    }

    @ModifyConstant(method = "aiStep()V", constant = @Constant(floatValue = 3.0f))
    private float onModifyFlyingHorizontalMultiplier(float value) {
        return ElytraFly.instance.onModifyFlyingHorizontalMultiplier(value);
    }

    @Override
    public void lerpMotion(double dx, double dy, double dz) {
        var config = ConfigStore.instance.getConfig().movementHackConfig;
        if (config.antiKnockback) {
            return;
        }
        super.lerpMotion(dx, dy, dz);
    }

    @Override
    public void push(double dx, double dy, double dz) {
        var config = ConfigStore.instance.getConfig().movementHackConfig;
        if (config.antiPush) {
            return;
        }
        super.push(dx, dy, dz);
    }

    @Override
    protected float getJumpPower() {
        MovementHackConfig config = ConfigStore.instance.getConfig().movementHackConfig;
        if (config.scaleJumpHeight) {
            return (float) (Math.sqrt(config.jumpHeightFactor) * super.getJumpPower());
        } else {
            return super.getJumpPower();
        }
    }

    @ModifyArgs(
            method = "move",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"))
    private void onModifyDeltaMove(Args args) {
        MoverType type = args.get(0);
        Vec3 delta = args.get(1);
        if (type == MoverType.SELF) {
            args.set(1, ElytraFly.instance.onModifyDeltaMove(delta));
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean updateFluidHeightAndDoFluidPushing(TagKey<Fluid> fluid, double p_204033_) {
        MovementHackConfig config = ConfigStore.instance.getConfig().movementHackConfig;
        if (config.disableWaterPush) {
            Vec3 delta = this.getDeltaMovement();
            boolean result = super.updateFluidHeightAndDoFluidPushing(fluid, p_204033_);
            this.setDeltaMovement(delta);
            return result;
        } else {
            return super.updateFluidHeightAndDoFluidPushing(fluid, p_204033_);
        }
    }

    @Override
    public double blockInteractionRange() {
        ReachConfig config = ConfigStore.instance.getConfig().reachConfig;
        if (config.overrideReachDistance) {
            return config.reachDistance;
        } else {
            return super.blockInteractionRange();
        }
    }

    @Override
    public double entityInteractionRange() {
        ReachConfig config = ConfigStore.instance.getConfig().reachConfig;
        if (config.overrideAttackRange) {
            return config.attackRange;
        } else {
            return super.entityInteractionRange();
        }
    }

    @Override
    public float maxUpStep() {
        StepUpConfig config = ConfigStore.instance.getConfig().stepUp;
        if (config.enabled) {
            return (float) config.height;
        } else {
            return super.maxUpStep();
        }
    }
}