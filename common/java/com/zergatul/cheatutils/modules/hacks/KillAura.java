package com.zergatul.cheatutils.modules.hacks;

import com.zergatul.cheatutils.common.Events;
import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.configs.KillAuraConfig;
import com.zergatul.cheatutils.controllers.FakeRotationController;
import com.zergatul.cheatutils.controllers.NetworkPacketsController;
import com.zergatul.cheatutils.controllers.PlayerMotionController;
import com.zergatul.cheatutils.modules.Module;
import com.zergatul.cheatutils.utils.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class KillAura implements Module {

    public static final KillAura instance = new KillAura();

    private final Minecraft mc = Minecraft.getInstance();
    private long ticks;
    private long lastAttackTick;
    private Entity target;
    private final List<Entity> targets = new ArrayList<>();

    private KillAura() {
        PlayerMotionController.instance.addOnAfterSendPosition(this::onAfterSendPosition);
        Events.ClientPlayerLoggingIn.add(this::onPlayerLoggingIn);
        Events.ClientTickEnd.add(this::onClientTickEnd);
        Events.DimensionChange.add(this::onDimensionChange);
    }

    public void onEnabled() {
        lastAttackTick = 0;
    }

    private void onPlayerLoggingIn(Connection connection) {
        ticks = 0;
        lastAttackTick = 0;
    }

    private void onDimensionChange() {
        lastAttackTick = 0;
    }

    private void onClientTickEnd() {
        ticks++;

        KillAuraConfig config = ConfigStore.instance.getConfig().killAuraConfig;
        if (!config.enabled) {
            target = null;
            targets.clear();
            return;
        }

        LocalPlayer player = mc.player;
        ClientLevel world = mc.level;

        if (player == null || world == null) {
            return;
        }

        if (!shouldAttackNow(config)) {
            return;
        }

        target = null;
        targets.clear();
        int targetPriority = Integer.MAX_VALUE;
        double targetDistance2 = Double.MAX_VALUE;

        double maxRange2 = getRangeSquared(config);
        Vec3 eyePos = player.getEyePosition();

        for (Entity entity : world.entitiesForRendering()) {
            double distance2 = entity.distanceToSqr(eyePos);
            if (distance2 > maxRange2) {
                continue;
            }

            if (entity instanceof LivingEntity living) {
                if (!living.isAlive()) {
                    continue;
                }
            }

            int priority = getPriority(config, entity);
            if (priority < 0) {
                continue;
            }

            if (config.maxHorizontalAngle != null || config.maxVerticalAngle != null) {
                Vec3 attackPoint = getAttackPoint(entity);
                Vec3 diff = attackPoint.subtract(eyePos);
                double diffXZ = Math.sqrt(diff.x * diff.x + diff.y * diff.y);

                if (config.maxHorizontalAngle != null) {
                    double targetYRot = Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90F;
                    double delta = MathUtils.deltaAngle180(targetYRot, player.getYRot());
                    if (delta > config.maxHorizontalAngle) {
                        continue;
                    }
                }

                if (config.maxVerticalAngle != null) {
                    double targetXRot = Math.toDegrees(-Math.atan2(diff.y, diffXZ));
                    double delta = MathUtils.deltaAngle180(targetXRot, player.getXRot());
                    if (delta > config.maxVerticalAngle) {
                        continue;
                    }
                }
            }

            if (config.attackAll) {
                targets.add(entity);
            } else {
                if (priority < targetPriority || (priority == targetPriority && distance2 < targetDistance2)) {
                    target = entity;
                    targetPriority = priority;
                    targetDistance2 = distance2;
                }
            }
        }

        if (target != null) {
            FakeRotationController.instance.setServerRotation(getAttackPoint(target));
        }

        if (targets.size() > 0) {
            targets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(eyePos)));
        }
    }

    private Vec3 getAttackPoint(Entity entity) {
        return entity.getBoundingBox().getCenter();
    }

    private int getPriority(KillAuraConfig config, Entity entity) {
        int i = 0;
        for (KillAuraConfig.PriorityEntry entry : config.priorities) {
            if (entry.enabled && entry.predicate.test(entity)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    private void onAfterSendPosition() {
        if (target != null) {
            LocalPlayer player = mc.player;
            mc.gameMode.attack(player, target);
            mc.player.swing(InteractionHand.MAIN_HAND);
            target = null;

            lastAttackTick = ticks;
        }
        if (targets.size() > 0) {
            LocalPlayer player = mc.player;
            mc.gameMode.attack(player, targets.get(0));
            for (int i = 1; i < targets.size(); i++) {
                NetworkPacketsController.instance.sendPacket(ServerboundInteractPacket.createAttackPacket(targets.get(i), player.isShiftKeyDown()));
            }
            mc.player.swing(InteractionHand.MAIN_HAND);
            targets.clear();

            lastAttackTick = ticks;
        }
    }

    private double getRangeSquared(KillAuraConfig config) {
        double range = config.overrideAttackRange ? config.maxRange : 3.0;
        return range * range;
    }

    private boolean shouldAttackNow(KillAuraConfig config) {
        if (KillAuraConfig.Cooldown.equals(config.delayMode)) {
            return mc.player.getAttackStrengthScale((float) -config.extraTicks) == 1;
        } else {
            return ticks - lastAttackTick >= config.attackTickInterval;
        }
    }
}