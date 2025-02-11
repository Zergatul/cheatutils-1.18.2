package com.zergatul.cheatutils.render;

import com.zergatul.cheatutils.common.events.RenderWorldLastEvent;
import com.zergatul.cheatutils.render.gl.BlockOverlayBufferProgram;
import com.zergatul.cheatutils.render.gl.FrameBuffer;
import com.zergatul.cheatutils.render.gl.OverlayDrawProgram;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL30;

public class BlockOverlayRenderer {

    private BlockOverlayBufferProgram bufferProgram;
    private OverlayDrawProgram drawProgram;
    private RenderWorldLastEvent event;
    private Vec3 view;

    public void begin(RenderWorldLastEvent event) {
        if (this.event != null) {
            throw new IllegalStateException("Rendered is already active");
        }

        this.event = event;
        this.view = event.getCamera().getPosition();

        createGlObjectsIfRequired();

        bufferProgram.buffer.clear();
        drawProgram.buffer.clear();
    }

    public void quad(
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            double x3, double y3, double z3,
            double x4, double y4, double z4
    ) {
        triangle(
                x1, y1, z1,
                x2, y2, z2,
                x4, y4, z4);
        triangle(
                x2, y2, z2,
                x3, y3, z3,
                x4, y4, z4);
    }

    public void triangle(
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            double x3, double y3, double z3
    ) {
        bufferProgram.buffer.add((float) (x1 - view.x));
        bufferProgram.buffer.add((float) (y1 - view.y));
        bufferProgram.buffer.add((float) (z1 - view.z));

        bufferProgram.buffer.add((float) (x2 - view.x));
        bufferProgram.buffer.add((float) (y2 - view.y));
        bufferProgram.buffer.add((float) (z2 - view.z));

        bufferProgram.buffer.add((float) (x3 - view.x));
        bufferProgram.buffer.add((float) (y3 - view.y));
        bufferProgram.buffer.add((float) (z3 - view.z));
    }

    public void end(float red, float green, float blue, float alpha) {
        renderInFrameBuffer();

        // set line settings
        GL30.glEnable(GL30.GL_BLEND);
        GL30.glBlendFuncSeparate(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA, GL30.GL_ONE, GL30.GL_ZERO);
        GL30.glDisable(GL30.GL_DEPTH_TEST);

        // draw with shader program
        drawProgram.buffer.add(-1);
        drawProgram.buffer.add(-1);
        drawProgram.buffer.add(0);
        drawProgram.buffer.add(0);
        drawProgram.buffer.add(0);

        drawProgram.buffer.add(1);
        drawProgram.buffer.add(-1);
        drawProgram.buffer.add(0);
        drawProgram.buffer.add(1);
        drawProgram.buffer.add(0);

        drawProgram.buffer.add(-1);
        drawProgram.buffer.add(1);
        drawProgram.buffer.add(0);
        drawProgram.buffer.add(0);
        drawProgram.buffer.add(1);

        drawProgram.buffer.add(1);
        drawProgram.buffer.add(1);
        drawProgram.buffer.add(0);
        drawProgram.buffer.add(1);
        drawProgram.buffer.add(1);

        drawProgram.buffer.add(-1);
        drawProgram.buffer.add(1);
        drawProgram.buffer.add(0);
        drawProgram.buffer.add(0);
        drawProgram.buffer.add(1);

        drawProgram.buffer.add(1);
        drawProgram.buffer.add(-1);
        drawProgram.buffer.add(0);
        drawProgram.buffer.add(1);
        drawProgram.buffer.add(0);

        drawProgram.draw(FrameBuffers.get1(), red, green, blue, alpha);

        // reset settings
        GL30.glDisable(GL30.GL_BLEND);
        GL30.glEnable(GL30.GL_DEPTH_TEST);

        // reset renderer state
        this.event = null;
        this.view = null;

        // upload vertex data to buffer
        /*GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, VBO);
        GLNative.glBufferData(GL20.GL_ARRAY_BUFFER, glBuffer.size() * 4, glBuffer.get(), GL20.GL_DYNAMIC_DRAW);
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0);

        // set draw settings
        GL30.glEnable(GL30.GL_BLEND);
        GL30.glBlendFuncSeparate(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA, GL30.GL_ONE, GL30.GL_ZERO);
        GL30.glDisable(GL30.GL_DEPTH_TEST);
        GL30.glDisable(GL30.GL_CULL_FACE); // ?????

        // prepare program
        GL30.glUseProgram(framebufferProgram.getId());

        float[] matrix = new float[16];
        event.getPoseMatrix().get(matrix);
        GL30.glUniformMatrix4fv(modelViewLocation, false, matrix);

        matrix = new float[16];
        event.getProjectionMatrix().get(matrix);
        GL30.glUniformMatrix4fv(projectionLocation, false, matrix);

        // draw triangles into framebuffer
        int prevFBO = GL30.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, FBO);
        GL30.glClearColor(0, 0, 0, 0);
        GL30.glClear(GL30.GL_COLOR_BUFFER_BIT);
        GL30.glBindVertexArray(VAO);
        GL30.glDrawArrays(GL30.GL_TRIANGLES, 0, triangles * 3);
        GL30.glBindVertexArray(0);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFBO);

        // reset settings
        GL30.glDisable(GL30.GL_BLEND);
        GL30.glEnable(GL30.GL_DEPTH_TEST);
        GL30.glEnable(GL30.GL_CULL_FACE); // ?????*/

        // reset renderer state
        /*this.view = null;
        this.event = null;*/
    }

    public void close() {
        bufferProgram.delete();
        drawProgram.delete();
    }

    private void renderInFrameBuffer() {
        FrameBuffer.push();

        // set draw settings
        GL30.glEnable(GL30.GL_BLEND);
        GL30.glBlendFuncSeparate(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA, GL30.GL_ONE, GL30.GL_ZERO);
        GL30.glDisable(GL30.GL_DEPTH_TEST);
        //GL30.glDisable(GL30.GL_CULL_FACE); // ?????
        GL30.glEnable(GL30.GL_CULL_FACE);

        // draw with shader program in framebuffer
        FrameBuffers.get1().bind();
        GL30.glClearColor(0, 0, 0, 0);
        GL30.glClear(GL30.GL_COLOR_BUFFER_BIT);
        bufferProgram.draw(event.getMvp());

        // restore settings
        GL30.glDisable(GL30.GL_BLEND);
        GL30.glEnable(GL30.GL_DEPTH_TEST);
        //GL30.glEnable(GL30.GL_CULL_FACE); // ?????

        FrameBuffer.pop();
    }

    private void createGlObjectsIfRequired() {
        if (bufferProgram == null) {
            bufferProgram = new BlockOverlayBufferProgram();
            drawProgram = new OverlayDrawProgram();
        }
    }
}