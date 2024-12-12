package com.zergatul.cheatutils.modules.esp;

import com.zergatul.cheatutils.collections.ImmutableList;
import com.zergatul.cheatutils.common.Events;
import com.zergatul.cheatutils.configs.BlockEspConfig;
import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.common.events.BlockUpdateEvent;
import com.zergatul.cheatutils.controllers.BlockEventsProcessor;
import com.zergatul.cheatutils.controllers.SnapshotChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BlockFinder {

    public static final BlockFinder instance = new BlockFinder();

    public final Map<BlockEspConfig, Set<BlockPos>> blocks = new ConcurrentHashMap<>();
    public final Set<BlockPos> blackList = ConcurrentHashMap.newKeySet();

    private BlockFinder() {
        Events.ChunkLoaded.add(this::onChunkLoaded);
        Events.ChunkUnloaded.add(this::onChunkUnloaded);
        Events.BlockUpdated.add(this::onBlockUpdated);
    }

    public void addConfig(BlockEspConfig config) {
        BlockEventsProcessor.instance.getExecutor().execute(() -> {
            blocks.put(config, ConcurrentHashMap.newKeySet());
            scan(config);
        });
    }

    public void removeConfig(BlockEspConfig config) {
        blocks.remove(config);
    }

    public void removeAllConfigs() {
        blocks.clear();
    }

    public void rescan() {
        BlockEventsProcessor.instance.getChunks().thenAcceptAsync(chunks -> {
            for (Set<BlockPos> set : blocks.values()) {
                set.clear();
            }
            for (SnapshotChunk chunk : chunks) {
                onChunkLoaded(chunk);
            }
        }, BlockEventsProcessor.instance.getExecutor());
    }

    public void addBlackList(BlockPos pos) {
        BlockFinder.instance.blackList.add(pos);
        for (Set<BlockPos> set : blocks.values()) {
            set.remove(pos);
        }
    }

    private void onChunkLoaded(SnapshotChunk chunk) {
        // need to call unload?
        Map<Block, BlockEspConfig> map = ConfigStore.instance.getConfig().blocks.getMap();
        int minY = chunk.getMinY();
        int xc = chunk.getPos().x << 4;
        int zc = chunk.getPos().z << 4;
        for (int x = 0; x < 16; x++) {
            int xw = xc | x;
            for (int z = 0; z < 16; z++) {
                int zw = zc | z;
                int height = minY + chunk.getHeight(x, z);
                for (int y = minY; y < height; y++) {
                    BlockState state = chunk.getBlockState(x, y, z);
                    checkBlock(xw, y, zw, state, map);
                }
            }
        }
    }

    private void onChunkUnloaded(ChunkPos pos) {
        final int cx = pos.x;
        final int cz = pos.z;
        for (Set<BlockPos> set : blocks.values()) {
            set.removeIf(p -> (p.getX() >> 4) == cx && (p.getZ() >> 4) == cz);
        }
    }

    private void onBlockUpdated(BlockUpdateEvent event) {
        BlockPos pos = event.pos();
        for (Set<BlockPos> set : blocks.values()) {
            set.remove(pos);
        }
        checkBlock(pos.getX(), pos.getY(), pos.getZ(), event.state(), ConfigStore.instance.getConfig().blocks.getMap());
    }

    private void scan(final BlockEspConfig config) {
        BlockEventsProcessor.instance.getChunks().thenAcceptAsync(chunks -> {
            for (SnapshotChunk chunk : chunks) {
                scanChunkForBlock(chunk, config);
            }
        }, BlockEventsProcessor.instance.getExecutor());
    }

    private void scanChunkForBlock(SnapshotChunk chunk, BlockEspConfig config) {
        Set<BlockPos> set = blocks.get(config);
        ImmutableList<Block> blockTypes = config.blocks;
        int minY = chunk.getMinY();
        int xc = chunk.getPos().x << 4;
        int zc = chunk.getPos().z << 4;
        for (int x = 0; x < 16; x++) {
            int xw = xc | x;
            for (int z = 0; z < 16; z++) {
                int zw = zc | z;
                int height = chunk.getHeight(x, z) + minY;
                for (int y = minY; y < height; y++) {
                    BlockState state = chunk.getBlockState(x, y, z);
                    Block block = state.getBlock();
                    for (int i = 0; i < blockTypes.size(); i++) {
                        if (block == blockTypes.get(i)) {
                            set.add(new BlockPos(xw, y, zw));
                        }
                    }
                }
            }
        }
    }

    private void checkBlock(int x, int y, int z, BlockState state, Map<Block, BlockEspConfig> map) {
        if (state.isAir()) {
            return;
        }

        BlockEspConfig config = map.get(state.getBlock());
        if (config != null) {
            Set<BlockPos> set = blocks.get(config);
            if (set != null) {
                BlockPos pos = new BlockPos(x, y, z);
                if (!blackList.contains(pos)) {
                    set.add(pos);
                }
            }
        }
    }
}