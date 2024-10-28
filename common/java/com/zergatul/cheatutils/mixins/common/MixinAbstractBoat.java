package com.zergatul.cheatutils.mixins.common;

import com.zergatul.cheatutils.configs.BoatHackConfig;
import com.zergatul.cheatutils.configs.ConfigStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractBoat.class)
public abstract class MixinAbstractBoat extends Entity {

    @Shadow
    private float invFriction;

    @Shadow
    private boolean inputUp;

    @Shadow
    private boolean inputLeft;

    @Shadow
    private boolean inputRight;

    @Shadow
    private boolean inputDown;

    private MixinAbstractBoat(EntityType<?> p_19870_, Level p_19871_) {
        super(p_19870_, p_19871_);
    }

    @ModifyArg(
            method = "floatBoat()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/AbstractBoat;setDeltaMovement(DDD)V", ordinal = 0),
            index = 0)
    private double onFloatBoatSetDeltaMovementX(double dx) {
        var config = ConfigStore.instance.getConfig().boatHackConfig;
        if (config.overrideFriction) {
            return dx / invFriction * config.friction;
        } else {
            return dx;
        }
    }

    @ModifyArg(
            method = "floatBoat()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/AbstractBoat;setDeltaMovement(DDD)V", ordinal = 0),
            index = 2)
    private double onFloatBoatSetDeltaMovementZ(double dz) {
        var config = ConfigStore.instance.getConfig().boatHackConfig;
        if (config.overrideFriction) {
            return dz / invFriction * config.friction;
        } else {
            return dz;
        }
    }

    @Inject(at = @At("HEAD"), method = "controlBoat()V")
    private void onControlBoat(CallbackInfo info) {
        if (!level().isClientSide()) {
            return;
        }

        BoatHackConfig config = ConfigStore.instance.getConfig().boatHackConfig;
        if (config.fly) {
            this.inputUp = false;
            this.inputLeft = false;
            this.inputRight = false;
            this.inputDown = false;
        }
    }

    @Override
    public void move(MoverType type, Vec3 speed) {
        if (!level().isClientSide()) {
            super.move(type, speed);
            return;
        }

        BoatHackConfig config = ConfigStore.instance.getConfig().boatHackConfig;
        if (!config.fly) {
            super.move(type, speed);
            return;
        }

        Boat boat = (Boat) (Object) this;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (boat.getControllingPassenger() != player) {
            super.move(type, speed);
            return;
        }

        boat.setYRot(player.getYRot());

        double angle = boat.getYRot() * Math.PI / 180;
        speed = Vec3.ZERO;

        boolean up = mc.options.keyUp.isDown();
        boolean down = mc.options.keyDown.isDown();
        boolean left = mc.options.keyLeft.isDown();
        boolean right = mc.options.keyRight.isDown();
        if (up && down) {
            up = false;
            down = false;
        }
        if (left && right) {
            left = false;
            right = false;
        }
        if (up || down || left || right) {
            if (up) {
                if (left) {
                    angle -= Math.PI / 4;
                } else if (right) {
                    angle += Math.PI / 4;
                }
            } else if (down) {
                if (left) {
                    angle -= 3 * Math.PI / 4;
                } else if (right) {
                    angle += 3 * Math.PI / 4;
                } else {
                    angle += Math.PI;
                }
            } else if (left) {
                angle -= Math.PI / 2;
            } else {
                angle += Math.PI / 2;
            }
            speed = new Vec3(-config.horizontalFlySpeed / 20 * Math.sin(angle), 0, config.horizontalFlySpeed / 20 * Math.cos(angle));
        }

        if (mc.options.keyJump.isDown()) {
            speed = speed.add(0, config.verticalFlySpeed / 20, 0);
        }
        if (mc.options.keySprint.isDown()) {
            speed = speed.add(0, -config.verticalFlySpeed / 20, 0);
        }

        this.setDeltaMovement(speed);

        super.move(type, speed);
    }
}