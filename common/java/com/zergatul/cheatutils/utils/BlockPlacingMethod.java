package com.zergatul.cheatutils.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public enum BlockPlacingMethod {
    ANY,
    BOTTOM_SLAB,
    TOP_SLAB,
    FACING_TOP,
    FACING_BOTTOM,
    FACING_EAST,
    FACING_WEST,
    FACING_SOUTH,
    FACING_NORTH,
    FROM_TOP,
    FROM_BOTTOM,
    FROM_HORIZONTAL,
    FROM_EAST,
    FROM_WEST,
    FROM_SOUTH,
    FROM_NORTH,
    ITEM_USE,
    AIR_PLACE;

    public Vec3 getTarget(Vec3 playerPos, BlockPos blockPos, Direction direction, boolean airPlace) {
        return switch (this) {
            case BOTTOM_SLAB -> getBottomSlabTarget(playerPos, blockPos, direction, airPlace);
            case TOP_SLAB -> getTopSlabTarget(playerPos, blockPos, direction, airPlace);
            default -> getTargetDefault(playerPos, blockPos, direction);
        };
    }

    public Rotation getRotation() {
        return switch (this) {
            case FACING_TOP -> new Rotation(90, Float.NaN);
            case FACING_BOTTOM -> new Rotation(-90, Float.NaN);
            case FACING_EAST -> new Rotation(Float.NaN, 90);
            case FACING_WEST -> new Rotation(Float.NaN, -90);
            case FACING_SOUTH -> new Rotation(Float.NaN, 180);
            case FACING_NORTH -> new Rotation(Float.NaN, 0);
            default -> null;
        };
    }

    public Direction[] getAllowedDirections() {
        return switch (this) {
            case FROM_TOP -> new Direction[] { Direction.DOWN };
            case FROM_HORIZONTAL -> new Direction[] { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };
            case FROM_BOTTOM -> new Direction[] { Direction.UP };
            case FROM_EAST -> new Direction[] { Direction.WEST };
            case FROM_WEST -> new Direction[] { Direction.EAST };
            case FROM_SOUTH -> new Direction[] { Direction.NORTH };
            case FROM_NORTH -> new Direction[] { Direction.SOUTH };
            default -> Direction.values();
        };
    }

    public static BlockPlacingMethod facing(Direction direction) {
        return switch (direction) {
            case UP -> FACING_TOP;
            case DOWN -> FACING_BOTTOM;
            case EAST -> FACING_EAST;
            case WEST -> FACING_WEST;
            case NORTH -> FACING_NORTH;
            case SOUTH -> FACING_SOUTH;
        };
    }

    private static Vec3 getTargetDefault(Vec3 playerPos, BlockPos blockPos, Direction direction) {
        return new Vec3(
                blockPos.getX() + 0.5f + direction.getOpposite().getStepX() * 0.5,
                blockPos.getY() + 0.5f + direction.getOpposite().getStepY() * 0.5,
                blockPos.getZ() + 0.5f + direction.getOpposite().getStepZ() * 0.5);
    }

    private static Vec3 getBottomSlabTarget(Vec3 playerPos, BlockPos blockPos, Direction direction, boolean airPlace) {
        if (direction == Direction.UP) {
            return !airPlace ? getTargetDefault(playerPos, blockPos, direction) : null;
        } else if (direction == Direction.DOWN) {
            return null;
        } else {
            return new Vec3(
                    blockPos.getX() + 0.5f + direction.getOpposite().getStepX() * 0.5,
                    blockPos.getY() + 0.25f,
                    blockPos.getZ() + 0.5f + direction.getOpposite().getStepZ() * 0.5);
        }
    }

    private static Vec3 getTopSlabTarget(Vec3 playerPos, BlockPos blockPos, Direction direction, boolean airPlace) {
        if (direction == Direction.DOWN) {
            return !airPlace ? getTargetDefault(playerPos, blockPos, direction) : null;
        } else if (direction == Direction.UP) {
            return null;
        } else {
            return new Vec3(
                    blockPos.getX() + 0.5f + direction.getOpposite().getStepX() * 0.5,
                    blockPos.getY() + 0.75f,
                    blockPos.getZ() + 0.5f + direction.getOpposite().getStepZ() * 0.5);
        }
    }
}