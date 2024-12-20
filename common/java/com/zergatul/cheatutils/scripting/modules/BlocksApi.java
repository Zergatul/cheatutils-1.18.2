package com.zergatul.cheatutils.scripting.modules;

import com.zergatul.cheatutils.common.Registries;
import com.zergatul.cheatutils.configs.BlockEspConfig;
import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.modules.esp.BlockFinder;
import com.zergatul.cheatutils.scripting.ApiType;
import com.zergatul.cheatutils.scripting.ApiVisibility;
import com.zergatul.cheatutils.scripting.types.BlockPosWrapper;
import com.zergatul.cheatutils.utils.ColorUtils;
import com.zergatul.scripting.MethodDescription;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.awt.*;
import java.util.Set;

@SuppressWarnings("unused")
public class BlocksApi {

    @MethodDescription("""
            Checks if block is enabled. If block is part of a group, returns status of this group
            """)
    public boolean isEnabled(String blockId) {
        BlockEspConfig config = getConfig(blockId);
        if (config == null) {
            return false;
        }
        return config.enabled;
    }

    @MethodDescription("""
            Toggles block enabled status. If block is part of a group, toggles status of entire group
            """)
    @ApiVisibility(ApiType.UPDATE)
    public void toggle(String blockId) {
        BlockEspConfig config = getConfig(blockId);
        if (config == null) {
            return;
        }

        config.enabled = !config.enabled;
        ConfigStore.instance.requestWrite();
    }

    @ApiVisibility(ApiType.UPDATE)
    public void setTracerColor(String blockId, String color) {
        Color colorValue = ColorUtils.parseColor2(color);
        if (colorValue == null) {
            return;
        }

        BlockEspConfig config = getConfig(blockId);
        if (config == null) {
            return;
        }

        config.tracerColor = colorValue;
        ConfigStore.instance.requestWrite();
    }

    @ApiVisibility(ApiType.UPDATE)
    public void setBoundingBoxColor(String blockId, String color) {
        Color colorValue = ColorUtils.parseColor2(color);
        if (colorValue == null) {
            return;
        }

        BlockEspConfig config = getConfig(blockId);
        if (config == null) {
            return;
        }

        config.outlineColor = colorValue;
        ConfigStore.instance.requestWrite();
    }

    @ApiVisibility(ApiType.UPDATE)
    public void setOverlayColor(String blockId, String color) {
        Color colorValue = ColorUtils.parseColor2(color);
        if (colorValue == null) {
            return;
        }

        BlockEspConfig config = getConfig(blockId);
        if (config == null) {
            return;
        }

        config.overlayColor = colorValue;
        ConfigStore.instance.requestWrite();
    }

    @MethodDescription("""
            Rescans chunks. Use it when you face some problems from Block ESP. Normally you should not have problems
            """)
    @ApiVisibility(ApiType.UPDATE)
    public void rescan() {
        BlockFinder.instance.rescan();
    }

    @MethodDescription("""
            Returns blocks count which are tracked by Block ESP. If block is part of a group, returns count of entire group
            """)
    public int getCount(String blockId) {
        ResourceLocation location = ResourceLocation.parse(blockId);
        Block block = Registries.BLOCKS.getValue(location);
        if (block == null) {
            return Integer.MIN_VALUE;
        }

        BlockEspConfig config = ConfigStore.instance.getConfig().blocks.find(block);
        if (config == null) {
            return 0;
        }

        Set<BlockPos> set = BlockFinder.instance.blocks.get(config);
        if (set == null) {
            return 0;
        } else {
            return set.size();
        }
    }

    @MethodDescription("""
            Removes block from Block ESP and prevents future Block ESP at this coordinates.
            Black list is not persistent and it is cleared when you restart Minecraft.
            """)
    public void addBlackList(BlockPosWrapper pos) {
        addBlackList(pos.getX(), pos.getY(), pos.getZ());
    }

    @MethodDescription("""
            Removes block from Block ESP and prevents future Block ESP at this coordinates.
            Black list is not persistent and it is cleared when you restart Minecraft.
            """)
    public void addBlackList(int x, int y, int z) {
        BlockFinder.instance.addBlackList(new BlockPos(x, y, z));
    }

    @MethodDescription("""
            Clears black list with block coordinates. This action does not re-add blocks that match Block ESP conditions at these coordinates.
            """)
    public void clearBlackList() {
        BlockFinder.instance.blackList.clear();
    }

    private BlockEspConfig getConfig(String blockId) {
        ResourceLocation location = ResourceLocation.parse(blockId);
        Block block = Registries.BLOCKS.getValue(location);
        if (block == null) {
            return null;
        }

        return ConfigStore.instance.getConfig().blocks.find(block);
    }
}