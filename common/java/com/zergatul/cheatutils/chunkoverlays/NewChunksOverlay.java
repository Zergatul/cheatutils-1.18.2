package com.zergatul.cheatutils.chunkoverlays;

import com.mojang.blaze3d.platform.NativeImage;
import com.zergatul.cheatutils.ModMain;
import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.utils.Dimension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NewChunksOverlay extends AbstractChunkOverlay {

    private final Map<Dimension, Map<ChunkPos, ChunkEntry>> dimensions = new ConcurrentHashMap<>();
    private final NativeImage oldChunkImage;
    private final NativeImage newChunkImage;
    private final NativeImage semiNewChunkImage;

    public NewChunksOverlay(int segmentSize, long updateDelay) {
        super(segmentSize, updateDelay);

        oldChunkImage = loadImage("textures/newchunks/old.png");
        newChunkImage = loadImage("textures/newchunks/new.png");
        semiNewChunkImage = loadImage("textures/newchunks/seminew.png");
    }

    @Override
    public int getTranslateZ() {
        return 101;
    }

    @Override
    public boolean isEnabled() {
        return ConfigStore.instance.getConfig().newChunksConfig.enabled;
    }

    @Override
    protected void drawChunk(Dimension dimension, LevelChunk chunk, Map<SegmentPos, Segment> segments) {
        ChunkPos chunkPos = chunk.getPos();
        Map<ChunkPos, ChunkEntry> chunks = dimensions.computeIfAbsent(dimension, k -> new ConcurrentHashMap<>());
        ChunkEntry entry = chunks.computeIfAbsent(chunkPos, p -> new ChunkEntry());

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) {
            pos.setX(x);
            for (int z = 0; z < 16; z++) {
                pos.setZ(z);
                int height = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                for (int y = dimension.getMinY(); y <= height; y++) {
                    pos.setY(y);
                    BlockState blockState = chunk.getBlockState(pos);
                    if (isFlowingLiquid(blockState)) {
                        entry.addExistingFlow(x, y - dimension.getMinY(), z);
                    }
                }
            }
        }

        SegmentPos segmentPos = new SegmentPos(chunkPos, segmentSize);
        if (!segments.containsKey(segmentPos)) {
            segments.put(segmentPos, new Segment(segmentPos, segmentSize));
        }

        Segment segment = segments.get(segmentPos);
        int xf = Math.floorMod(chunkPos.x, segmentSize) * 16;
        int yf = Math.floorMod(chunkPos.z, segmentSize) * 16;
        redrawChunk(entry, segment, xf, yf);
        segment.onChange();
    }

    @Override
    protected void processBlockChange(Dimension dimension, ChunkPos chunkPos, Segment segment, BlockPos pos, BlockState state) {
        if (!isFlowingLiquid(state)) {
            return;
        }

        Map<ChunkPos, ChunkEntry> chunks = dimensions.computeIfAbsent(dimension, d -> new ConcurrentHashMap<>());
        ChunkEntry entry = chunks.computeIfAbsent(chunkPos, p -> new ChunkEntry());
        entry.addNewFlow(pos.getX() & 15, pos.getY() - dimension.getMinY(), pos.getZ() & 15);

        if (segment == null) {
            return;
        }

        int xf = Math.floorMod(chunkPos.x, segmentSize) * 16;
        int yf = Math.floorMod(chunkPos.z, segmentSize) * 16;
        redrawChunk(entry, segment, xf, yf);

        if (!segment.updated) {
            segment.updated = true;
            segment.updateTime = System.nanoTime();
            addUpdatedSegment(segment);
        }
    }

    private void redrawChunk(ChunkEntry chunk, Segment segment, int xf, int yf) {
        // clear pixels
        for (int dx = 0; dx < 16; dx++) {
            for (int dy = 0; dy < 16; dy++) {
                segment.image.setPixel(xf + dx, yf + dy, 0);
            }
        }

        int existingFlows = chunk.getExistingFlows();
        int newFlows = chunk.getNewFlows();


        if (existingFlows == 0 && newFlows == 0) {
            return;
        }

        NativeImage image;
        if (existingFlows == 0) {
            image = newChunkImage;
        } else {
            if (newFlows == 0) {
                image = oldChunkImage;
            } else {
                image = semiNewChunkImage;
            }
        }

        for (int dx = 0; dx < 16; dx++) {
            for (int dy = 0; dy < 16; dy++) {
                segment.image.setPixel(xf + dx, yf + dy, image.getPixel(dx, dy));
            }
        }
    }

    private boolean isFlowingLiquid(BlockState blockState) {
        FluidState fluidState = blockState.getFluidState();
        return !fluidState.isEmpty() && !fluidState.isSource();
    }

    private static class ChunkEntry {
        private final Set<Integer> existingFlows = new HashSet<>();
        private final Set<Integer> newFlows = new HashSet<>();

        public void addExistingFlow(int x, int y, int z) {
            int value = combine(x, y, z);
            if (!newFlows.contains(value)) {
                existingFlows.add(value);
            }
        }

        public void addNewFlow(int x, int y, int z) {
            int value = combine(x, y, z);
            if (!existingFlows.contains(value)) {
                newFlows.add(value);
            }
        }

        public int getExistingFlows() {
            return existingFlows.size();
        }

        public int getNewFlows() {
            return newFlows.size();
        }

        private int combine(int x, int y, int z) {
            return (x | (z << 4)) | (y << 8);
        }

        // TODO: temp
        /*public Stream<BlockPos> getNewBlocks(ChunkPos chunk) {
            return newFlows.stream().map(i -> new BlockPos(chunk.x * 16 + (i & 15), (i >> 8), chunk.z * 16 + ((i >> 4) & 15))).toList().stream();
        }*/

        // TODO: temp
        /*public Stream<BlockPos> getOldBlocks(ChunkPos chunk) {
            return existingFlows.stream().map(i -> new BlockPos(chunk.x * 16 + (i & 15), (i >> 8), chunk.z * 16 + ((i >> 4) & 15))).toList().stream();
        }*/
    }
}