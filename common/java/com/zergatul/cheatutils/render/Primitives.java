package com.zergatul.cheatutils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.zergatul.cheatutils.utils.SharedVertexBuffer;
import net.minecraft.client.renderer.CoreShaders;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class Primitives {

    public static void drawCube(
            BufferBuilder bufferBuilder,
            double x1, double y1, double z1,
            double x2, double y2, double z2
    ) {
        drawCube(bufferBuilder, (float) x1, (float) y1, (float) z1, (float) x2, (float) y2, (float) z2);
    }

    public static void renderLines(BufferBuilder bufferBuilder, Matrix4f pose, Matrix4f projection) {
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        RenderHelper.drawBuffer(SharedVertexBuffer.instance, bufferBuilder, pose, projection, CoreShaders.POSITION_COLOR);

        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
    }

    public static void fill(PoseStack poseStack, int x1, int y1, int x2, int y2, int color) {
        if (x1 < x2) {
            int i = x1;
            x1 = x2;
            x2 = i;
        }

        if (y1 < y2) {
            int j = y1;
            y1 = y2;
            y2 = j;
        }

        Matrix4f matrix = poseStack.last().pose();

        float f3 = (float)(color >> 24 & 255) / 255.0F;
        float f = (float)(color >> 16 & 255) / 255.0F;
        float f1 = (float)(color >> 8 & 255) / 255.0F;
        float f2 = (float)(color & 255) / 255.0F;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(CoreShaders.POSITION_COLOR);
        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferbuilder.addVertex(matrix, (float)x1, (float)y2, 0.0F).setColor(f, f1, f2, f3);
        bufferbuilder.addVertex(matrix, (float)x2, (float)y2, 0.0F).setColor(f, f1, f2, f3);
        bufferbuilder.addVertex(matrix, (float)x2, (float)y1, 0.0F).setColor(f, f1, f2, f3);
        bufferbuilder.addVertex(matrix, (float)x1, (float)y1, 0.0F).setColor(f, f1, f2, f3);
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
        RenderSystem.disableBlend();
    }

    public static void fill(PoseStack poseStack, double x1, double y1, double x2, double y2, int color) {
        if (x1 < x2) {
            double buf = x1;
            x1 = x2;
            x2 = buf;
        }

        if (y1 < y2) {
            double buf = y1;
            y1 = y2;
            y2 = buf;
        }

        Matrix4f matrix = poseStack.last().pose();

        float a = (float)(color >> 24 & 255) / 255.0F;
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(CoreShaders.POSITION_COLOR);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferbuilder.addVertex(matrix, (float)x1, (float)y2, 0.0F).setColor(r, g, b, a);
        bufferbuilder.addVertex(matrix, (float)x2, (float)y2, 0.0F).setColor(r, g, b, a);
        bufferbuilder.addVertex(matrix, (float)x2, (float)y1, 0.0F).setColor(r, g, b, a);
        bufferbuilder.addVertex(matrix, (float)x1, (float)y1, 0.0F).setColor(r, g, b, a);
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
        RenderSystem.disableBlend();
    }

    // copy from
    // Lnet/minecraft/client/renderer/entity/ItemRenderer;fillRect(Lcom/mojang/blaze3d/vertex/BufferBuilder;IIIIIIII)V
    public static void fillRect(double p_115154_, double p_115155_, int p_115156_, int p_115157_, int p_115158_, int p_115159_, int p_115160_, int p_115161_) {
        RenderSystem.setShader(CoreShaders.POSITION_COLOR);
        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferbuilder.addVertex((float) (p_115154_ + 0), (float)(p_115155_ + 0), 0.0F).setColor(p_115158_, p_115159_, p_115160_, p_115161_);
        bufferbuilder.addVertex((float)(p_115154_ + 0), (float)(p_115155_ + p_115157_), 0.0F).setColor(p_115158_, p_115159_, p_115160_, p_115161_);
        bufferbuilder.addVertex((float)(p_115154_ + p_115156_), (float)(p_115155_ + p_115157_), 0.0F).setColor(p_115158_, p_115159_, p_115160_, p_115161_);
        bufferbuilder.addVertex((float)(p_115154_ + p_115156_), (float)(p_115155_ + 0), 0.0F).setColor(p_115158_, p_115159_, p_115160_, p_115161_);
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
    }

    public static void drawTexture(Matrix4f matrix, float x, float y, float width, float height, float z, int texX, int texY, int texWidth, int texHeight, int texSizeX, int texSizeY) {
        RenderSystem.setShader(CoreShaders.POSITION_TEX);
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.addVertex(matrix, x, y, z).setUv(1F * texX / texSizeX, 1F * texY / texSizeY);
        bufferBuilder.addVertex(matrix, x, y + height, z).setUv(1F * texX / texSizeX, 1F * (texY + texHeight) / texSizeY);
        bufferBuilder.addVertex(matrix, x + width, y + height, z).setUv(1F * (texX + texWidth) / texSizeX, 1F * (texY + texHeight) / texSizeY);
        bufferBuilder.addVertex(matrix, x + width, y, z).setUv(1F * (texX + texWidth) / texSizeX, 1F * texY / texSizeY);
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
    }

    private static void drawCube(
            BufferBuilder bufferBuilder,
            float x1, float y1, float z1,
            float x2, float y2, float z2
    ) {
        bufferBuilder.addVertex(x1, y1, z1).setColor(1f, 1f, 1f, 1f);
        bufferBuilder.addVertex(x1, y1, z2).setColor(1f, 1f, 1f, 1f);
        bufferBuilder.addVertex(x1, y1, z2).setColor(1f, 1f, 1f, 1f);
        bufferBuilder.addVertex(x2, y1, z2).setColor(1f, 1f, 1f, 1f);
        bufferBuilder.addVertex(x2, y1, z2).setColor(1f, 1f, 1f, 1f);
        bufferBuilder.addVertex(x2, y1, z1).setColor(1f, 1f, 1f, 1f);
        bufferBuilder.addVertex(x2, y1, z1).setColor(1f, 1f, 1f, 1f);
        bufferBuilder.addVertex(x1, y1, z1).setColor(1f, 1f, 1f, 1f);

        bufferBuilder.addVertex(x1, y2, z1).setColor(1f, 1f, 1f, 1f);
        bufferBuilder.addVertex(x1, y2, z2).setColor(1f, 1f, 1f, 1f);
        bufferBuilder.addVertex(x1, y2, z2).setColor(1f, 1f, 1f, 1f);
        bufferBuilder.addVertex(x2, y2, z2).setColor(1f, 1f, 1f, 1f);
        bufferBuilder.addVertex(x2, y2, z2).setColor(1f, 1f, 1f, 1f);
        bufferBuilder.addVertex(x2, y2, z1).setColor(1f, 1f, 1f, 1f);
        bufferBuilder.addVertex(x2, y2, z1).setColor(1f, 1f, 1f, 1f);
        bufferBuilder.addVertex(x1, y2, z1).setColor(1f, 1f, 1f, 1f);

        bufferBuilder.addVertex(x1, y1, z1).setColor(1f, 1f, 1f, 1f);
        bufferBuilder.addVertex(x1, y2, z1).setColor(1f, 1f, 1f, 1f);
        bufferBuilder.addVertex(x1, y1, z2).setColor(1f, 1f, 1f, 1f);
        bufferBuilder.addVertex(x1, y2, z2).setColor(1f, 1f, 1f, 1f);
        bufferBuilder.addVertex(x2, y1, z2).setColor(1f, 1f, 1f, 1f);
        bufferBuilder.addVertex(x2, y2, z2).setColor(1f, 1f, 1f, 1f);
        bufferBuilder.addVertex(x2, y1, z1).setColor(1f, 1f, 1f, 1f);
        bufferBuilder.addVertex(x2, y2, z1).setColor(1f, 1f, 1f, 1f);
    }
}