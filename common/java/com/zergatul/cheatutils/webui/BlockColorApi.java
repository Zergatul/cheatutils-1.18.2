package com.zergatul.cheatutils.webui;

import com.zergatul.cheatutils.common.Registries;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public class BlockColorApi extends ApiBase {

    @Override
    public String getRoute() {
        return "block-color";
    }

    @Override
    public String get(String id) {
        Block block = Registries.BLOCKS.getValue(ResourceLocation.parse(id));
        int color = Minecraft.getInstance().getBlockColors().getColor(block.defaultBlockState(), null, null, 0);
        return Integer.toString(color);
    }
}