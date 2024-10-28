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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
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
        CompoundTag compoundtag = NbtUtils.addCurrentDataVersion(new CompoundTag());
        /*compoundtag.putInt("xPos", chunk.getPos().x);
        compoundtag.putInt("yPos", chunk.getMinSectionY());
        compoundtag.putInt("zPos", chunk.getPos().z);
        compoundtag.putLong("LastUpdate", level.getGameTime());
        compoundtag.putLong("InhabitedTime", chunk.getInhabitedTime());
        compoundtag.putString("Status", BuiltInRegistries.CHUNK_STATUS.getKey(chunk.getPersistedStatus()).toString());

        ListTag listtag = new ListTag();

        Registry<Biome> registry = level.registryAccess().lookupOrThrow(Registries.BIOME);
        Codec<PalettedContainerRO<Holder<Biome>>> codec = makeBiomeCodec(registry);

        LevelLightEngine levellightengine = level.getChunkSource().getLightEngine();
        for (int i = levellightengine.getMinLightSection(); i < levellightengine.getMaxLightSection(); i++) {
            CompoundTag compoundtag1 = new CompoundTag();
            if (section != null) {
                compoundtag1.put("block_states", SerializableChunkDataAccessor.getBlockStateCodec_CU().encodeStart(NbtOps.INSTANCE, section.getStates()).getOrThrow());
                compoundtag1.put("biomes", codec.encodeStart(NbtOps.INSTANCE, section.getBiomes()).getOrThrow());
            }

            if (!compoundtag1.isEmpty()) {
                compoundtag1.putByte("Y", (byte)section.y);
                listtag.add(compoundtag1);
            }
        }

        compoundtag.put("sections", listtag);
        if (this.lightCorrect) {
            compoundtag.putBoolean("isLightOn", true);
        }

        ListTag listtag1 = new ListTag();
        listtag1.addAll(this.blockEntities);
        compoundtag.put("block_entities", listtag1);
        if (this.chunkStatus.getChunkType() == ChunkType.PROTOCHUNK) {
            ListTag listtag2 = new ListTag();
            listtag2.addAll(this.entities);
            compoundtag.put("entities", listtag2);
            if (this.carvingMask != null) {
                compoundtag.putLongArray("carving_mask", this.carvingMask);
            }
        }

        saveTicks(compoundtag, this.packedTicks);
        compoundtag.put("PostProcessing", packOffsets(this.postProcessingSections));
        CompoundTag compoundtag2 = new CompoundTag();
        this.heightmaps.forEach((p_369025_, p_369618_) -> compoundtag2.put(p_369025_.getSerializationKey(), new LongArrayTag(p_369618_)));
        compoundtag.put("Heightmaps", compoundtag2);
        compoundtag.put("structures", this.structureData);*/
        return compoundtag;

        /*

        LevelChunkSection[] alevelchunksection = chunk.getSections();
        ListTag listtag = new ListTag();
        LevelLightEngine levellightengine = level.getChunkSource().getLightEngine();
        Registry<Biome> registry = level.registryAccess().registryOrThrow(Registries.BIOME);
        Codec<PalettedContainerRO<Holder<Biome>>> codec = makeBiomeCodec(registry);
        boolean flag = chunk.isLightCorrect();

        for (int i = levellightengine.getMinLightSection(); i < levellightengine.getMaxLightSection(); i++) {
            int j = chunk.getSectionIndexFromSectionY(i);
            boolean flag1 = j >= 0 && j < alevelchunksection.length;
            DataLayer datalayer = levellightengine.getLayerListener(LightLayer.BLOCK).getDataLayerData(SectionPos.of(chunkpos, i));
            DataLayer datalayer1 = levellightengine.getLayerListener(LightLayer.SKY).getDataLayerData(SectionPos.of(chunkpos, i));
            if (flag1 || datalayer != null || datalayer1 != null) {
                CompoundTag compoundtag1 = new CompoundTag();
                if (flag1) {
                    LevelChunkSection levelchunksection = alevelchunksection[j];
                    compoundtag1.put("block_states", ChunkSerializerAccessor.getBlockStateCodec_CU().encodeStart(NbtOps.INSTANCE, levelchunksection.getStates()).getOrThrow());
                    compoundtag1.put("biomes", codec.encodeStart(NbtOps.INSTANCE, levelchunksection.getBiomes()).getOrThrow());
                }

                if (datalayer != null && !datalayer.isEmpty()) {
                    compoundtag1.putByteArray("BlockLight", datalayer.getData());
                }

                if (datalayer1 != null && !datalayer1.isEmpty()) {
                    compoundtag1.putByteArray("SkyLight", datalayer1.getData());
                }

                if (!compoundtag1.isEmpty()) {
                    compoundtag1.putByte("Y", (byte)i);
                    listtag.add(compoundtag1);
                }
            }
        }

        compoundtag.put("sections", listtag);
        if (flag) {
            compoundtag.putBoolean("isLightOn", true);
        }

        ListTag listtag1 = new ListTag();

        for (BlockPos blockpos : chunk.getBlockEntitiesPos()) {
            CompoundTag compoundtag3 = chunk.getBlockEntityNbtForSaving(blockpos, level.registryAccess());
            if (compoundtag3 != null) {
                listtag1.add(compoundtag3);
            }
        }

        compoundtag.put("block_entities", listtag1);

        saveTicks(level, compoundtag, chunk.getTicksForSerialization());
        compoundtag.put("PostProcessing", ChunkSerializer.packOffsets(chunk.getPostProcessing()));
        CompoundTag compoundtag2 = new CompoundTag();

        for (Map.Entry<Heightmap.Types, Heightmap> entry : chunk.getHeightmaps()) {
            if (chunk.getPersistedStatus().heightmapsAfter().contains(entry.getKey())) {
                compoundtag2.put(entry.getKey().getSerializationKey(), new LongArrayTag(entry.getValue().getRawData()));
            }
        }

        compoundtag.put("Heightmaps", compoundtag2);
        return compoundtag;*/
    }

    /*private Codec<PalettedContainerRO<Holder<Biome>>> makeBiomeCodec(Registry<Biome> p_188261_) {
        return PalettedContainer.codecRO(p_188261_.asHolderIdMap(), p_188261_.holderByNameCodec(), PalettedContainer.Strategy.SECTION_BIOMES, p_188261_.getHolderOrThrow(Biomes.PLAINS));
    }*/

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

    /*private static void saveTicks(ClientLevel p_188236_, CompoundTag p_188237_, ChunkAccess.TicksToSave p_188238_) {
        long i = p_188236_.getLevelData().getGameTime();
        p_188237_.put("block_ticks", p_188238_.blocks().save(i, p_258987_ -> BuiltInRegistries.BLOCK.getKey(p_258987_).toString()));
        p_188237_.put("fluid_ticks", p_188238_.fluids().save(i, p_258989_ -> BuiltInRegistries.FLUID.getKey(p_258989_).toString()));
    }*/
}