package com.zergatul.cheatutils.controllers;

import com.zergatul.cheatutils.mixins.common.accessors.ChunkAccessAccessor;
import com.zergatul.cheatutils.mixins.common.accessors.HeightmapAccessor;
import com.zergatul.cheatutils.utils.Dimension;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.BitStorage;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;

public class SnapshotChunk {

    private final Dimension dimension;
    private final ChunkPos pos;
    private final LevelHeightAccessor levelHeightAccessor;
    private final BitStorage heightmap;
    private final LevelChunkSection[] sections;

    private SnapshotChunk(Dimension dimension, ChunkPos pos, LevelHeightAccessor levelHeightAccessor, BitStorage heightmap, LevelChunkSection[] sections) {
        this.dimension = dimension;
        this.pos = pos;
        this.levelHeightAccessor = levelHeightAccessor;
        this.heightmap = heightmap;
        this.sections = sections;
    }

    public int getMinY() {
        return levelHeightAccessor.getMinY();
    }

    public ChunkPos getPos() {
        return pos;
    }

    public int getHeight(int x, int z) {
        return heightmap.get(x | (z << 4));
    }

    public BlockState getBlockState(int x, int y, int z) {
        int index = (y - levelHeightAccessor.getMinY()) >> 4;
        return sections[index].getBlockState(x, y & 0x0F, z);
    }

    public Dimension getDimension() {
        return dimension;
    }

    public static SnapshotChunk from(LevelChunk chunk) {
        // we just copy references for all objects since they are immutable
        // except LevelChunkSection.states
        // and heightmap
        ChunkAccessAccessor accessor = (ChunkAccessAccessor) chunk;
        LevelHeightAccessor levelHeightAccessor = accessor.getLevelHeightAccessor_CU();
        Heightmap surfaceHeightmap = accessor.getHeightmaps_CU().get(Heightmap.Types.WORLD_SURFACE);
        BitStorage heightmap = ((HeightmapAccessor) surfaceHeightmap).getData_CU().copy();
        LevelChunkSection[] sections = copySections(chunk.getSections());
        Dimension dimension = Dimension.get((ClientLevel) chunk.getLevel());
        return new SnapshotChunk(dimension, chunk.getPos(), levelHeightAccessor, heightmap, sections);
    }

    private static LevelChunkSection[] copySections(LevelChunkSection[] source) {
        LevelChunkSection[] destination = new LevelChunkSection[source.length];
        for (int i = 0; i < source.length; i++) {
            LevelChunkSection section = source[i];
            destination[i] = new LevelChunkSection(section.getStates().copy(), section.getBiomes());
        }
        return destination;
    }
}