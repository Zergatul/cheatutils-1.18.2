package com.zergatul.cheatutils.webui;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.zergatul.cheatutils.common.Registries;
import com.zergatul.cheatutils.mixins.common.accessors.CompositeRenderTypeAccessor;
import com.zergatul.cheatutils.mixins.common.accessors.CompositeStateAccessor;
import com.zergatul.cheatutils.mixins.common.accessors.TextureStateShardAccessor;
import com.zergatul.cheatutils.utils.JavaRandom;
import com.zergatul.cheatutils.wrappers.BakedModelWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class BlockModelApi extends ApiBase {

    private static final JavaRandom random = new JavaRandom(0);

    @Override
    public String getRoute() {
        return "block-model";
    }

    @Override
    public String get(String id) throws ApiException {
        ResourceLocation loc = ResourceLocation.parse(id);
        Block block = Registries.BLOCKS.getValue(loc);
        if (block == null) {
            throw new ApiException("Cannot find block by id.", HttpResponseCodes.NOT_FOUND);
        }

        List<Quad> quads = getFromBlockModel(block);
        if (quads.isEmpty()) {
            quads = getFromItemRenderer(block);
        }

        return gson.toJson(quads);
    }

    private static List<Quad> getFromBlockModel(Block block) {
        List<Quad> result = new ArrayList<>();

        BlockState state = block.defaultBlockState();
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        for (Direction direction : Direction.values()) {
            List<BakedQuad> quads = BakedModelWrapper.getQuads(model, state, direction, random);
            for (BakedQuad quad : quads) {
                result.add(new Quad(quad, state));
            }
        }

        List<BakedQuad> quads = BakedModelWrapper.getQuads(model, state, null, random);
        for (BakedQuad quad : quads) {
            result.add(new Quad(quad, state));
        }

        return result;
    }

    private static List<Quad> getFromItemRenderer(Block block) {
        List<Quad> result = new ArrayList<>();

        PoseStack pose = new PoseStack();
        ItemStack stack = new ItemStack(block);
        MemoryMultiBufferSource bufferSources = new MemoryMultiBufferSource();
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.GROUND,
                0, OverlayTexture.NO_OVERLAY,
                pose,
                bufferSources,
                null,
                0);

        for (RenderType renderType : bufferSources.buffers.keySet()) {
            if (renderType.mode() != VertexFormat.Mode.QUADS) {
                continue;
            }

            // TODO: extract method
            ResourceLocation texture = null;
            if (renderType instanceof CompositeRenderTypeAccessor accessor) {
                RenderType.CompositeState state = accessor.getState_CU();
                RenderStateShard.EmptyTextureStateShard shard = ((CompositeStateAccessor) (Object) state).getTextureState_CU();
                if (shard instanceof RenderStateShard.TextureStateShard textureStateShard) {
                    Optional<ResourceLocation> location = ((TextureStateShardAccessor) textureStateShard).getTexture_CU();
                    if (location.isPresent()) {
                        texture = location.get();
                    }
                }
            }

            if (texture == null) {
                continue;
            }

            MemoryVertexConsumer consumer = bufferSources.buffers.get(renderType);
            consumer.vertices.forEach(v -> {
                // [-0.125 .. +0.125]
                v.x = (v.x + 0.125f) * 4;

                // [0.0625 .. 0.3125]
                v.y = (v.y - 0.0625f) * 4;

                // [-0.125 .. +0.125]
                v.z = (v.z + 0.125f) * 4;
            });

            int i = 0;
            while (i <= consumer.vertices.size() - 4) {
                result.add(new Quad(
                        texture.toString(),
                        consumer.vertices.get(i++),
                        consumer.vertices.get(i++),
                        consumer.vertices.get(i++),
                        consumer.vertices.get(i++)));
            }
        }

        return result;
    }

    private static class MemoryMultiBufferSource implements MultiBufferSource {

        public Map<RenderType, MemoryVertexConsumer> buffers = new HashMap<>();

        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            if (buffers.containsKey(renderType)) {
                return buffers.get(renderType);
            } else {
                MemoryVertexConsumer consumer = new MemoryVertexConsumer();
                buffers.put(renderType, consumer);
                return consumer;
            }
        }
    }

    public static class MemoryVertexConsumer implements VertexConsumer {

        private Vertex current;
        public List<Vertex> vertices = new ArrayList<>();

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            current = new Vertex();
            current.x = x;
            current.y = y;
            current.z = z;
            vertices.add(current);
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            current.r = r;
            current.g = g;
            current.b = b;
            current.a = a;
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            current.u = u;
            current.v = v;
            return this;
        }

        @Override
        public VertexConsumer setUv1(int x, int y) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return this;
        }
    }

    public static class Quad {

        public final String location;
        public final Vertex[] vertices;

        public Quad(BakedQuad quad, BlockState state) {
            this.location = quad.getSprite().atlasLocation().toString();

            int[] values = quad.getVertices();
            this.vertices = new Vertex[4];
            for (int i = 0; i < 4; i++) {
                int offset = i * 8;
                this.vertices[i] = new Vertex();
                this.vertices[i].x = Float.intBitsToFloat(values[offset]);
                this.vertices[i].y = Float.intBitsToFloat(values[offset + 1]);
                this.vertices[i].z = Float.intBitsToFloat(values[offset + 2]);

                int color = quad.isTinted() ?
                        Minecraft.getInstance().getBlockColors().getColor(state, null, null, 0) :
                        values[offset + 3];
                this.vertices[i].r = color & 0xFF;
                this.vertices[i].g = (color >> 8) & 0xFF;
                this.vertices[i].b = (color >> 16) & 0xFF;
                this.vertices[i].a = quad.isTinted() ? 255 : (color >> 24) & 0xFF;

                this.vertices[i].u = Float.intBitsToFloat(values[offset + 4]);
                this.vertices[i].v = Float.intBitsToFloat(values[offset + 5]);
            }
        }

        public Quad(String location, Vertex v1, Vertex v2, Vertex v3, Vertex v4) {
            this.location = location;
            this.vertices = new Vertex[] { v1, v2, v3, v4 };
        }
    }

    public static class Vertex {
        public float x;
        public float y;
        public float z;
        public int r;
        public int g;
        public int b;
        public int a;
        public float u;
        public float v;
    }
}