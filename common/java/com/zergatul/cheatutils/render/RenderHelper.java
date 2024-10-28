package com.zergatul.cheatutils.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderProgram;
import org.joml.Matrix4f;

public class RenderHelper {

    public static void drawBuffer(VertexBuffer vertexBuffer, BufferBuilder bufferBuilder, Matrix4f pose, Matrix4f projection, ShaderProgram shader) {
        MeshData data = bufferBuilder.build();
        if (data != null) {
            vertexBuffer.bind();
            vertexBuffer.upload(data);
            vertexBuffer.drawWithShader(pose, projection, Minecraft.getInstance().getShaderManager().getProgram(shader));
            VertexBuffer.unbind();
        }
    }
}