package com.zergatul.cheatutils.modules.automation;

import com.zergatul.cheatutils.common.Events;
import com.zergatul.cheatutils.modules.Module;
import com.zergatul.cheatutils.utils.Rotation;
import com.zergatul.cheatutils.utils.RotationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class AimAssist implements Module {

    public static final AimAssist instance = new AimAssist();

    private final Minecraft mc = Minecraft.getInstance();

    private boolean enabled;
    private Entity target;

    private AimAssist() {
        Events.BeforeGameRender.add(this::onRender);
    }

    public void enable() {
        enabled = true;
    }

    public void disable() {
        enabled = false;
        target = null;
    }

    public Entity getTargetEntity() {
        return target;
    }

    private void onRender() {
        if (!enabled) {
            return;
        }
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (!mc.player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BOW)) {
            return;
        }
        if (!mc.player.isUsingItem()) {
            return;
        }

        if (target != null && mc.player.clientLevel.getEntity(target.getId()) == null) {
            target = null;
        }
        if (target != null && !target.isAlive()) {
            target = null;
        }

        float partialTicks = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Rotation playerRot = new Rotation(mc.player.getXRot(partialTicks), mc.player.getYRot(partialTicks));
        if (target == null) {
            target = findTarget(playerRot, partialTicks);
            if (target == null) {
                return;
            }
        }

        int ticks = mc.player.getTicksUsingItem();
        float power = BowItem.getPowerForTime(ticks);
        float speed = power * 3;

        List<Rotation> rotations = findRotations(mc.player, speed, partialTicks);
        Optional<Rotation> closest = rotations.stream().min(Comparator.comparingDouble(playerRot::distanceSqrTo));
        if (closest.isPresent()) {
            Rotation rotation = closest.get();
            float dxRot = rotation.xRot() - playerRot.xRot();
            float dyRot = rotation.yRot() - playerRot.yRot();
            mc.player.setXRot(mc.player.getXRot() + dxRot);
            mc.player.setYRot(mc.player.getYRot() + dyRot);
            mc.player.setXRot(Mth.clamp(mc.player.getXRot(), -90.0F, 90.0F));
            mc.player.xRotO += dxRot;
            mc.player.yRotO += dyRot;
            mc.player.xRotO = Mth.clamp(mc.player.xRotO, -90.0F, 90.0F);
            if (mc.player.getVehicle() != null) {
                mc.player.getVehicle().onPassengerTurned(mc.player);
            }
        } else {
            target = null;
        }
    }

    private Entity findTarget(Rotation playerRot, float partialTicks) {
        assert mc.player != null;

        Entity target = null;
        double bestDeltaAngleSqr = Double.MAX_VALUE;
        for (Entity entity : mc.player.clientLevel.entitiesForRendering()) {
            if (entity instanceof Monster monster /*|| entity instanceof Slime*/) {
                Vec3 center = getEntityCenter(entity, partialTicks);
                Rotation rotation = RotationUtils.getRotation(mc.player.getEyePosition(), center);
                double deltaAngleSqr = playerRot.distanceSqrTo(rotation);
                if (deltaAngleSqr < bestDeltaAngleSqr) {
                    bestDeltaAngleSqr = deltaAngleSqr;
                    target = entity;
                }
            }
        }

        return target;
    }

    private List<Rotation> findRotations(LocalPlayer player, float speed, float partialTicks) {
        assert target != null;

        Rotation straight = RotationUtils.getRotation(player.getEyePosition(partialTicks), target.position());
        Rotation rot1 = findRotation(player, speed, straight.withXRot(-90), partialTicks);
        Rotation rot2 = findRotation(player, speed, straight.withXRot(90), partialTicks);
        if (rot1 == null && rot2 == null) {
            return List.of();
        }
        if (rot1 == null) {
            return List.of(rot2);
        }
        if (rot2 == null) {
            return List.of(rot1);
        }
        if (rot1.approximateEquals(rot2, 0.05F)) {
            return List.of(rot1);
        }
        return List.of(rot1, rot2);
    }

    private Rotation findRotation(LocalPlayer player, float speed, Rotation initial, float partialTicks) {
        Rotation rotation = initial;
        double bestDistance = calculatePath(player, speed, rotation.yRot(), rotation.xRot(), partialTicks).getClosestDistance();

        float delta = 5;
        while (delta > 0.02F) {
            Path path;
            boolean changed = false;

            path = calculatePath(player, speed, rotation.yRot() + delta, rotation.xRot(), partialTicks);
            if (path.getClosestDistance() < bestDistance) {
                rotation = rotation.addYRot(delta);
                bestDistance = path.getClosestDistance();
                changed = true;
            }

            path = calculatePath(player, speed, rotation.yRot() - delta, rotation.xRot(), partialTicks);
            if (path.getClosestDistance() < bestDistance) {
                rotation = rotation.addYRot(-delta);
                bestDistance = path.getClosestDistance();
                changed = true;
            }

            if (rotation.xRot() + delta <= 90) {
                path = calculatePath(player, speed, rotation.yRot(), rotation.xRot() + delta, partialTicks);
                if (path.getClosestDistance() < bestDistance) {
                    rotation = rotation.addXRot(delta);
                    bestDistance = path.getClosestDistance();
                    changed = true;
                }
            }

            if (rotation.xRot() - delta >= -90) {
                path = calculatePath(player, speed, rotation.yRot(), rotation.xRot() - delta, partialTicks);
                if (path.getClosestDistance() < bestDistance) {
                    rotation = rotation.addXRot(-delta);
                    bestDistance = path.getClosestDistance();
                    changed = true;
                }
            }

            if (!changed) {
                delta /= 5;
            }
        }

        if (bestDistance < target.getBbWidth() / 2) {
            return rotation;
        } else {
            return null;
        }
    }

    private Path calculatePath(LocalPlayer player, float speed, float yRot, float xRot, float partialTicks) {
        float speedX = -Mth.sin(yRot * ((float)Math.PI / 180F)) * Mth.cos(xRot * ((float)Math.PI / 180F));
        float speedY = -Mth.sin(xRot * ((float)Math.PI / 180F));
        float speedZ = Mth.cos(yRot * ((float)Math.PI / 180F)) * Mth.cos(xRot * ((float)Math.PI / 180F));
        Vec3 deltaMovement = new Vec3(speedX, speedY, speedZ).normalize().scale(speed);

        Vec3 playerSpeed = player.getDeltaMovement();
        if (player.onGround()) {
            playerSpeed = playerSpeed.subtract(0, playerSpeed.y, 0);
        }
        deltaMovement = deltaMovement.add(playerSpeed);

        Path path = new Path();
        path.entityPosition[0] = target.getEyePosition(partialTicks); //getEntityCenter(target, partialTicks);
        path.arrowPosition[0] = player.getEyePosition(partialTicks);

        Vec3 entityDeltaMovement = getEntitySpeed(target);
        for (int i = 1; i < 200; i++) {
            path.entityPosition[i] = path.entityPosition[i - 1].add(entityDeltaMovement);
            path.arrowPosition[i] = path.arrowPosition[i - 1].add(deltaMovement);
            deltaMovement = deltaMovement.scale(0.99F).add(0, -0.05, 0);
        }

        return path;
    }

    private Vec3 getEntityCenter(Entity entity, float partialTicks) {
        return entity.getPosition(partialTicks).add(0, entity.getBbHeight() / 2, 0);
    }

    private Vec3 getEntitySpeed(Entity entity) {
        if (entity.isPassenger()) {
            return getEntitySpeed(entity.getVehicle());
        }
        return new Vec3(entity.getX() - entity.xo, entity.getY() - entity.yo, entity.getZ() - entity.zo);
    }

    private static class Path {

        public final Vec3[] entityPosition;
        public final Vec3[] arrowPosition;

        public Path() {
            entityPosition = new Vec3[200];
            arrowPosition = new Vec3[200];
        }

        public double getClosestDistance() {
            double closest = Double.MAX_VALUE;
            for (int i = 1; i < 200; i++) {
                double distanceSqr = getPointToLineSegmentDistanceSqr(entityPosition[i], arrowPosition[i - 1], arrowPosition[i]);
                if (distanceSqr < closest) {
                    closest = distanceSqr;
                }
            }
            return Math.sqrt(closest);
        }

        private double getPointToLineSegmentDistanceSqr(Vec3 point, Vec3 line1, Vec3 line2) {
            Vec3 lineVector = line2.subtract(line1);
            Vec3 pointVec1 = point.subtract(line1);

            // Point is lagging behind start of the segment, so perpendicular distance is not viable.
            // Use distance to start of segment instead.
            if (pointVec1.dot(lineVector) <= 0) {
                return pointVec1.lengthSqr();
            }

            Vec3 pointVec2 = point.subtract(line2);

            // Point is advanced past the end of the segment, so perpendicular distance is not viable.
            // Use distance to end of the segment instead.
            if (pointVec2.dot(lineVector) >= 0) {
                return pointVec2.lengthSqr();
            }

            return lineVector.cross(pointVec1).lengthSqr() / lineVector.lengthSqr();
        }
    }
}