package com.zergatul.cheatutils.webui;

import com.zergatul.cheatutils.collections.ImmutableList;
import com.zergatul.cheatutils.common.Registries;
import com.zergatul.cheatutils.configs.BlockEspConfig;
import com.zergatul.cheatutils.configs.BlocksConfig;
import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.modules.esp.BlockFinder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

public class BlocksConfigApi extends ApiBase {

    @Override
    public String getRoute() {
        return "blocks";
    }

    @Override
    public synchronized String get() {
        Object[] result;
        var list = ConfigStore.instance.getConfig().blocks.getBlockConfigs();
        result = list.stream().toArray();
        return gson.toJson(result);
    }

    @Override
    public synchronized String post(String body) throws ApiException {
        BlockEspConfig jsonConfig = gson.fromJson(body, BlockEspConfig.class);
        jsonConfig.validate();

        if (jsonConfig.blocks.size() == 0) {
            throw new ApiException("Received empty BlockEspConfig.", HttpResponseCodes.BAD_REQUEST);
        }

        BlocksConfig blocksConfig = ConfigStore.instance.getConfig().blocks;
        BlockEspConfig config = blocksConfig.findExact(jsonConfig.blocks);
        if (config != null) {
            config.copyFrom(jsonConfig);
        } else {
            List<BlockEspConfig> configs = new ArrayList<>();
            for (Block block: jsonConfig.blocks) {
                config = blocksConfig.find(block);
                if (config != null && !configs.contains(config)) {
                    configs.add(config);
                }
            }

            if (configs.size() > 1) {
                throw new ApiException("Received BlockEspConfig with blocks from multiple already created configs.", HttpResponseCodes.BAD_REQUEST);
            }

            if (configs.size() == 1) {
                config = configs.get(0);
                config.blocks = jsonConfig.blocks;
                config.copyFrom(jsonConfig);
                blocksConfig.refreshMap();
                // re-adding causes rescan
                BlockFinder.instance.removeConfig(config);
                BlockFinder.instance.addConfig(config);
            } else {
                config = BlockEspConfig.createDefault(jsonConfig.blocks);
                blocksConfig.add(config);
            }
        }

        ConfigStore.instance.requestWrite();

        return gson.toJson(config);
    }

    @Override
    public synchronized String delete(String id) throws ApiException {
        ResourceLocation loc = ResourceLocation.parse(id);
        Block block = Registries.BLOCKS.getValue(loc);
        if (block == null) {
            throw new ApiException("Cannot find block by id.", HttpResponseCodes.BAD_REQUEST);
        }

        BlocksConfig blocksConfig = ConfigStore.instance.getConfig().blocks;
        BlockEspConfig config = blocksConfig.find(block);
        if (config != null) {
            blocksConfig.remove(config);
        } else {
            throw new ApiException("Config doesn't exist for this block.", HttpResponseCodes.BAD_REQUEST);
        }

        ConfigStore.instance.requestWrite();

        return "{ \"ok\": true }";
    }

    public static class Add extends ApiBase {

        @Override
        public String getRoute() {
            return "blocks-add";
        }

        @Override
        public String post(String body) throws ApiException {
            String id = gson.fromJson(body, String.class);
            ResourceLocation loc = ResourceLocation.parse(id);
            Block block = Registries.BLOCKS.getValue(loc);
            if (block == null) {
                throw new ApiException("Cannot find block by id.", HttpResponseCodes.BAD_REQUEST);
            }

            BlocksConfig blocksConfig = ConfigStore.instance.getConfig().blocks;
            if (blocksConfig.find(block) != null) {
                throw new ApiException("Selected block is already part of other BlockEspConfig.", HttpResponseCodes.BAD_REQUEST);
            }

            BlockEspConfig config = BlockEspConfig.createDefault(ImmutableList.from(block));
            blocksConfig.add(config);

            ConfigStore.instance.requestWrite();

            return gson.toJson(config);
        }
    }
}