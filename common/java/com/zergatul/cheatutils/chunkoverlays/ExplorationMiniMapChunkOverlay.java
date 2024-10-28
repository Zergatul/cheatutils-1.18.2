package com.zergatul.cheatutils.chunkoverlays;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.zergatul.cheatutils.ModMain;
import com.zergatul.cheatutils.concurrent.PreRenderGuiExecutor;
import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.configs.ExplorationMiniMapConfig;
import com.zergatul.cheatutils.render.Primitives;
import com.zergatul.cheatutils.utils.Dimension;
import com.zergatul.cheatutils.utils.LevelChunkUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExplorationMiniMapChunkOverlay extends AbstractChunkOverlay {

    private static final ResourceLocation PlayerPosTexture = ResourceLocation.fromNamespaceAndPath(ModMain.MODID, "textures/mini-map-player.png");
    private static final ResourceLocation CenterPosTexture = ResourceLocation.fromNamespaceAndPath(ModMain.MODID, "textures/mini-map-center.png");
    private static final ResourceLocation MarkerTexture = ResourceLocation.fromNamespaceAndPath(ModMain.MODID, "textures/mini-map-marker.png");

    private final Map<Dimension, List<Marker>> markers = new HashMap<>();

    public ExplorationMiniMapChunkOverlay(int segmentSize, long updateDelay) {
        super(segmentSize, updateDelay);
    }

    public void addMarker() {
        if (mc.player != null) {
            addMarker(mc.player.getX(), mc.player.getZ());
        }
    }

    public void addMarker(double x, double z) {
        if (mc.player != null && mc.level != null) {
            Dimension dimension = Dimension.get(mc.level);
            List<Marker> list = markers.computeIfAbsent(dimension, d -> new ArrayList<>());
            Marker marker = new Marker(x, z);
            if (!list.contains(marker)) {
                list.add(marker);
            }
        }
    }

    public void clearMarkers() {
        if (mc.level != null) {
            Dimension dimension = Dimension.get(mc.level);
            markers.computeIfAbsent(dimension, d -> new ArrayList<>()).clear();
        }
    }

    @Override
    public int getTranslateZ() {
        return 100;
    }

    @Override
    public boolean isEnabled() {
        return getConfig().enabled;
    }

    @Override
    public void onPostDrawSegments(Dimension dimension, PoseStack poseStack, float xp, float zp, float xc, float zc, float multiplier) {
        final int ImageSize = 8;

        for (Marker marker : markers.computeIfAbsent(dimension, d -> new ArrayList<>())) {
            RenderSystem.setShaderTexture(0, MarkerTexture);
            Primitives.drawTexture(poseStack.last().pose(),
                    -ImageSize / 2f + ((float) marker.x - xc) * multiplier, -ImageSize / 2f + ((float) marker.z - zc) * multiplier,
                    ImageSize, ImageSize, getTranslateZ() + 2, 0, 0, ImageSize, ImageSize, ImageSize, ImageSize);
        }

        double distanceToPlayer = Math.sqrt((xp - xc) * (xp - xc) + (zp - zc) * (zp - zc));
        if (distanceToPlayer * multiplier > 2 * ImageSize) {
            RenderSystem.setShaderTexture(0, CenterPosTexture);
            Primitives.drawTexture(poseStack.last().pose(),
                    -ImageSize / 2f, -ImageSize / 2f,
                    ImageSize, ImageSize, getTranslateZ() + 3, 0, 0, ImageSize, ImageSize, ImageSize, ImageSize);
        }

        RenderSystem.setShaderTexture(0, PlayerPosTexture);
        Primitives.drawTexture(poseStack.last().pose(),
                -ImageSize / 2f + (xp - xc) * multiplier, -ImageSize / 2f + (zp - zc) * multiplier,
                ImageSize, ImageSize, getTranslateZ() + 4, 0, 0, ImageSize, ImageSize, ImageSize, ImageSize);
    }

    @Override
    protected void drawChunk(Dimension dimension, LevelChunk chunk, Map<SegmentPos, Segment> segments) {
        ChunkPos chunkPos = chunk.getPos();
        SegmentPos segmentPos = new SegmentPos(chunkPos, segmentSize);

        if (!segments.containsKey(segmentPos)) {
            segments.put(segmentPos, new Segment(segmentPos, segmentSize));
        }

        Segment segment = segments.get(segmentPos);
        int xf = Math.floorMod(chunkPos.x, segmentSize) * 16;
        int yf = Math.floorMod(chunkPos.z, segmentSize) * 16;

        Integer scanFromY = getConfig().scanFromY;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                drawPixel(dimension, xf, yf, dx, dz, segment, chunk, scanFromY);
            }
        }

        PreRenderGuiExecutor.instance.execute(segment::onChange);
    }

    @Override
    protected void processBlockChange(Dimension dimension, ChunkPos chunkPos, Segment segment, BlockPos pos, BlockState state) {
        if (mc.level == null || segment == null) {
            return;
        }

        if (dimension.isNether()) {
            int xf = Math.floorMod(chunkPos.x, segmentSize) * 16;
            int yf = Math.floorMod(chunkPos.z, segmentSize) * 16;
            LevelChunk chunk = mc.level.getChunk(chunkPos.x, chunkPos.z);
            boolean updated = drawPixel(dimension, xf, yf, Math.floorMod(pos.getX(), 16), Math.floorMod(pos.getZ(), 16), segment, chunk, getConfig().scanFromY);
            if (updated && !segment.updated) {
                segment.updated = true;
                segment.updateTime = System.nanoTime();
                addUpdatedSegment(segment);
            }
        } else {
            LevelChunk chunk = mc.level.getChunk(chunkPos.x, chunkPos.z);
            int dx = Math.floorMod(pos.getX(), 16);
            int dz = Math.floorMod(pos.getZ(), 16);
            int height = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, dx, dz);
            if (pos.getY() >= height) {
                int xf = Math.floorMod(chunkPos.x, segmentSize) * 16;
                int yf = Math.floorMod(chunkPos.z, segmentSize) * 16;
                boolean updated = drawPixel(dimension, xf, yf, dx, dz, segment, chunk, getConfig().scanFromY);
                if (updated && !segment.updated) {
                    segment.updated = true;
                    segment.updateTime = System.nanoTime();
                    addUpdatedSegment(segment);
                }
            }
        }
    }

    private boolean drawPixel(Dimension dimension, int xf, int yf, int dx, int dz, Segment segment, LevelChunk chunk, Integer scanFromY) {
        if (dimension.hasCeiling() || scanFromY != null) {
            for (int y1 = scanFromY != null ? scanFromY : dimension.getMinY() + dimension.getLogicalHeight() - 1; y1 >= dimension.getMinY(); y1--) {
                BlockState state = LevelChunkUtils.getBlockState(chunk, dx, y1, dz);
                if (state.isAir()) {
                    // first non-air block below
                    for (int y2 = y1 - 1; y2 >= dimension.getMinY(); y2--) {
                        state = LevelChunkUtils.getBlockState(chunk, dx, y2, dz);
                        if (!state.isAir()) {
                            MapColor materialColor = state.getBlock().defaultMapColor();
                            if (materialColor == MapColor.NONE) {
                                continue;
                            }
                            int color = convert(materialColor.col);
                            if (segment.image.getPixel(xf + dx, yf + dz) != color) {
                                segment.image.setPixel(xf + dx, yf + dz, color);
                                return true;
                            } else {
                                return false;
                            }
                        }
                    }
                    break;
                }
            }
        }

        int height = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, dx, dz);
        for (int y = height; y >= dimension.getMinY(); y--) {
            BlockState state = LevelChunkUtils.getBlockState(chunk, dx, y, dz);
            MapColor materialColor = state.getBlock().defaultMapColor();
            if (materialColor != MapColor.NONE) {
                int color = convert(materialColor.col);
                if (segment.image.getPixel(xf + dx, yf + dz) != color) {
                    segment.image.setPixel(xf + dx, yf + dz, color);
                    return true;
                } else {
                    return false;
                }
            }
        }

        return false;
    }

    private ExplorationMiniMapConfig getConfig() {
        return ConfigStore.instance.getConfig().explorationMiniMapConfig;
    }

    private static int convert(int color) {
        return 0xFF000000 | color;
    }

    public static class Marker {
        public int x;
        public int z;

        public Marker(double x, double z) {
            this.x = (int) Math.round(x);
            this.z = (int) Math.round(z);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Marker other) {
                return x == other.x && z == other.z;
            } else {
                return false;
            }
        }
    }
}