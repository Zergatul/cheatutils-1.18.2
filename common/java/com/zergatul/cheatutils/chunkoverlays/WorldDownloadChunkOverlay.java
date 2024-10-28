package com.zergatul.cheatutils.chunkoverlays;

import com.mojang.blaze3d.platform.NativeImage;
import com.zergatul.cheatutils.concurrent.PreRenderGuiExecutor;
import com.zergatul.cheatutils.utils.Dimension;
import com.zergatul.cheatutils.controllers.WorldDownloadController;
import net.minecraft.world.level.ChunkPos;

import java.util.Map;

public class WorldDownloadChunkOverlay extends AbstractChunkOverlay {

    private final NativeImage texture;

    public WorldDownloadChunkOverlay(int segmentSize, long updateDelay) {
        super(segmentSize, updateDelay);

        texture = loadImage("textures/worlddownload/dl.png");
    }

    @Override
    public int getTranslateZ() {
        return 102;
    }

    @Override
    public boolean isEnabled() {
        return WorldDownloadController.instance.isActive();
    }

    public void notifyChunkSaved(Dimension dimension, int x, int z) {
        PreRenderGuiExecutor.instance.execute(() -> {
            ChunkPos chunkPos = new ChunkPos(x, z);
            SegmentPos segmentPos = new SegmentPos(chunkPos, segmentSize);
            Map<SegmentPos, Segment> segments = getSegmentsMap(dimension);
            if (!segments.containsKey(segmentPos)) {
                segments.put(segmentPos, new Segment(segmentPos, segmentSize));
            }
            Segment segment = segments.get(segmentPos);
            int xf = Math.floorMod(chunkPos.x, segmentSize) * 16;
            int yf = Math.floorMod(chunkPos.z, segmentSize) * 16;

            // clear pixels
            for (int dx = 0; dx < 16; dx++) {
                for (int dy = 0; dy < 16; dy++) {
                    segment.image.setPixel(xf + dx, yf + dy, 0);
                }
            }

            for (int dx = 0; dx < 16; dx++) {
                for (int dy = 0; dy < 16; dy++) {
                    segment.image.setPixel(xf + dx, yf + dy, texture.getPixel(dx, dy));
                }
            }

            segment.onChange();
        });
    }
}