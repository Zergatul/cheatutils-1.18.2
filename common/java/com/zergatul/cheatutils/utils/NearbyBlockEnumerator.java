package com.zergatul.cheatutils.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class NearbyBlockEnumerator {

    private static final Minecraft mc = Minecraft.getInstance();

    public static List<BlockPos> getPositions(Vec3 source, double maxRange) {
        assert mc.level != null;

        double maxRangeSqr = maxRange * maxRange;
        int delta = (int) Math.ceil(maxRange) + 1;

        int xc = (int) Math.round(source.x);
        int yc = (int) Math.round(source.y);
        int zc = (int) Math.round(source.z);
        int x1 = xc - delta;
        int x2 = xc + delta;
        int y1 = Math.max(yc - delta, mc.level.getMinY());
        int y2 = Math.min(yc + delta, mc.level.getMaxY());
        int z1 = zc - delta;
        int z2 = zc + delta;

        List<BlockPos> positions = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = x1; x <= x2; x++) {
            pos.setX(x);
            for (int y = y1; y <= y2; y++) {
                pos.setY(y);
                for (int z = z1; z <= z2; z++) {
                    pos.setZ(z);

                    double distanceSqr = pos.distToCenterSqr(source);
                    if (distanceSqr > maxRangeSqr) {
                        continue;
                    }

                    positions.add(pos.immutable());
                }
            }
        }

        positions.sort(Comparator.comparingDouble(p -> p.distToCenterSqr(source)));

        return positions;
    }
}