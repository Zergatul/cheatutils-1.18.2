package com.zergatul.cheatutils.controllers;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TeleportHackController {

    public static final TeleportHackController instance = new TeleportHackController();

    private final double minDistanceSqr = 1; // skip teleports for such distances
    private final Minecraft mc = Minecraft.getInstance();

    private TeleportHackController() {

    }

    public boolean teleportToCrosshair(double distance, int repeats) {
        if (distance < 1) {
            return false;
        }

        if (mc.player == null || mc.level == null) {
            return false;
        }

        HitResult result = mc.player.pick(distance, 1, false);
        if (result instanceof BlockHitResult blockHitResult) {
            Vec3 target = blockHitResult.getLocation();
            Vec3 pos = mc.player.getPosition(1);
            if (pos.distanceToSqr(target) < minDistanceSqr) {
                return false;
            }

            Vec3 direction = target.subtract(target).normalize();
            return teleport(pos, target, pos.add(direction), false, repeats);
        } else {
            return false;
        }
    }

    public boolean verticalTeleport(double distance, int repeats) {
        return verticalTeleport(distance, Math.signum(distance), false, repeats);
    }

    public boolean verticalTeleport(double from, double to, boolean findSurface, int repeats) {
        if (Math.signum(from) != Math.signum(to)) {
            return false;
        }

        if (Math.abs(from) < 1 || Math.abs(to) < 1) {
            return false;
        }

        if (mc.player == null || mc.level == null) {
            return false;
        }

        Vec3 pos = mc.player.getPosition(1);
        return teleport(pos, pos.add(0, from, 0), pos.add(0, to, 0), findSurface, repeats);
    }

    private boolean teleport(Vec3 playerPos, Vec3 from, Vec3 to, boolean findSurface, int repeats) {
            if (mc.player == null || mc.level == null) {
            return false;
        }

        double maxDistanceSqr = from.distanceToSqr(to);
        if (maxDistanceSqr < 0.01f) {
            return false;
        }

        EntityDimensions dimensions = mc.player.getDimensions(mc.player.getPose());
        Vec3 direction = to.subtract(from).normalize();

        Vec3 target = from;
        while (true) {
            if (from.distanceToSqr(target) > maxDistanceSqr) {
                return false;
            }

            Iterable<VoxelShape> collisions = mc.level.getBlockCollisions(mc.player, dimensions.makeBoundingBox(target));
            if (!collisions.iterator().hasNext()) {
                // no collisions
                if (findSurface) {
                    Iterable<VoxelShape> belowCollisions = mc.level.getBlockCollisions(
                            mc.player,
                            dimensions.makeBoundingBox(target.add(0, -0.1, 0)));
                    if (belowCollisions.iterator().hasNext()) {
                        break;
                    }
                } else {
                    break;
                }
            }

            // move target
            double mult = dimensions.width() * 0.1;
            target = target.add(direction.x * mult, direction.y * mult, direction.z * mult);
        }

        for (int i = 0 ; i < repeats; i++) {
            NetworkPacketsController.instance.sendPacket(
                    new ServerboundMovePlayerPacket.Pos(playerPos.x, playerPos.y, playerPos.z, mc.player.onGround(), false));
        }

        NetworkPacketsController.instance.sendPacket(
                new ServerboundMovePlayerPacket.Pos(target.x, target.y, target.z, findSurface, false));

        mc.player.moveTo(target);

        return true;
    }
}