package com.zergatul.cheatutils.controllers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.serialization.Codec;
import com.zergatul.cheatutils.chunkoverlays.WorldDownloadChunkOverlay;
import com.zergatul.cheatutils.mixins.common.accessors.SerializableChunkDataAccessor;
import com.zergatul.cheatutils.utils.Dimension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.storage.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WorldDownloadController {

    public static final WorldDownloadController instance = new WorldDownloadController();

    private final Minecraft mc = Minecraft.getInstance();
    private final Logger logger = LogManager.getLogger(WorldDownloadController.class);
    private Map<ResourceKey<Level>, ChunkStorage> chunkStorages;
    private LevelStorageSource.LevelStorageAccess access;

    public WorldDownloadController() {}

    public boolean isActive() {
        return chunkStorages != null;
    }

    public void start(String name) {
        final Object syncObject = new Object();
        RenderSystem.recordRenderCall(() -> {
            try {
                stopInternal();

                File file = new File("./saves/" + name + "/level.dat");
                if (!file.exists()) {
                    throw new IllegalStateException("World [" + name + "] doesn't exist in [saves] directory.");
                }

                access = mc.getLevelSource().createAccess(name);
                chunkStorages = new HashMap<>();
                ChunkOverlayController.instance.ofType(WorldDownloadChunkOverlay.class).onEnabledChanged();
            } catch (Throwable e) {
                logger.error("Cannot start World Download", e);
                stopInternal();
            } finally {
                synchronized (syncObject) {
                    syncObject.notify();
                }
            }
        });

        try {
            synchronized (syncObject) {
                syncObject.wait();
            }
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    public void stop() {
        final Object syncObject = new Object();
        RenderSystem.recordRenderCall(() -> {
            try {
                stopInternal();
            } finally {
                synchronized (syncObject) {
                    syncObject.notify();
                }
            }
        });

        try {
            synchronized (syncObject) {
                syncObject.wait();
            }
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    public void onChunkFilledFromPacket(LevelChunk chunk) {
        if (isActive()) {
            processChunk(chunk);
        }
    }

    private void stopInternal() {
        try {
            if (chunkStorages != null) {
                for (ChunkStorage storage : chunkStorages.values()) {
                    try {
                        storage.flushWorker();
                        storage.close();
                    } catch (IOException e) {
                        logger.error("Cannot save ChunkStorage", e);
                    }
                }
            }

            if (mc.player != null && access != null) {
                PlayerDataStorage playerDataStorage = access.createPlayerStorage();
                playerDataStorage.save(mc.player);
            }

            chunkStorages = null;

            closeAccess();

            ChunkOverlayController.instance.ofType(WorldDownloadChunkOverlay.class).onEnabledChanged();
        } catch (Throwable e) {
            logger.error("Cannot stop World Download", e);
            stop();
        }
    }

    private void processChunk(LevelChunk chunk) {
        try {
            ClientLevel level = (ClientLevel) chunk.getLevel();
            Dimension dimension = Dimension.get(level);
            ResourceKey<Level> levelDimension = level.dimension();
            ChunkStorage storage;
            if (chunkStorages.containsKey(levelDimension)) {
                storage = chunkStorages.get(levelDimension);
            } else {
                storage = new ChunkStorage(
                        new RegionStorageInfo(access.getLevelId(), levelDimension, "chunk"),
                        access.getDimensionPath(levelDimension).resolve("region"),
                        null, // data fixer
                        true); // sync
                chunkStorages.put(levelDimension, storage);
            }

            CompoundTag compoundtag = write(level, chunk);
            storage.write(chunk.getPos(), () -> compoundtag);

            ChunkOverlayController.instance.ofType(WorldDownloadChunkOverlay.class).notifyChunkSaved(
                    dimension, chunk.getPos().x, chunk.getPos().z);
        } catch (Throwable e) {
            logger.error("Cannot save chunk", e);
        }
    }

    // copy from SerializableChunkData.write(level, chunk)
    private CompoundTag write(ClientLevel level, ChunkAccess chunk) {
        CompoundTag chunkNbt = NbtUtils.addCurrentDataVersion(new CompoundTag());
        chunkNbt.putInt("xPos", chunk.getPos().x);
        chunkNbt.putInt("yPos", chunk.getMinSectionY());
        chunkNbt.putInt("zPos", chunk.getPos().z);
        chunkNbt.putLong("LastUpdate", level.getGameTime());
        chunkNbt.putLong("InhabitedTime", chunk.getInhabitedTime());
        chunkNbt.putString("Status", BuiltInRegistries.CHUNK_STATUS.getKey(chunk.getPersistedStatus()).toString());

        Registry<Biome> registry = level.registryAccess().lookupOrThrow(Registries.BIOME);
        Codec<PalettedContainerRO<Holder<Biome>>> codec = makeBiomeCodec(registry);

        ListTag sectionsNbt = new ListTag();
        for (int i = 0; i < chunk.getSectionsCount(); i++) {
            LevelChunkSection section = chunk.getSection(i);
            CompoundTag sectionNbt = new CompoundTag();
            sectionNbt.put("block_states", SerializableChunkDataAccessor.getBlockStateCodec_CU().encodeStart(NbtOps.INSTANCE, section.getStates()).getOrThrow());
            sectionNbt.put("biomes", codec.encodeStart(NbtOps.INSTANCE, section.getBiomes()).getOrThrow());
            sectionNbt.putByte("Y", (byte)(i + chunk.getMinSectionY()));
            sectionsNbt.add(sectionNbt);
        }

        chunkNbt.put("sections", sectionsNbt);

        ListTag blockEntitiesNbt = new ListTag();
        for (BlockPos blockpos : chunk.getBlockEntitiesPos()) {
            CompoundTag blockEntityNbt = chunk.getBlockEntityNbtForSaving(blockpos, level.registryAccess());
            if (blockEntityNbt != null) {
                blockEntitiesNbt.add(blockEntityNbt);
            }
        }
        chunkNbt.put("block_entities", blockEntitiesNbt);

        return chunkNbt;
    }

    private Codec<PalettedContainerRO<Holder<Biome>>> makeBiomeCodec(Registry<Biome> registry) {
        return PalettedContainer.codecRO(registry.asHolderIdMap(), registry.holderByNameCodec(), PalettedContainer.Strategy.SECTION_BIOMES, registry.getOrThrow(Biomes.PLAINS));
    }

    private void closeAccess() {
        if (access != null) {
            try {
                access.close();
            }
            catch (Throwable e) {
                logger.error("Cannot close LevelStorageAccess", e);
            }
            access = null;
        }
    }
}