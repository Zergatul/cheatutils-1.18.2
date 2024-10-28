package com.zergatul.cheatutils.utils;

import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.vertex.VertexBuffer;

public class SharedVertexBuffer {
    public static final VertexBuffer instance = new VertexBuffer(BufferUsage.DYNAMIC_WRITE);
}