package com.zergatul.cheatutils.webui;

import com.zergatul.cheatutils.common.Registries;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Collection;

public class BlockInfoApi extends ApiBase {

    @Override
    public String getRoute() {
        return "block-info";
    }

    @Override
    public String get() {
        Collection<Block> blocks = Registries.BLOCKS.getValues();
        Object[] result = blocks.stream().filter(b -> !b.equals(Blocks.AIR)).map(BlockInfo::new).toArray();
        return gson.toJson(result);
    }

    private static class BlockInfo {

        public String id;
        public String name;

        public BlockInfo(Block block) {
            id = Registries.BLOCKS.getKey(block).toString();

            MutableComponent text = block.getName();
            if (text.getContents() instanceof TranslatableContents) {
                name = I18n.get(((TranslatableContents) text.getContents()).getKey());
            } else {
                name = id;
            }
        }
    }
}