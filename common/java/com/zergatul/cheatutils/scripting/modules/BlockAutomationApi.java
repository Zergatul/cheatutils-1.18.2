package com.zergatul.cheatutils.scripting.modules;

import com.zergatul.cheatutils.configs.BlockAutomationConfig;
import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.modules.scripting.BlockAutomation;
import com.zergatul.cheatutils.scripting.ApiType;
import com.zergatul.cheatutils.scripting.ApiVisibility;
import com.zergatul.cheatutils.utils.BlockPlacingMethod;
import com.zergatul.scripting.MethodDescription;
import net.minecraft.world.InteractionHand;

@SuppressWarnings("unused")
public class BlockAutomationApi extends ModuleApi<BlockAutomationConfig> {

    @MethodDescription("""
            For debugging
            """)
    public void useOne() {
        BlockAutomation.instance.placeOne();
    }

    @MethodDescription("""
            Uses specified item from your inventory at current coordinates
            """)
    @ApiVisibility(ApiType.BLOCK_AUTOMATION)
    public void useItem(String itemId) {
        BlockAutomation.instance.useItem(itemId, BlockPlacingMethod.ANY);
    }

    @MethodDescription("""
            Uses one specified item from your inventory at current coordinates.
            If first item is missing in your inventory, it will try to use second, and so on.
            """)
    @ApiVisibility(ApiType.BLOCK_AUTOMATION)
    public void useItem(String[] itemIds) {
        BlockAutomation.instance.useItem(itemIds, BlockPlacingMethod.ANY);
    }

    @MethodDescription(value = """
            Uses one specified item from your inventory at current coordinates.
            "method" parameter specifies custom way to use block. Allowed values:
                - "bottom-slab"
                - "top-slab"
                - "facing-top"    // for blocks like piston
                - "facing-bottom"
                - "facing-north"
                - "facing-south"
                - "facing-east"
                - "facing-west"
                - "from-top"      // for items like seeds
                - "from-bottom"
                - "from-horizontal"
                - "item-use"      // for items like bonemeal
                - "air-place"
            """)
    @ApiVisibility(ApiType.BLOCK_AUTOMATION)
    public void useItem(String itemId, String method) {
        BlockAutomation.instance.useItem(itemId, parseMethod(method));
    }

    @ApiVisibility(ApiType.BLOCK_AUTOMATION)
    public void useWithMainHand() {
        BlockAutomation.instance.useItem(InteractionHand.MAIN_HAND, BlockPlacingMethod.ANY);
    }

    @ApiVisibility(ApiType.BLOCK_AUTOMATION)
    public void useWithMainHand(String method) {
        BlockAutomation.instance.useItem(InteractionHand.MAIN_HAND, parseMethod(method));
    }

    @ApiVisibility(ApiType.BLOCK_AUTOMATION)
    public void useWithOffHand() {
        BlockAutomation.instance.useItem(InteractionHand.OFF_HAND, BlockPlacingMethod.ANY);
    }

    @ApiVisibility(ApiType.BLOCK_AUTOMATION)
    public void useWithOffHand(String method) {
        BlockAutomation.instance.useItem(InteractionHand.OFF_HAND, parseMethod(method));
    }

    @MethodDescription("""
            Breaks block with currently equipped item
            """)
    @ApiVisibility(ApiType.BLOCK_AUTOMATION)
    public void breakBlock() {
        BlockAutomation.instance.breakBlock(null, null);
    }

    @MethodDescription("""
            Breaks block with item id you specify
            """)
    @ApiVisibility(ApiType.BLOCK_AUTOMATION)
    public void breakBlock(String itemId) {
        BlockAutomation.instance.breakBlock(itemId, null);
    }

    @MethodDescription("""
            Breaks block with item id and enchantment id you specify
            """)
    @ApiVisibility(ApiType.BLOCK_AUTOMATION)
    public void breakBlock(String itemId, String enchantmentId) {
        BlockAutomation.instance.breakBlock(itemId, enchantmentId);
    }

    private BlockPlacingMethod parseMethod(String value) {
        return switch (value) {
            case "bottom-slab" -> BlockPlacingMethod.BOTTOM_SLAB;
            case "top-slab" -> BlockPlacingMethod.TOP_SLAB;
            case "facing-top" -> BlockPlacingMethod.FACING_TOP;
            case "facing-bottom" -> BlockPlacingMethod.FACING_BOTTOM;
            case "facing-north" -> BlockPlacingMethod.FACING_NORTH;
            case "facing-south" -> BlockPlacingMethod.FACING_SOUTH;
            case "facing-east" -> BlockPlacingMethod.FACING_EAST;
            case "facing-west" -> BlockPlacingMethod.FACING_WEST;
            case "from-top" -> BlockPlacingMethod.FROM_TOP;
            case "from-bottom" -> BlockPlacingMethod.FROM_BOTTOM;
            case "from-horizontal" -> BlockPlacingMethod.FROM_HORIZONTAL;
            case "item-use" -> BlockPlacingMethod.ITEM_USE;
            case "air-place" -> BlockPlacingMethod.AIR_PLACE;
            default -> BlockPlacingMethod.ANY;
        };
    }

    @Override
    protected BlockAutomationConfig getConfig() {
        return ConfigStore.instance.getConfig().blockAutomationConfig;
    }
}