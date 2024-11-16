package com.zergatul.cheatutils.modules.esp;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.zergatul.cheatutils.collections.FloatList;
import com.zergatul.cheatutils.collections.ImmutableList;
import com.zergatul.cheatutils.common.Events;
import com.zergatul.cheatutils.compatibility.WrappedRenderType;
import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.configs.EntityEspConfig;
import com.zergatul.cheatutils.font.StylizedText;
import com.zergatul.cheatutils.mixins.common.accessors.CompositeRenderTypeAccessor;
import com.zergatul.cheatutils.mixins.common.accessors.CompositeStateAccessor;
import com.zergatul.cheatutils.mixins.common.accessors.TextureStateShardAccessor;
import com.zergatul.cheatutils.modules.Module;
import com.zergatul.cheatutils.modules.utilities.RenderUtilities;
import com.zergatul.cheatutils.render.*;
import com.zergatul.cheatutils.common.events.RenderWorldLastEvent;
import com.zergatul.cheatutils.scripting.modules.EntityEspEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Predicate;

public class EntityEsp implements Module {

    public static final EntityEsp instance = new EntityEsp();

    private final Minecraft mc = Minecraft.getInstance();
    private final Map<EntityEspConfig, List<BufferedVerticesEntry>> overlayBufferedVertices = new HashMap<>();
    private final Map<EntityEspConfig, List<BufferedVerticesEntry>> outlineBufferedVertices = new HashMap<>();
    private final Map<EntityScriptResultKey, EntityScriptResult> scriptResults = new HashMap<>();

    private EntityEsp() {
        Events.BeforeRenderWorld.add(this::onBeforeRender);
        Events.AfterRenderWorld.add(this::render);
    }

    private void onBeforeRender() {
        scriptResults.clear();
    }

    public MultiBufferSource onRenderEntityModifyBufferSource(Entity entity, MultiBufferSource bufferSource) {
        if (mc.player != null && ConfigStore.instance.getConfig().esp) {
            for (EntityEspConfig config : ConfigStore.instance.getConfig().entities.configs) {
                if (!config.enabled) {
                    continue;
                }

                if (!config.isValidEntity(entity)) {
                    continue;
                }

                boolean drawOverlay =
                        config.drawOverlay &&
                        entity.distanceToSqr(mc.player) < config.getOverlayMaxDistanceSqr() &&
                        !isOverlayDisabledFromScript(config, entity);
                boolean drawOutline =
                        config.useModOutline() &&
                        entity.distanceToSqr(mc.player) < config.getGlowMaxDistanceSqr() &&
                        !isOutlineDisabledFromScript(config, entity);
                if (drawOverlay || drawOutline) {
                    // TODO: cache wrappers? each entity = new wrapper!
                    return new EntityEsp.MultiBufferSourceWrapper(config, bufferSource, drawOverlay, drawOutline);
                }
            }
        }

        return bufferSource;
    }

    public boolean shouldEntityGlow(Entity entity) {
        if (!ConfigStore.instance.getConfig().esp) {
            return false;
        }
        if (mc.player == null) {
            return false;
        }
        for (EntityEspConfig config : ConfigStore.instance.getConfig().entities.configs) {
            if (config.useMinecraftOutline() && config.isValidEntity(entity) && entity.distanceToSqr(mc.player) < config.getGlowMaxDistanceSqr()) {
                return !isOutlineDisabledFromScript(config, entity);
            }
        }
        return false;
    }

    public Integer getGlowColor(Entity entity) {
        if (!ConfigStore.instance.getConfig().esp) {
            return null;
        }
        for (EntityEspConfig config : ConfigStore.instance.getConfig().entities.configs) {
            if (config.useMinecraftOutline() && config.isValidEntity(entity)) {
                return config.glowColor.getRGB();
            }
        }
        return null;
    }

    public StylizedText getTitleOverride(EntityEspConfig config, Entity entity) {
        if (!config.scriptEnabled || config.script == null) {
            return null;
        }

        EntityScriptResult result = scriptResults.get(new EntityScriptResultKey(entity.getId(), config));
        if (result != null) {
            return result.title;
        }

        return executeScript(config, entity).title;
    }

    private void render(RenderWorldLastEvent event) {
        assert mc.player != null;

        if (!ConfigStore.instance.getConfig().esp) {
            return;
        }

        float partialTicks = event.getTickDelta();

        Vec3 playerPos = event.getPlayerPos();
        double playerX = playerPos.x;
        double playerY = playerPos.y;
        double playerZ = playerPos.z;

        Vec3 tracerCenter = event.getTracerCenter();
        double tracerX = tracerCenter.x;
        double tracerY = tracerCenter.y;
        double tracerZ = tracerCenter.z;

        LineRenderer lineRenderer = RenderUtilities.instance.getLineRenderer();
        ThickLineRenderer thickLineRenderer = RenderUtilities.instance.getThickLineRenderer();

        lineRenderer.begin(event, false);
        thickLineRenderer.begin(event, false);

        ImmutableList<EntityEspConfig> list = ConfigStore.instance.getConfig().entities.configs;
        for (Entity entity : mc.player.clientLevel.entitiesForRendering()) {
            if (entity instanceof LocalPlayer) {
                continue;
            }

            if (entity.isRemoved()) {
                continue;
            }

            double dx = entity.getX() - playerX;
            double dy = entity.getY() - playerY;
            double dz = entity.getZ() - playerZ;
            double distanceSqr = dx * dx + dy * dy + dz * dz;

            EntityEspConfig config = list.stream().filter(c ->
                    c.enabled &&
                    c.drawOutline &&
                    c.isValidEntity(entity) &&
                    distanceSqr < c.getOutlineMaxDistanceSqr()).findFirst().orElse(null);

            if (config != null && !isCollisionBoxDisabledFromScript(config, entity)) {
                Vec3 pos = entity.getPosition(partialTicks);
                AABB box = entity.getDimensions(entity.getPose()).makeBoundingBox(pos);

                float r = config.outlineColor.getRed() / 255f;
                float g = config.outlineColor.getGreen() / 255f;
                float b = config.outlineColor.getBlue() / 255f;
                float a = config.outlineColor.getAlpha() / 255f;

                final int lineWidth = config.outlineWidth;
                if (lineWidth == 1) {
                    lineRenderer.cuboid(
                            box.minX, box.minY, box.minZ,
                            box.maxX, box.maxY, box.maxZ,
                            r, g, b, a);
                } else {
                    thickLineRenderer.setWidth(lineWidth);
                    thickLineRenderer.cuboid(
                            box.minX, box.minY, box.minZ,
                            box.maxX, box.maxY, box.maxZ,
                            r, g, b, a);
                }
            }

            config = list.stream().filter(c ->
                    c.enabled &&
                            c.drawTracers &&
                            c.clazz.isInstance(entity) &&
                            distanceSqr < c.getTracerMaxDistanceSqr()).findFirst().orElse(null);

            if (config != null && !isTracerDisabledFromScript(config, entity)) {
                float r = config.tracerColor.getRed() / 255f;
                float g = config.tracerColor.getGreen() / 255f;
                float b = config.tracerColor.getBlue() / 255f;
                float a = config.tracerColor.getAlpha() / 255f;

                Vec3 pos = entity.getPosition(event.getTickDelta());
                final int lineWidth = config.tracerWidth;
                if (lineWidth == 1) {
                    lineRenderer.line(tracerX, tracerY, tracerZ, pos.x, pos.y, pos.z, r, g, b, a);
                } else {
                    thickLineRenderer.setWidth(lineWidth);
                    thickLineRenderer.line(tracerX, tracerY, tracerZ, pos.x, pos.y, pos.z, r, g, b, a);
                }
            }
        }

        lineRenderer.end();
        thickLineRenderer.end();

        drawOverlays(event);
        drawOutlines(event);

        TextureStateTracker.restore();
    }

    private void drawOverlays(RenderWorldLastEvent event) {
        EntityOverlayRenderer renderer = RenderUtilities.instance.getEntityOverlayRenderer();
        for (EntityEspConfig config: overlayBufferedVertices.keySet()) {
            renderer.begin();

            List<BufferedVerticesEntry> entries = overlayBufferedVertices.get(config);
            for (BufferedVerticesEntry entry: entries) {
                FloatList list = entry.list;
                if (list.size() == 0) {
                    continue;
                }

                int size = list.size();
                if (size % 20 != 0) {
                    continue; // invalid buffer, silently skip
                }

                int i = 0;
                while (i < size) {
                    float x1 = list.get(i++);
                    float y1 = list.get(i++);
                    float z1 = list.get(i++);
                    float u1 = list.get(i++);
                    float v1 = list.get(i++);
                    float x2 = list.get(i++);
                    float y2 = list.get(i++);
                    float z2 = list.get(i++);
                    float u2 = list.get(i++);
                    float v2 = list.get(i++);
                    float x3 = list.get(i++);
                    float y3 = list.get(i++);
                    float z3 = list.get(i++);
                    float u3 = list.get(i++);
                    float v3 = list.get(i++);
                    float x4 = list.get(i++);
                    float y4 = list.get(i++);
                    float z4 = list.get(i++);
                    float u4 = list.get(i++);
                    float v4 = list.get(i++);
                    renderer.quad(
                            x1, y1, z1, u1, v1,
                            x2, y2, z2, u2, v2,
                            x3, y3, z3, u3, v3,
                            x4, y4, z4, u4, v4);
                }

                AbstractTexture texture = mc.getTextureManager().getTexture(entry.texture);
                renderer.renderBuffer(event.getMvp(), texture.getId());
            }

            renderer.end(
                    config.overlayColor.getRed() / 255f,
                    config.overlayColor.getGreen() / 255f,
                    config.overlayColor.getBlue() / 255f,
                    config.overlayColor.getAlpha() / 255f);
        }

        overlayBufferedVertices.clear();
    }

    private void drawOutlines(RenderWorldLastEvent event) {
        EntityOutlineRenderer renderer = RenderUtilities.instance.getEntityOutlineRenderer();
        for (EntityEspConfig config: outlineBufferedVertices.keySet()) {
            renderer.begin();

            for (BufferedVerticesEntry entry : outlineBufferedVertices.get(config)) {
                FloatList list = entry.list;
                if (list.size() == 0) {
                    continue;
                }

                int size = list.size();
                int i = 0;
                while (i < size) {
                    float x1 = list.get(i++);
                    float y1 = list.get(i++);
                    float z1 = list.get(i++);
                    float u1 = list.get(i++);
                    float v1 = list.get(i++);
                    float x2 = list.get(i++);
                    float y2 = list.get(i++);
                    float z2 = list.get(i++);
                    float u2 = list.get(i++);
                    float v2 = list.get(i++);
                    float x3 = list.get(i++);
                    float y3 = list.get(i++);
                    float z3 = list.get(i++);
                    float u3 = list.get(i++);
                    float v3 = list.get(i++);
                    float x4 = list.get(i++);
                    float y4 = list.get(i++);
                    float z4 = list.get(i++);
                    float u4 = list.get(i++);
                    float v4 = list.get(i++);
                    renderer.quad(
                            x1, y1, z1, u1, v1,
                            x2, y2, z2, u2, v2,
                            x3, y3, z3, u3, v3,
                            x4, y4, z4, u4, v4);
                }

                AbstractTexture texture = mc.getTextureManager().getTexture(entry.texture);
                renderer.renderBuffer(event.getMvp(), texture.getId());
            }

            renderer.end(
                    config.glowColor.getRed() / 255f,
                    config.glowColor.getGreen() / 255f,
                    config.glowColor.getBlue() / 255f,
                    config.glowColor.getAlpha() / 255f);
        }

        outlineBufferedVertices.clear();
    }

    private boolean isCollisionBoxDisabledFromScript(EntityEspConfig config, Entity entity) {
        return getBooleanFromScript(config, entity, result -> result.collisionBoxDisabled);
    }

    private boolean isTracerDisabledFromScript(EntityEspConfig config, Entity entity) {
        return getBooleanFromScript(config, entity, result -> result.tracerDisabled);
    }

    private boolean isOverlayDisabledFromScript(EntityEspConfig config, Entity entity) {
        return getBooleanFromScript(config, entity, result -> result.overlayDisabled);
    }

    private boolean isOutlineDisabledFromScript(EntityEspConfig config, Entity entity) {
        return getBooleanFromScript(config, entity, result -> result.outlineDisabled);
    }

    private boolean getBooleanFromScript(EntityEspConfig config, Entity entity, Predicate<EntityScriptResult> predicate) {
        if (!config.scriptEnabled || config.script == null) {
            return false;
        }

        EntityScriptResult result = scriptResults.get(new EntityScriptResultKey(entity.getId(), config));
        if (result != null) {
            return predicate.test(result);
        }

        return predicate.test(executeScript(config, entity));
    }

    private EntityScriptResult executeScript(EntityEspConfig config, Entity entity) {
        assert config.script != null;

        EntityScriptResult result = new EntityScriptResult(entity.getId(), config);
        scriptResults.put(new EntityScriptResultKey(entity.getId(), config), result);
        config.script.accept(entity.getId(), new EntityEspEvent(result));
        return result;
    }

    public static class MultiBufferSourceWrapper implements MultiBufferSource {

        private final EntityEspConfig config;
        private final MultiBufferSource source;
        private final boolean overlay;
        private final boolean outline;

        public MultiBufferSourceWrapper(EntityEspConfig config, MultiBufferSource source, boolean overlay, boolean outline) {
            this.config = config;
            this.source = source;
            this.overlay = overlay;
            this.outline = outline;
        }

        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            if (isGoodRenderType(renderType)) {
                // for Iris compatibility
                if (renderType instanceof WrappedRenderType wrapped) {
                    renderType = wrapped.unwrap();
                }
                if (renderType instanceof CompositeRenderTypeAccessor accessor) {
                    RenderType.CompositeState state = accessor.getState_CU();
                    RenderStateShard.EmptyTextureStateShard shard = ((CompositeStateAccessor) (Object) state).getTextureState_CU();
                    if (shard instanceof RenderStateShard.TextureStateShard textureStateShard) {
                        Optional<ResourceLocation> texture = ((TextureStateShardAccessor) textureStateShard).getTexture_CU();
                        if (texture.isPresent()) {
                            VertexConsumer inner = source.getBuffer(renderType);
                            if (inner instanceof VertexConsumerWrapper) {
                                return inner;
                            } else {
                                return new VertexConsumerWrapper(config, texture.get(), source.getBuffer(renderType), overlay, outline);
                            }
                        }
                    }
                }
            }

            return source.getBuffer(renderType);
        }

        private static boolean isGoodRenderType(RenderType renderType) {
            if (renderType.outline().isEmpty()) {
                return false;
            }
            if (renderType.mode() != VertexFormat.Mode.QUADS) {
                return false;
            }
            if (renderType.format().getElements().stream().noneMatch(e -> e.usage() == VertexFormatElement.Usage.POSITION)) {
                return false;
            }
            if (renderType.format().getElements().stream().noneMatch(e -> e.usage() == VertexFormatElement.Usage.UV)) {
                return false;
            }
            return true;
        }
    }

    public static class VertexConsumerWrapper implements VertexConsumer {

        private final VertexConsumer consumer;
        private FloatList overlayList;
        private FloatList outlineList;

        public VertexConsumerWrapper(
                EntityEspConfig config,
                ResourceLocation texture,
                VertexConsumer consumer,
                boolean overlay,
                boolean outline
        ) {
            this.consumer = consumer;

            if (overlay) {
                List<BufferedVerticesEntry> entries = EntityEsp.instance.overlayBufferedVertices.computeIfAbsent(config, c -> new ArrayList<>());
                for (BufferedVerticesEntry entry : entries) {
                    if (entry.texture.equals(texture)) {
                        overlayList = entry.list;
                        break;
                    }
                }

                if (overlayList == null) {
                    BufferedVerticesEntry entry = new BufferedVerticesEntry(texture, new FloatList());
                    entries.add(entry);
                    overlayList = entry.list;
                }
            }

            if (outline) {
                List<BufferedVerticesEntry> entries = EntityEsp.instance.outlineBufferedVertices.computeIfAbsent(config, c -> new ArrayList<>());
                for (BufferedVerticesEntry entry : entries) {
                    if (entry.texture.equals(texture)) {
                        outlineList = entry.list;
                        break;
                    }
                }

                if (outlineList == null) {
                    BufferedVerticesEntry entry = new BufferedVerticesEntry(texture, new FloatList());
                    entries.add(entry);
                    outlineList = entry.list;
                }
            }
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            if (overlayList != null) {
                overlayList.add(x);
                overlayList.add(y);
                overlayList.add(z);
            }
            if (outlineList != null) {
                outlineList.add(x);
                outlineList.add(y);
                outlineList.add(z);
            }
            consumer.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            consumer.setColor(r, g, b, a);
            return this;
        }

        @Override
        public VertexConsumer setUv(float x, float y) {
            if (overlayList != null) {
                overlayList.add(x);
                overlayList.add(y);
            }
            if (outlineList != null) {
                outlineList.add(x);
                outlineList.add(y);
            }
            consumer.setUv(x, y);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int x, int y) {
            consumer.setUv1(x, y);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int x, int y) {
            consumer.setUv2(x, y);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            consumer.setNormal(x, y, z);
            return this;
        }
    }

    private record BufferedVerticesEntry(ResourceLocation texture, FloatList list) {}

    public static class EntityScriptResult {

        public final int id;
        public final EntityEspConfig config;
        public boolean tracerDisabled;
        public boolean outlineDisabled;
        public boolean overlayDisabled;
        public boolean collisionBoxDisabled;
        public StylizedText title;

        public EntityScriptResult(int id, EntityEspConfig config) {
            this.id = id;
            this.config = config;
        }
    }

    private static class EntityScriptResultKey {

        private final int id;
        private final EntityEspConfig config;

        public EntityScriptResultKey(int id, EntityEspConfig config) {
            this.id = id;
            this.config = config;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof EntityScriptResultKey other) {
                return other.id == id && other.config == config;
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return 31 * id + config.hashCode();
        }
    }
}