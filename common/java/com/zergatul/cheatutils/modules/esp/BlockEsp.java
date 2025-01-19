package com.zergatul.cheatutils.modules.esp;

import com.zergatul.cheatutils.common.Events;
import com.zergatul.cheatutils.configs.BlockEspConfig;
import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.modules.utilities.RenderUtilities;
import com.zergatul.cheatutils.render.*;
import com.zergatul.cheatutils.common.events.RenderWorldLastEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BlockEsp {

    public static final BlockEsp instance = new BlockEsp();

    private final List<BlockPos> bbList = new ArrayList<>();
    private final List<BlockPos> tracerList = new ArrayList<>();
    private final List<BlockPos> overlayList = new ArrayList<>();

    private BlockEsp() {
        Events.AfterRenderWorld.add(this::render);
    }

    private void render(RenderWorldLastEvent event) {
        if (!ConfigStore.instance.getConfig().esp) {
            return;
        }

        Vec3 playerPos = event.getPlayerPos();
        double playerX = playerPos.x;
        double playerY = playerPos.y;
        double playerZ = playerPos.z;

        Vec3 tracerCenter = event.getTracerCenter();
        double tracerX = tracerCenter.x;
        double tracerY = tracerCenter.y;
        double tracerZ = tracerCenter.z;

        GroupLineRenderer lineRenderer = RenderUtilities.instance.getGroupLineRenderer();
        GroupThickLineRenderer thickLineRenderer = RenderUtilities.instance.getGroupThickLineRenderer();
        BlockOverlayRenderer overlayRenderer = RenderUtilities.instance.getBlockOverlayRenderer();

        for (BlockEspConfig config : ConfigStore.instance.getConfig().blocks.getBlockConfigs()) {
            if (!config.enabled) {
                continue;
            }

            Set<BlockPos> set = BlockFinder.instance.blocks.get(config);
            if (set == null || set.isEmpty()) {
                continue;
            }

            double tracerMaxDistanceSqr = config.getTracerMaxDistanceSqr();
            double outlineMaxDistanceSqr = config.getOutlineMaxDistanceSqr();
            double overlayMaxDistanceSqr = config.getOverlayMaxDistanceSqr();

            bbList.clear();
            tracerList.clear();
            overlayList.clear();

            for (BlockPos pos : set) {
                double dx = pos.getX() - playerX;
                double dy = pos.getY() - playerY;
                double dz = pos.getZ() - playerZ;
                double distanceSqr = dx * dx + dy * dy + dz * dz;

                if (config.drawOutline && distanceSqr < outlineMaxDistanceSqr) {
                    bbList.add(pos);
                }

                if (config.drawTracers && distanceSqr < tracerMaxDistanceSqr) {
                    tracerList.add(pos);
                }

                if (config.drawOverlay && distanceSqr < overlayMaxDistanceSqr) {
                    overlayList.add(pos);
                }
            }

            if (!bbList.isEmpty()) {
                final int lineWidth = config.outlineWidth;
                CuboidRenderer renderer;
                if (lineWidth == 1) {
                    lineRenderer.begin(event);
                    renderer = lineRenderer;
                } else {
                    thickLineRenderer.begin(event, lineWidth);
                    renderer = thickLineRenderer;
                }

                for (BlockPos pos : bbList) {
                    float x = pos.getX();
                    float y = pos.getY();
                    float z = pos.getZ();
                    renderer.cuboid(x, y, z, x + 1, y + 1, z + 1);
                }

                if (lineWidth == 1) {
                    lineRenderer.end(config.outlineColor);
                } else {
                    thickLineRenderer.end(config.outlineColor);
                }
            }

            if (!tracerList.isEmpty()) {
                final int lineWidth = config.tracerWidth;
                SimpleLineRenderer renderer;
                if (lineWidth == 1) {
                    lineRenderer.begin(event);
                    renderer = lineRenderer;
                } else {
                    thickLineRenderer.begin(event, lineWidth);
                    renderer = thickLineRenderer;
                }

                for (BlockPos pos : tracerList) {
                    renderer.line(
                            tracerX, tracerY, tracerZ,
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                }

                if (lineWidth == 1) {
                    lineRenderer.end(config.tracerColor);
                } else {
                    thickLineRenderer.end(config.tracerColor);
                }
            }

            if (!overlayList.isEmpty()) {
                overlayRenderer.begin(event);
                for (BlockPos pos: overlayList) {
                    renderOverlay(overlayRenderer, pos);
                }
                overlayRenderer.end(
                        config.overlayColor.getRed() / 255f,
                        config.overlayColor.getGreen() / 255f,
                        config.overlayColor.getBlue() / 255f,
                        config.overlayColor.getAlpha() / 255f);
            }
        }
    }

    private void renderOverlay(BlockOverlayRenderer renderer, BlockPos pos) {
        int x1 = pos.getX();
        int y1 = pos.getY();
        int z1 = pos.getZ();
        int x2 = x1 + 1;
        int y2 = y1 + 1;
        int z2 = z1 + 1;

        // bottom
        renderer.quad(
                x2, y1, z2,
                x1, y1, z2,
                x1, y1, z1,
                x2, y1, z1);

        // top
        renderer.quad(
                x2, y2, z2,
                x2, y2, z1,
                x1, y2, z1,
                x1, y2, z2);

        // west
        renderer.quad(
                x1, y1, z1,
                x1, y1, z2,
                x1, y2, z2,
                x1, y2, z1);

        // east
        renderer.quad(
                x2, y1, z1,
                x2, y2, z1,
                x2, y2, z2,
                x2, y1, z2);

        // north
        renderer.quad(
                x2, y1, z1,
                x1, y1, z1,
                x1, y2, z1,
                x2, y2, z1);

        // south
        renderer.quad(
                x2, y1, z2,
                x2, y2, z2,
                x1, y2, z2,
                x1, y1, z2);
    }
}