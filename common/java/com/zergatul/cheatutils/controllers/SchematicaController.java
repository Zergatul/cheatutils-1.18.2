package com.zergatul.cheatutils.controllers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.zergatul.cheatutils.common.Events;
import com.zergatul.cheatutils.common.events.RenderWorldLayerEvent;
import com.zergatul.cheatutils.concurrent.TickEndExecutor;
import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.configs.SchematicaConfig;
import com.zergatul.cheatutils.render.Primitives;
import com.zergatul.cheatutils.render.RenderHelper;
import com.zergatul.cheatutils.schematics.PlacingConverter;
import com.zergatul.cheatutils.schematics.PlacingSettings;
import com.zergatul.cheatutils.schematics.SchemaFile;
import com.zergatul.cheatutils.utils.*;
import com.zergatul.cheatutils.common.events.BlockUpdateEvent;
import com.zergatul.cheatutils.common.events.RenderWorldLastEvent;
import com.zergatul.cheatutils.wrappers.BakedModelWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SchematicaController {

    public static final SchematicaController instance = new SchematicaController();

    private final Minecraft mc = Minecraft.getInstance();
    private final List<Entry> entries = new ArrayList<>();
    private final RandomSource random = new JavaRandom(0);
    private final SlotSelector slotSelector = new SlotSelector();

    private SchematicaController() {
        Events.RawChunkUnloaded.add(this::onChunkLoaded);
        Events.RawBlockUpdated.add(this::onBlockUpdated);
        Events.ClientTickEnd.add(this::onClientTickEnd);
        Events.RenderSolidLayer.add(this::onRenderSolidLayer);
        Events.AfterRenderWorld.add(this::onRender);
    }

    public synchronized void clear() {
        entries.clear();
    }

    public synchronized void place(SchemaFile file, PlacingSettings placing) {
        TickEndExecutor.instance.execute(() -> {
            final Entry entry = new Entry(file, placing);
            entries.add(entry);

            AtomicReferenceArray<LevelChunk> chunks = BlockEventsProcessor.instance.getRawChunks();
            for (int i = 0; i < chunks.length(); i++) {
                LevelChunk chunk = chunks.get(i);
                if (chunk != null) {
                    entry.onChunkLoaded(chunk);
                }
            }
        });
    }

    private synchronized void onClientTickEnd() {
        SchematicaConfig config = ConfigStore.instance.getConfig().schematicaConfig;
        if (!config.enabled || !config.autoBuild) {
            return;
        }

        if (mc.level == null || mc.player == null) {
            return;
        }

        Vec3 eyePos = mc.player.getEyePosition();
        ItemStack itemInHand = mc.player.getMainHandItem();

        Block blockInHand;
        if (itemInHand.getItem() instanceof BlockItem blockItem) {
            blockInHand = blockItem.getBlock();
        } else {
            blockInHand = null;
        }

        BlockUtils.PlaceBlockPlan plan = null;
        BlockState state = null;
        for (BlockPos pos : NearbyBlockEnumerator.getPositions(eyePos, config.maxRange)) {
            for (Entry entry : entries) {
                state = entry.getBlockState(pos.getX(), pos.getY(), pos.getZ());
                if (state.isAir()) {
                    continue;
                }

                plan = BlockUtils.getPlacingPlan(pos, config.attachToAir);
                if (plan != null) {
                    break;
                }
            }

            if (plan != null) {
                break;
            }
        }

        if (plan == null) {
            return;
        }

        int slot = slotSelector.selectBlock(config, state.getBlock());
        if (slot >= 0)  {
            mc.player.getInventory().selected = slot;
            blockInHand = state.getBlock();
        }
        if (blockInHand == state.getBlock()) {
            BlockUtils.applyPlacingPlan(plan, config.useShift);
        }
    }

    private synchronized void onRenderSolidLayer(RenderWorldLayerEvent event) {
        SchematicaConfig config = ConfigStore.instance.getConfig().schematicaConfig;
        if (!config.enabled) {
            return;
        }

        if (mc.level == null) {
            return;
        }

        Vec3 view = event.getCamera().getPosition();

        if (config.showMissingBlockGhosts) {
            /*RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.enableBlend();*/
            RenderSystem.setShaderColor(1.0f, 0.5f, 0.5f, 1f);

            Map<BlockPos, BlockState> ghosts = new HashMap<>();
            for (Entry entry : entries) {
                entry.forEachMissingState(view, config.missingBlockGhostsMaxDistance, ghosts::put);
            }

            BlockPos.MutableBlockPos neighPos = new BlockPos.MutableBlockPos();
            for (var mapEntry : ghosts.entrySet()) {
                BlockPos pos = mapEntry.getKey();
                BlockState state = mapEntry.getValue();
                BakedModel model = mc.getBlockRenderer().getBlockModel(state);
                for (var direction : Direction.values()) {
                    neighPos.setX(pos.getX() + direction.getStepX());
                    neighPos.setY(pos.getY() + direction.getStepY());
                    neighPos.setZ(pos.getZ() + direction.getStepZ());
                    if (!ghosts.containsKey(neighPos)) {
                        List<BakedQuad> quads = BakedModelWrapper.getQuads(model, state, direction, random);
                        if (!quads.isEmpty()) {
                            BakedQuad quad = quads.get(0);
                            TextureAtlasSprite sprite = quad.getSprite();
                            BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                            RenderSystem.setShaderTexture(0, sprite.atlasLocation());

                            FaceInfo face = FaceInfo.fromFacing(direction);
                            FaceInfo.VertexInfo info;

                            info = face.getVertexInfo(0);
                            bufferBuilder.addVertex(
                                            (float) ((info.xFace == FaceInfo.Constants.MIN_X ? pos.getX() : pos.getX() + 1) - view.x),
                                            (float) ((info.yFace == FaceInfo.Constants.MIN_Y ? pos.getY() : pos.getY() + 1) - view.y),
                                            (float) ((info.zFace == FaceInfo.Constants.MIN_Z ? pos.getZ() : pos.getZ() + 1) - view.z))
                                    .setUv(sprite.getU0(), sprite.getV0());

                            info = face.getVertexInfo(1);
                            bufferBuilder.addVertex(
                                            (float) ((info.xFace == FaceInfo.Constants.MIN_X ? pos.getX() : pos.getX() + 1) - view.x),
                                            (float) ((info.yFace == FaceInfo.Constants.MIN_Y ? pos.getY() : pos.getY() + 1) - view.y),
                                            (float) ((info.zFace == FaceInfo.Constants.MIN_Z ? pos.getZ() : pos.getZ() + 1) - view.z))
                                    .setUv(sprite.getU0(), sprite.getV1());

                            info = face.getVertexInfo(2);
                            bufferBuilder.addVertex(
                                            (float) ((info.xFace == FaceInfo.Constants.MIN_X ? pos.getX() : pos.getX() + 1) - view.x),
                                            (float) ((info.yFace == FaceInfo.Constants.MIN_Y ? pos.getY() : pos.getY() + 1) - view.y),
                                            (float) ((info.zFace == FaceInfo.Constants.MIN_Z ? pos.getZ() : pos.getZ() + 1) - view.z))
                                    .setUv(sprite.getU1(), sprite.getV1());

                            info = face.getVertexInfo(3);
                            bufferBuilder.addVertex(
                                            (float) ((info.xFace == FaceInfo.Constants.MIN_X ? pos.getX() : pos.getX() + 1) - view.x),
                                            (float) ((info.yFace == FaceInfo.Constants.MIN_Y ? pos.getY() : pos.getY() + 1) - view.y),
                                            (float) ((info.zFace == FaceInfo.Constants.MIN_Z ? pos.getZ() : pos.getZ() + 1) - view.z))
                                    .setUv(sprite.getU1(), sprite.getV0());

                            RenderHelper.drawBuffer(SharedVertexBuffer.instance, bufferBuilder, event.pose(), event.getProjection(), CoreShaders.POSITION_TEX);
                        }
                    }
                }
            }

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private synchronized void onRender(RenderWorldLastEvent event) {
        SchematicaConfig config = ConfigStore.instance.getConfig().schematicaConfig;
        if (!config.enabled) {
            return;
        }

        if (mc.level == null) {
            return;
        }

        Vec3 view = event.getCamera().getPosition();

        if (config.showMissingBlockTracers) {
            Vec3 tracerCenter = event.getTracerCenter();
            double tracerX = tracerCenter.x;
            double tracerY = tracerCenter.y;
            double tracerZ = tracerCenter.z;

            BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
            RenderSystem.setShaderColor(0.2f, 1.0f, 0.2f, 0.8f);

            for (Entry entry : entries) {
                entry.forEachMissing(view, config.missingBlockTracersMaxDistance, pos -> {
                    bufferBuilder.addVertex((float) (tracerX - view.x), (float) (tracerY - view.y), (float) (tracerZ - view.z))
                            .setColor(1f, 1f, 1f, 1f);
                    bufferBuilder.addVertex((float) (pos.getX() + 0.5 - view.x), (float) (pos.getY() + 0.5 - view.y), (float) (pos.getZ() + 0.5 - view.z))
                            .setColor(1f, 1f, 1f, 1f);
                });
            }

            Primitives.renderLines(bufferBuilder, event.getPose(), event.getProjection());
        }

        if (config.showMissingBlockCubes) {
            BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
            RenderSystem.setShaderColor(0.2f, 1.0f, 0.2f, 0.8f);

            for (Entry entry : entries) {
                entry.forEachMissing(view, config.missingBlockCubesMaxDistance, pos -> {
                    double x1 = pos.getX() + 0.25 - view.x;
                    double y1 = pos.getY() + 0.25 - view.y;
                    double z1 = pos.getZ() + 0.25 - view.z;
                    double x2 = x1 + 0.5;
                    double y2 = y1 + 0.5;
                    double z2 = z1 + 0.5;
                    Primitives.drawCube(bufferBuilder, x1, y1, z1, x2, y2, z2);
                });
            }

            Primitives.renderLines(bufferBuilder, event.getPose(), event.getProjection());
        }

        if (config.showWrongBlockTracers) {
            Vec3 tracerCenter = event.getTracerCenter();
            double tracerX = tracerCenter.x;
            double tracerY = tracerCenter.y;
            double tracerZ = tracerCenter.z;

            BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
            RenderSystem.setShaderColor(1.0f, 0.5f, 0.5f, 0.6f);

            for (Entry entry : entries) {
                entry.forEachWrong(view, config.wrongBlockTracersMaxDistance, pos -> {
                    bufferBuilder.addVertex((float) (tracerX - view.x), (float) (tracerY - view.y), (float) (tracerZ - view.z))
                            .setColor(1f, 1f, 1f, 1f);
                    bufferBuilder.addVertex((float) (pos.getX() + 0.5 - view.x), (float) (pos.getY() + 0.5 - view.y), (float) (pos.getZ() + 0.5 - view.z))
                            .setColor(1f, 1f, 1f, 1f);
                });
            }

            Primitives.renderLines(bufferBuilder, event.getPose(), event.getProjection());
        }

        if (config.showWrongBlockCubes) {
            BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
            RenderSystem.setShaderColor(1.0f, 0.5f, 0.5f, 0.6f);

            for (Entry entry : entries) {
                entry.forEachWrong(view, config.wrongBlockCubesMaxDistance, pos -> {
                    double x1 = pos.getX() + 0.25 - view.x;
                    double y1 = pos.getY() + 0.25 - view.y;
                    double z1 = pos.getZ() + 0.25 - view.z;
                    double x2 = x1 + 0.5;
                    double y2 = y1 + 0.5;
                    double z2 = z1 + 0.5;
                    Primitives.drawCube(bufferBuilder, x1, y1, z1, x2, y2, z2);
                });
            }

            Primitives.renderLines(bufferBuilder, event.getPose(), event.getProjection());
        }

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private synchronized void onChunkLoaded(LevelChunk chunk) {
        for (Entry entry : entries) {
            entry.onChunkLoaded(chunk);
        }
    }

    private synchronized void onBlockUpdated(BlockUpdateEvent event) {
        for (Entry entry : entries) {
            entry.onBlockUpdated(event);
        }
    }

    private static class Entry {

        public final int x1, x2, y1, y2, z1, z2;
        public final Map<Long, Chunk> chunks;

        public Entry(SchemaFile file, PlacingSettings placing) {
            PlacingConverter converter = new PlacingConverter(placing, file.getWidth(), file.getHeight(), file.getLength());

            x1 = placing.x;
            x2 = x1 + converter.getWidth();
            y1 = placing.y;
            y2 = y1 + converter.getHeight();
            z1 = placing.z;
            z2 = z1 + converter.getLength();

            chunks = new HashMap<>();
            for (int x = 0; x < file.getWidth(); x++) {
                for (int y = 0; y < file.getHeight(); y++) {
                    for (int z = 0; z < file.getLength(); z++) {
                        BlockState state = file.getBlockState(x, y, z);
                        if (!state.isAir()) {
                            PlacingConverter.Vec3iMutable vec = converter.convert(x, y, z);
                            int wx = x1 + vec.x;
                            int wy = y1 + vec.y;
                            int wz = z1 + vec.z;
                            long chunkIndex = blockToChunkIndex(wx, wz);
                            Chunk chunk = chunks.get(chunkIndex);
                            if (chunk == null) {
                                chunk = new Chunk(wx & 0xFFFFFFF0, wz & 0xFFFFFFF0);
                                chunks.put(chunkIndex, chunk);
                            }
                            chunk.setBlockState(wx & 0x0F, wy, wz & 0x0F, state);
                        }
                    }
                }
            }
        }

        public void forEachMissing(Vec3 view, double distance, Consumer<BlockPos> consumer) {
            double chunkDistance2 = (distance + 23) * (distance + 23);
            double distance2 = distance * distance;
            for (Chunk chunk : chunks.values()) {
                if (chunk.getDistanceSqrTo(view) > chunkDistance2) {
                    continue;
                }

                for (ChunkSection section : chunk.sections) {
                    if (section == null) {
                        continue;
                    }
                    if (section.getDistanceSqrTo(view) > chunkDistance2) {
                        continue;
                    }

                    for (BlockPos pos : section.missing) {
                        if (pos.distToCenterSqr(view) < distance2) {
                            consumer.accept(pos);
                        }
                    }
                }
            }
        }

        public void forEachMissingState(Vec3 view, double distance, BiConsumer<BlockPos, BlockState> consumer) {
            double chunkDistance2 = (distance + 23) * (distance + 23);
            double distance2 = distance * distance;
            for (Chunk chunk : chunks.values()) {
                if (chunk.getDistanceSqrTo(view) > chunkDistance2) {
                    continue;
                }

                for (ChunkSection section : chunk.sections) {
                    if (section == null) {
                        continue;
                    }
                    if (section.getDistanceSqrTo(view) > chunkDistance2) {
                        continue;
                    }

                    for (BlockPos pos : section.missing) {
                        if (pos.distToCenterSqr(view) < distance2) {
                            consumer.accept(pos, section.getBlockState(pos.getX() & 0x0F, pos.getY() & 0x0F, pos.getZ() & 0x0F));
                        }
                    }
                }
            }
        }

        public void forEachWrong(Vec3 view, double distance, Consumer<BlockPos> consumer) {
            double chunkDistance2 = (distance + 23) * (distance + 23);
            double distance2 = distance * distance;
            for (Chunk chunk : chunks.values()) {
                if (chunk.getDistanceSqrTo(view) > chunkDistance2) {
                    continue;
                }

                for (ChunkSection section : chunk.sections) {
                    if (section == null) {
                        continue;
                    }
                    if (section.getDistanceSqrTo(view) > chunkDistance2) {
                        continue;
                    }

                    for (BlockPos pos : section.wrong) {
                        if (pos.distToCenterSqr(view) < distance2) {
                            consumer.accept(pos);
                        }
                    }
                }
            }
        }

        public void forEachSection(Vec3 view, double distance, Consumer<ChunkSection> consumer) {
            double chunkDistance2 = (distance + 23) * (distance + 23);
            for (Chunk chunk : chunks.values()) {
                if (chunk.getDistanceSqrTo(view) > chunkDistance2) {
                    continue;
                }

                for (ChunkSection section : chunk.sections) {
                    if (section == null) {
                        continue;
                    }
                    if (section.getDistanceSqrTo(view) > chunkDistance2) {
                        continue;
                    }

                    consumer.accept(section);
                }
            }
        }

        public BlockState getBlockState(int x, int y, int z) {
            long chunkIndex = blockToChunkIndex(x, z);
            Chunk chunk = chunks.get(chunkIndex);
            if (chunk == null) {
                return Blocks.AIR.defaultBlockState();
            } else {
                return chunk.getBlockState(x & 0x0F, y, z & 0x0F);
            }
        }

        public void onChunkLoaded( LevelChunk levelChunk) {
            long chunkIndex = chunkToChunkIndex(levelChunk);
            Chunk chunk = chunks.get(chunkIndex);
            if (chunk != null) {
                chunk.onChunkLoaded(this, levelChunk);
            }
        }

        public void onBlockUpdated(BlockUpdateEvent event) {
            long chunkIndex = blockToChunkIndex(event.pos().getX(), event.pos().getZ());
            Chunk chunk = chunks.get(chunkIndex);
            if (chunk != null) {
                chunk.onBlockUpdated(event);
            }
        }

        private long blockToChunkIndex(int x, int z) {
            x = SectionPos.blockToSectionCoord(x);
            z = SectionPos.blockToSectionCoord(z);
            return ChunkPos.asLong(x, z);
        }

        private long chunkToChunkIndex(LevelChunk chunk) {
            return ChunkPos.asLong(chunk.getPos().x, chunk.getPos().z);
        }
    }

    private static class Chunk {

        private static final int MIN_Y = -64;
        private static final int MAX_Y = 320;
        private static final int MIN_SECTION_Y = MIN_Y >> 4;
        private static final int MAX_SECTION_Y = MAX_Y >> 4;

        private final int minX;
        private final int minZ;
        public final ChunkSection[] sections;

        public Chunk(int x, int z) {
            minX = x;
            minZ = z;
            sections = new ChunkSection[MAX_SECTION_Y - MIN_SECTION_Y];
        }

        public BlockState getBlockState(int x, int y, int z) {
            int sectionIndex = (y - MIN_Y) >> 4;
            if (sectionIndex >= sections.length) {
                return Blocks.AIR.defaultBlockState();
            }
            ChunkSection section = sections[sectionIndex];
            if (section == null) {
                return Blocks.AIR.defaultBlockState();
            } else {
                return section.getBlockState(x, y & 0x0F, z);
            }
        }

        public double getDistanceSqrTo(Vec3 point) {
            double dx = point.x - (minX + 8);
            double dz = point.z - (minZ + 8);
            return dx * dx + dz * dz;
        }

        public void onChunkLoaded(Entry entry, LevelChunk chunk) {
            for (int i = 0; i < sections.length; i++) {
                if (sections[i] != null) {
                    sections[i].onChunkLoaded(entry, chunk);
                }
            }
        }

        public void onBlockUpdated(BlockUpdateEvent event) {
            if (event.pos().getY() >= 320) { // light update?
                return;
            }
            int sectionIndex = (event.pos().getY() - MIN_Y) >> 4;
            ChunkSection section = sections[sectionIndex];
            if (section != null) {
                section.onBlockUpdated(event);
            }
        }

        public void setBlockState(int x, int y, int z, BlockState state) {
            int sectionIndex = (y - MIN_Y) >> 4;
            if (sections[sectionIndex] == null) {
                sections[sectionIndex] = new ChunkSection(minX, MIN_Y + (sectionIndex << 4), minZ);
            }
            sections[sectionIndex].setBlockState(x, y & 0x0F, z, state);
        }
    }

    private static class ChunkSection {

        private final int minX;
        private final int minY;
        private final int minZ;
        private final PalettedContainer<BlockState> states;
        private final List<BlockPos> missing = new ArrayList<>();
        private final List<BlockPos> wrong = new ArrayList<>();

        public ChunkSection(int x, int y, int z) {
            minX = x;
            minY = y;
            minZ = z;
            states = new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES);
        }

        public BlockState getBlockState(int x, int y, int z) {
            return states.get(x, y, z);
        }

        public double getDistanceSqrTo(Vec3 point) {
            double dx = point.x - (minX + 8);
            double dy = point.y - (minY + 8);
            double dz = point.z - (minZ + 8);
            return dx * dx + dy * dy + dz * dz;
        }

        public void onChunkLoaded(Entry entry, LevelChunk chunk) {
            missing.clear();
            wrong.clear();

            SchematicaConfig config = ConfigStore.instance.getConfig().schematicaConfig;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int x = 0; x < 16; x++) {
                int worldX = minX | x;
                if (worldX < entry.x1 || worldX >= entry.x2) {
                    continue;
                }

                pos.setX(x);
                for (int y = 0; y < 16; y++) {
                    int worldY = minY | y;
                    if (worldY < entry.y1 || worldY >= entry.y2) {
                        continue;
                    }

                    pos.setY(worldY);
                    for (int z = 0; z < 16; z++) {
                        int worldZ = minZ | z;
                        if (worldZ < entry.z1 || worldZ >= entry.z2) {
                            continue;
                        }

                        pos.setZ(z);
                        BlockState chunkState = chunk.getBlockState(pos);
                        BlockState finalState = states.get(x, y, z);
                        if (isMissing(chunkState, finalState)) {
                            missing.add(new BlockPos(worldX, worldY, worldZ));
                        } else {
                            if (isWrong(chunkState, finalState, config)) {
                                wrong.add(new BlockPos(worldX, worldY, worldZ));
                            }
                        }
                    }
                }
            }
        }

        public void onBlockUpdated(BlockUpdateEvent event) {
            BlockPos pos = event.pos();
            missing.removeIf(p -> p.equals(pos));
            wrong.removeIf(p -> p.equals(pos));
            BlockState chunkState = event.state();
            BlockState finalState = states.get(pos.getX() & 0x0F, pos.getY() & 0x0F, pos.getZ() & 0x0F);
            if (isMissing(chunkState, finalState)) {
                missing.add(pos.immutable());
            } else {
                if (isWrong(chunkState, finalState, ConfigStore.instance.getConfig().schematicaConfig)) {
                    wrong.add(pos.immutable());
                }
            }
        }

        public void setBlockState(int x, int y, int z, BlockState state) {
            states.set(x, y, z, state); // locking can be slow?
        }

        private boolean isMissing(BlockState chunkState, BlockState finalState) {
            return chunkState.canBeReplaced() && !finalState.isAir();
        }

        private boolean isWrong(BlockState chunkState, BlockState finalState, SchematicaConfig config) {
            if (config.airAlwaysValid && finalState.isAir()) {
                return false;
            } else if (config.replaceableAsAir) {
                return !chunkState.canBeReplaced() && chunkState != finalState;
            } else {
                return !chunkState.isAir() && chunkState != finalState;
            }
        }

        private boolean contains(BlockPos pos) {
            if (pos.getX() < minX) {
                return false;
            }
            if (pos.getX() >= minX + 16) {
                return false;
            }
            if (pos.getY() < minY) {
                return false;
            }
            if (pos.getY() >= minY + 16) {
                return false;
            }
            if (pos.getZ() < minZ) {
                return false;
            }
            if (pos.getZ() >= minZ + 16) {
                return false;
            }
            return true;
        }
    }
}