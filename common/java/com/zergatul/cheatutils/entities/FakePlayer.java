package com.zergatul.cheatutils.entities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zergatul.cheatutils.mixins.common.accessors.LivingEntityAccessor;
import com.zergatul.cheatutils.mixins.common.accessors.WalkAnimationStateAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class FakePlayer extends RemotePlayer {

    private static final List<FakePlayer> list = new ArrayList<>();

    private final ItemStack mainHand;
    private final ItemStack offHand;
    private final ItemStack feet;
    private final ItemStack legs;
    private final ItemStack chest;
    private final ItemStack head;
    private final ItemStack body;

    public FakePlayer(LocalPlayer player) {
        super((ClientLevel) player.level(), player.getGameProfile());
        list.add(this);

        this.animStep = this.animStepO = 0;
        this.attackAnim = this.oAttackAnim = player.attackAnim;
        this.bob = this.oBob = player.bob;
        this.elytraRotX = player.elytraRotX;
        this.elytraRotY = player.elytraRotY;
        this.elytraRotZ = player.elytraRotZ;
        this.fallFlyTicks = player.getFallFlyingTicks();
        this.hurtDuration = player.hurtDuration;
        this.hurtTime = player.hurtTime;
        this.hurtDir = player.getHurtDir();
        //this.lastHurt = player.lasthurt
        ((LivingEntityAccessor) this).setSwimAmount_CU(((LivingEntityAccessor) player).getSwimAmount_CU());
        ((LivingEntityAccessor) this).setSwimAmount0_CU(((LivingEntityAccessor) player).getSwimAmount_CU());
        this.swinging = player.swinging;
        this.swingTime = player.swingTime;
        this.useItem = player.getUseItem().copy();
        this.xxa = player.xxa;
        this.yya = player.yya;
        this.zza = player.zza;
        this.xCloak = this.xCloakO = player.xCloak;
        this.yCloak = this.yCloakO = player.yCloak;
        this.zCloak = this.zCloakO = player.zCloak;

        ((WalkAnimationStateAccessor) this.walkAnimation).setSpeedOld_CU(player.walkAnimation.speed());
        this.walkAnimation.setSpeed(player.walkAnimation.speed());
        ((WalkAnimationStateAccessor) this.walkAnimation).setPosition_CU(player.walkAnimation.position());

        this.setPos(player.getPosition(1));
        this.setRot(player.getYRot(), player.getXRot());
        this.setOldPosAndRot();

        this.yBodyRot = this.yBodyRotO = player.yBodyRot;
        this.yHeadRot = this.yHeadRotO = player.yHeadRot;

        this.mainHand = player.getItemBySlot(EquipmentSlot.MAINHAND).copy();
        this.offHand = player.getItemBySlot(EquipmentSlot.OFFHAND).copy();
        this.head = player.getItemBySlot(EquipmentSlot.HEAD).copy();
        this.chest = player.getItemBySlot(EquipmentSlot.CHEST).copy();
        this.legs = player.getItemBySlot(EquipmentSlot.LEGS).copy();
        this.feet = player.getItemBySlot(EquipmentSlot.FEET).copy();
        this.body = player.getItemBySlot(EquipmentSlot.BODY).copy();

        this.entityData.set(DATA_SHARED_FLAGS_ID, player.getEntityData().get(DATA_SHARED_FLAGS_ID));
        this.entityData.set(DATA_POSE, player.getEntityData().get(DATA_POSE));
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> mainHand;
            case OFFHAND -> offHand;
            case HEAD -> head;
            case CHEST -> chest;
            case LEGS -> legs;
            case FEET -> feet;
            case BODY -> body;
        };
    }

    @Override
    public void remove(RemovalReason reason) {
        list.remove(this);
    }

    public static void render(Minecraft mc, Camera camera, RenderBuffers renderBuffers, RenderEntityCallback callback) {
        if (list.isEmpty()) {
            return;
        }
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }

        MultiBufferSource.BufferSource source = renderBuffers.bufferSource();
        Vec3 view = camera.getPosition();
        double x = view.x;
        double y = view.y;
        double z = view.z;
        for (FakePlayer fake : list) {
            if (fake.distanceToSqr(player) > 1) {
                // is new PoseStack good for compatibility?
                callback.renderEntity(fake, x, y, z, 1, new PoseStack(), source);
            }
        }
    }

    @FunctionalInterface
    public interface RenderEntityCallback {
        void renderEntity(Entity entity, double x, double y, double z, float partialTicks, PoseStack pose, MultiBufferSource.BufferSource source);
    }
}