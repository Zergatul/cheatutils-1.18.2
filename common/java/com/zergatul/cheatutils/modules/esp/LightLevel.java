package com.zergatul.cheatutils.modules.esp;

import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.datafixers.util.Pair;
import com.zergatul.cheatutils.ModMain;
import com.zergatul.cheatutils.common.Events;
import com.zergatul.cheatutils.concurrent.TickEndExecutor;
import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.configs.LightLevelConfig;
import com.zergatul.cheatutils.controllers.BlockEventsProcessor;
import com.zergatul.cheatutils.modules.Module;
import com.zergatul.cheatutils.render.RenderHelper;
import com.zergatul.cheatutils.utils.Dimension;
import com.zergatul.cheatutils.common.events.BlockUpdateEvent;
import com.zergatul.cheatutils.common.events.RenderWorldLastEvent;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class LightLevel implements Module {

    public static final LightLevel instance = new LightLevel();

    private final Minecraft mc = Minecraft.getInstance();
    private final Logger logger = LogManager.getLogger(LightLevel.class);
    private final ResourceLocation[] textures = new ResourceLocation[16];
    private final HashMap<ChunkPos, HashSet<BlockPos>> chunks = new HashMap<>();
    private final List<BlockPos> listForRendering = new ArrayList<>();
    private boolean active = false;
    private VertexBuffer vertexBuffer;
    private final Map<Direction, Pair<float[], float[]>> rotations = Map.ofEntries(
            Map.entry(Direction.NORTH, new Pair<>(
                    new float[] { 0, 0, 1, 1 },
                    new float[] { 0, 1, 1, 0 })),
            Map.entry(Direction.SOUTH, new Pair<>(
                    new float[] { 1, 1, 0, 0 },
                    new float[] { 1, 0, 0, 1 })),
            Map.entry(Direction.EAST, new Pair<>(
                    new float[] { 0, 1, 1, 0 },
                    new float[] { 1, 1, 0, 0 })),
            Map.entry(Direction.WEST, new Pair<>(
                    new float[] { 1, 0, 0, 1 },
                    new float[] { 0, 0, 1, 1 })));

    private LightLevel() {
        for (int i = 0; i < 16; i++) {
            textures[i] = ResourceLocation.fromNamespaceAndPath(ModMain.MODID, "textures/light-level-" + i + ".png");
        }

        RenderSystem.recordRenderCall(() -> vertexBuffer = new VertexBuffer(BufferUsage.DYNAMIC_WRITE));

        Events.AfterRenderWorld.add(this::render);
        Events.RawChunkLoaded.add(this::onChunkLoaded);
        Events.RawChunkUnloaded.add(this::onChunkUnLoaded);
        Events.RawBlockUpdated.add(this::onBlockChanged);
    }

    public void onChanged() {
        TickEndExecutor.instance.execute(() -> {
            boolean value = ConfigStore.instance.getConfig().lightLevelConfig.enabled;
            if (active != value) {
                active = value;
                if (active) {
                    AtomicReferenceArray<LevelChunk> chunks = BlockEventsProcessor.instance.getRawChunks();
                    for (int i = 0; i < chunks.length(); i++) {
                        LevelChunk chunk = chunks.get(i);
                        if (chunk != null) {
                            onChunkLoaded(chunk);
                        }
                    }
                }
            }
        });
    }

    private void render(RenderWorldLastEvent event) {
        assert mc.level != null;

        LightLevelConfig config = ConfigStore.instance.getConfig().lightLevelConfig;
        if (!config.enabled) {
            return;
        }

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 view = camera.getPosition();

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.enableBlend();
        RenderSystem.setShader(CoreShaders.POSITION_TEX);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        Direction direction = Direction.fromYRot(camera.getYRot());
        Pair<float[], float[]> texRot = rotations.get(direction);
        float[] u = texRot.getFirst();
        float[] v = texRot.getSecond();

        double maxDistance2 = config.maxDistance * config.maxDistance;
        double xc = config.useFreeCamPosition ? view.x : mc.player.getX();
        double yc = config.useFreeCamPosition ? view.y : mc.player.getY();
        double zc = config.useFreeCamPosition ? view.z : mc.player.getZ();

        List<BlockPos> listTracers = new ArrayList<>();
        for (BlockPos pos: getBlockForRendering()) {
            double dx = xc - pos.getX();
            double dy = yc - pos.getY();
            double dz = zc - pos.getZ();
            if (dx * dx + dy * dy + dz * dz > maxDistance2) {
                continue;
            }
            int blockLight = mc.level.getBrightness(LightLayer.BLOCK, pos);
            if (blockLight == 0) {
                listTracers.add(pos);
            }
            if (config.showLightLevelValue) {
                RenderSystem.setShaderTexture(0, textures[blockLight]);
                float y = (float)(pos.getY() + 0.05 - view.y);
                float x1 = (float)(pos.getX() + 0.05 - view.x);
                float z1 = (float)(pos.getZ() + 0.05 - view.z);
                float x2 = x1 + 0.9f;
                float z2 = z1 + 0.9f;
                BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                bufferBuilder.addVertex(x1, y, z1).setUv(u[0], v[0]);
                bufferBuilder.addVertex(x1, y, z2).setUv(u[1], v[1]);
                bufferBuilder.addVertex(x2, y, z2).setUv(u[2], v[2]);
                bufferBuilder.addVertex(x2, y, z1).setUv(u[3], v[3]);

                RenderHelper.drawBuffer(vertexBuffer, bufferBuilder, event.getPose(), event.getProjection(), CoreShaders.POSITION_TEX);
            }
        }

        Vec3 tracerCenter = event.getTracerCenter();
        double tracerX = tracerCenter.x;
        double tracerY = tracerCenter.y;
        double tracerZ = tracerCenter.z;

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        for (BlockPos pos: listTracers) {
            double y = pos.getY() + 0.05 - view.y;
            if (config.showTracers) {
                buffer.addVertex((float) (tracerX - view.x), (float) (tracerY - view.y), (float) (tracerZ - view.z)).setColor(1f, 1f, 1f, 0.5f);
                buffer.addVertex((float) (pos.getX() + 0.5 - view.x), (float) y, (float) (pos.getZ() + 0.5 - view.z)).setColor(1f, 1f, 1f, 0.5f);
            }

            if (config.showLocations) {
                double x1 = pos.getX() + 0.05 - view.x;
                double z1 = pos.getZ() + 0.05 - view.z;
                double x2 = x1 + 0.9;
                double z2 = z1 + 0.9;
                buffer.addVertex((float) x1, (float) y, (float) z1).setColor(1f, 1f, 1f, 0.5f);
                buffer.addVertex((float) x1, (float) y, (float) z2).setColor(1f, 1f, 1f, 0.5f);
                buffer.addVertex((float) x1, (float) y, (float) z2).setColor(1f, 1f, 1f, 0.5f);
                buffer.addVertex((float) x2, (float) y, (float) z2).setColor(1f, 1f, 1f, 0.5f);
                buffer.addVertex((float) x2, (float) y, (float) z2).setColor(1f, 1f, 1f, 0.5f);
                buffer.addVertex((float) x2, (float) y, (float) z1).setColor(1f, 1f, 1f, 0.5f);
                buffer.addVertex((float) x2, (float) y, (float) z1).setColor(1f, 1f, 1f, 0.5f);
                buffer.addVertex((float) x1, (float) y, (float) z1).setColor(1f, 1f, 1f, 0.5f);
            }
        }

        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        RenderHelper.drawBuffer(vertexBuffer, buffer, event.getPose(), event.getProjection(), CoreShaders.POSITION_COLOR);

        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
    }

    public List<BlockPos> getBlockForRendering() {
        listForRendering.clear();
        synchronized (chunks) {
            for (HashSet<BlockPos> set : chunks.values()) {
                synchronized (set) {
                    listForRendering.addAll(set);
                }
            }
        }
        return listForRendering;
    }

    private void onChunkLoaded(LevelChunk chunk) {
        if (!active) {
            return;
        }

        Dimension dimension = Dimension.get((ClientLevel) chunk.getLevel());
        ChunkPos chunkPos = chunk.getPos();
        HashSet<BlockPos> set;
        synchronized (chunks) {
            set = chunks.get(chunkPos);
            if (set == null) {
                set = new HashSet<>();
                chunks.put(chunkPos, set);
            }
        }
        int xc = chunk.getPos().x << 4;
        int zc = chunk.getPos().z << 4;
        synchronized (set) {
            set.clear();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int height = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                    for (int y = dimension.getMinY(); y <= height; y++) {
                        int xb = xc | x;
                        int zb = zc | z;
                        BlockPos pos = new BlockPos(xb, y, zb);
                        checkBlock(chunk, pos, set);
                    }
                }
            }
        }
    }

    private void onChunkUnLoaded(ChunkAccess chunk) {
        if (!active) {
            return;
        }

        synchronized (chunks) {
            chunks.remove(chunk.getPos());
        }
    }

    private void onBlockChanged(BlockUpdateEvent event) {
        if (!active) {
            return;
        }

        LevelChunk chunk = event.chunk();
        HashSet<BlockPos> set;
        synchronized (chunks) {
            set = chunks.get(chunk.getPos());
        }
        if (set == null) {
            return;
        }
        synchronized (set) {
            BlockPos pos = event.pos();
            BlockPos above = pos.above();
            BlockPos below = pos.below();
            BlockPos below2 = below.below();
            set.remove(pos);
            set.remove(above);
            set.remove(below);
            set.remove(below2);
            checkBlock(chunk, pos, set);
            checkBlock(chunk, above, set);
            checkBlock(chunk, below, set);
            checkBlock(chunk, below2, set);
        }
    }

    private void checkBlock(ChunkAccess chunk, BlockPos pos, HashSet<BlockPos> set) {
        if (canSpawnOn(chunk.getBlockState(pos), pos)) {
            BlockPos posAbove = pos.above();
            BlockState stateAbove = chunk.getBlockState(posAbove);
            if (stateAbove.isSolid()) {
                return;
            }
            if (!stateAbove.getFluidState().isEmpty()) {
                return;
            }
            if (stateAbove.is(BlockTags.PREVENT_MOB_SPAWNING_INSIDE)) {
                return;
            }

            BlockState stateAbove2 = chunk.getBlockState(posAbove.above());
            if (stateAbove2.isSolid()) {
                return;
            }

            set.add(posAbove);
        }
    }

    private boolean canSpawnOn(BlockState state, BlockPos pos) {
        assert mc.level != null;

        if (state.getBlock() instanceof SlabBlock) {
            return state.getValue(SlabBlock.TYPE) != SlabType.BOTTOM;
        }

        if (state.getBlock() instanceof StairBlock) {
            return state.getValue(StairBlock.HALF) == Half.TOP;
        }

        if (!state.canOcclude()) {
            return false;
        }

        if (state.getBlock() == Blocks.BEDROCK) {
            return false;
        }

        return state.isSolid() && state.isCollisionShapeFullBlock(mc.level, pos);
    }
}