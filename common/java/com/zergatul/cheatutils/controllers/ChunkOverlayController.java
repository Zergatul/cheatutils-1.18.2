package com.zergatul.cheatutils.controllers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zergatul.cheatutils.chunkoverlays.*;
import com.zergatul.cheatutils.common.Events;
import com.zergatul.cheatutils.render.Primitives;
import com.zergatul.cheatutils.utils.Dimension;
import com.zergatul.cheatutils.common.events.BlockUpdateEvent;
import com.zergatul.cheatutils.common.events.MouseScrollEvent;
import com.zergatul.cheatutils.common.events.RenderGuiEvent;
import com.zergatul.cheatutils.common.events.PreRenderGuiOverlayEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.chunk.LevelChunk;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class ChunkOverlayController {

    public static final ChunkOverlayController instance = new ChunkOverlayController();

    // store texture of 16x16 chunks
    private static final int SegmentSize = 16;
    // 250ms
    private static final long UpdateDelay = 250L * 1000000;
    private static final int TranslateZ = 250;
    private static final float MinScale = 1 * SegmentSize;
    private static final float MaxScale = 32 * SegmentSize;
    private static final float ScaleStep = 1.3f;

    private final Minecraft mc = Minecraft.getInstance();
    private final List<AbstractChunkOverlay> overlays = new ArrayList<>();
    private float scale = 16 * SegmentSize;

    private ChunkOverlayController() {
        register(new ExplorationMiniMapChunkOverlay(SegmentSize, UpdateDelay));
        register(new NewChunksOverlay(SegmentSize, UpdateDelay));
        register(new WorldDownloadChunkOverlay(SegmentSize, UpdateDelay));

        Events.RawChunkLoaded.add(this::onChunkLoaded);
        Events.RawBlockUpdated.add(this::onBlockChanged);
        Events.PostRenderGui.add(this::render);
        Events.PreRenderGuiOverlay.add(this::onPreRenderGameOverlay);
        Events.MouseScroll.add(this::onMouseScroll);
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractChunkOverlay> T ofType(Class<T> clazz) {
        return (T) overlays.stream().filter(o -> o.getClass() == clazz).findFirst().orElse(null);
    }

    private void render(RenderGuiEvent event) {
        if (noOverlaysEnabled()) {
            return;
        }

        for (AbstractChunkOverlay overlay: overlays) {
            overlay.onPreRender();
        }

        if (!mc.options.keyPlayerList.isDown()) {
            return;
        }

        if (Screen.hasAltDown()) {
            return;
        }

        if (mc.player == null || mc.level == null) {
            return;
        }

        float frameTime = event.getTickDelta();
        float xp = (float) Mth.lerp(frameTime, mc.player.xo, mc.player.getX());
        float zp = (float) Mth.lerp(frameTime, mc.player.zo, mc.player.getZ());
        float xc = (float) mc.gameRenderer.getMainCamera().getPosition().x;
        float zc = (float) mc.gameRenderer.getMainCamera().getPosition().z;
        float yRot = mc.gameRenderer.getMainCamera().getYRot();

        PoseStack poseStack = event.getGuiGraphics().pose();
        poseStack.pushPose();
        poseStack.setIdentity();
        poseStack.translate(1d * mc.getWindow().getGuiScaledWidth() / 2, 1d * mc.getWindow().getGuiScaledHeight() / 2, TranslateZ);

        Quaternionf quaternion = new Quaternionf(0, 0, 0, 1);
        quaternion.rotationYXZ(-(float)Math.PI, -(float)Math.PI, -yRot * ((float)Math.PI / 180F));
        poseStack.mulPose(quaternion);

        //RenderSystem.enableDepthTest();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        RenderSystem.setShader(CoreShaders.POSITION_TEX);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        //RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 0.5f);

        float multiplier = 1f / (16 * SegmentSize) * scale;
        Dimension dimension = Dimension.get(mc.level);

        //RenderSystem.enableTexture();

        for (AbstractChunkOverlay overlay: overlays) {
            int z = overlay.getTranslateZ();
            for (Segment segment: overlay.getSegments(dimension)) {
                if (segment.texture == null) {
                    continue;
                }

                /**/
                //RenderSystem.bindTextureForSetup(segment.texture.getId());
                //RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                //RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                /**/

                RenderSystem.setShaderTexture(0, segment.texture.getId());

                float x = (segment.pos.x * 16 * SegmentSize - xc) * multiplier;
                float y = (segment.pos.z * 16 * SegmentSize - zc) * multiplier;

                Primitives.drawTexture(
                        poseStack.last().pose(),
                        x, y, scale, scale, z,
                        0, 0, 16 * SegmentSize, 16 * SegmentSize,
                        16 * SegmentSize, 16 * SegmentSize);
            }
        }

        for (AbstractChunkOverlay overlay: overlays) {
            overlay.onPostDrawSegments(dimension, poseStack, xp, zp, xc, zc, multiplier);
        }

        poseStack.popPose();
    }

    private void onPreRenderGameOverlay(PreRenderGuiOverlayEvent event) {
        if (event.getGuiOverlayType() == PreRenderGuiOverlayEvent.GuiOverlayType.PLAYER_LIST) {
            if (noOverlaysEnabled()) {
                return;
            }
            if (Screen.hasAltDown()) {
                return;
            }
            event.cancel();
        }
    }

    private void onMouseScroll(MouseScrollEvent event) {
        if (noOverlaysEnabled()) {
            return;
        }

        if (!mc.options.keyPlayerList.isDown()) {
            return;
        }

        event.cancel();

        if (event.getScrollDelta() >= 1.0d) {
            if (scale < MaxScale) {
                scale *= ScaleStep;
            }
        }

        if (event.getScrollDelta() <= -1.0d) {
            if (scale > MinScale) {
                scale /= ScaleStep;
            }
        }
    }

    private void register(AbstractChunkOverlay overlay) {
        overlays.add(overlay);
    }

    private void onChunkLoaded(LevelChunk chunk) {
        for (AbstractChunkOverlay overlay : overlays) {
            overlay.onChunkLoaded(chunk);
        }
    }

    private void onBlockChanged(BlockUpdateEvent event) {
        assert mc.level != null;

        Dimension dimension = Dimension.get(mc.level);
        for (AbstractChunkOverlay overlay : overlays) {
            overlay.onBlockChanged(dimension, event.pos(), event.state());
        }
    }

    private boolean noOverlaysEnabled() {
        return overlays.stream().noneMatch(AbstractChunkOverlay::isEnabled);
    }
}