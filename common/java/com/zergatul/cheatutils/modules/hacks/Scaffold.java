package com.zergatul.cheatutils.modules.hacks;

import com.zergatul.cheatutils.common.Events;
import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.configs.ScaffoldConfig;
import com.zergatul.cheatutils.modules.Module;
import com.zergatul.cheatutils.utils.InventorySlot;
import com.zergatul.cheatutils.utils.InventoryUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Scaffold implements Module {

    public static final Scaffold instance = new Scaffold();

    private final Minecraft mc = Minecraft.getInstance();
    private final List<BlockPos> list = new ArrayList<>();

    private Scaffold() {
        Events.ClientTickEnd.add(this::onClientTickEnd);
    }

    private void onClientTickEnd() {
        if (mc.player == null || mc.level == null) {
            return;
        }
        ScaffoldConfig config = ConfigStore.instance.getConfig().scaffoldConfig;
        if (config.enabled) {
            double x = mc.player.getX();
            double y = mc.player.getY();
            double z = mc.player.getZ();

            int yb = Mth.floor(y) - 1;

            list.clear();
            list.add(new BlockPos(Mth.floor(x), yb, Mth.floor(z)));

            if (config.distance > 0) {
                BlockPos bp1 = new BlockPos(new BlockPos(Mth.floor(x + config.distance), yb, Mth.floor(z)));
                if (!list.contains(bp1)) {
                    list.add(bp1);
                }

                BlockPos bp2 = new BlockPos(new BlockPos(Mth.floor(x - config.distance), yb, Mth.floor(z)));
                if (!list.contains(bp2)) {
                    list.add(bp2);
                }

                BlockPos bp3 = new BlockPos(new BlockPos(Mth.floor(x), yb, Mth.floor(z - config.distance)));
                if (!list.contains(bp3)) {
                    list.add(bp3);
                }

                BlockPos bp4 = new BlockPos(new BlockPos(Mth.floor(x), yb, Mth.floor(z + config.distance)));
                if (!list.contains(bp4)) {
                    list.add(bp4);
                }
            }

            boolean placed = false;
            for (BlockPos bp: list) {
                if (canPlaceBlock(mc.level.getBlockState(bp))) {
                    for (Direction direction: Direction.values()) {
                        BlockPos other = bp.relative(direction);
                        BlockState state = mc.level.getBlockState(other);
                        if (!state.getShape(mc.level, other).isEmpty()) {
                            placeBlock(bp, direction.getOpposite(), other, config);
                            placed = true;
                            break;
                        }
                    }
                    if (placed) {
                        break;
                    }
                }
            }

            if (!placed && config.attachToAir) {
                for (BlockPos bp: list) {
                    if (canPlaceBlock(mc.level.getBlockState(bp))) {
                        BlockPos other = bp.relative(Direction.DOWN);
                        placeBlock(bp, Direction.UP, other, config);
                        break;
                    }
                }
            }
        }
    }

    private boolean canPlaceBlock(BlockState state) {
        return state.canBeReplaced();
    }

    private void placeBlock(BlockPos destination, Direction direction, BlockPos neighbour, ScaffoldConfig config) {
        int selectedSlot = mc.player.getInventory().selected;
        Optional<InteractionHand> optional = selectItem(config);
        if (optional.isEmpty()) {
            return;
        }

        InteractionHand hand = optional.get();
        ItemStack itemStack = mc.player.getItemInHand(hand);
        Item item = itemStack.getItem();
        //Block block = ((BlockItem) item).getBlock();
        //boolean isSlab = block instanceof SlabBlock;

        Vec3 location = new Vec3(
                destination.getX() + 0.5f + direction.getOpposite().getStepX() * 0.5,
                destination.getY() + 0.5f /*+ (block instanceof SlabBlock ? 0.25f : 0)*/ + direction.getOpposite().getStepY() * 0.5,
                destination.getZ() + 0.5f + direction.getOpposite().getStepZ() * 0.5);
        BlockHitResult hit = new BlockHitResult(location, direction, neighbour, false);

        InteractionResult result = mc.gameMode.useItemOn(mc.player, hand, hit);
        if (result.consumesAction()) {
            if (result instanceof InteractionResult.Success success && success.swingSource() == InteractionResult.SwingSource.CLIENT) {
                mc.player.swing(hand);
            }
        }

        if (config.keepSelectedSlot) {
            mc.player.getInventory().selected = selectedSlot;
        }

        if (itemStack.isEmpty() && config.replaceBlocksFromInventory) {
            for (int i = 9; i < 36; i++) {
                ItemStack itemStack1 = mc.player.getInventory().getItem(i);
                if (!itemStack1.isEmpty() && item == itemStack1.getItem()) {
                    InventoryUtils.moveItemStack(new InventorySlot(i), hand == InteractionHand.MAIN_HAND ? new InventorySlot(mc.player.getInventory().selected) : new InventorySlot(EquipmentSlot.OFFHAND));
                    return;
                }
            }
        }
    }

    private Optional<InteractionHand> selectItem(ScaffoldConfig config) {
        Item mainHandItem = mc.player.getItemInHand(InteractionHand.MAIN_HAND).getItem();
        if (mainHandItem instanceof BlockItem blockItem && isValidBlock(blockItem.getBlock(), config)) {
            return Optional.of(InteractionHand.MAIN_HAND);
        }

        Item offHandItem = mc.player.getItemInHand(InteractionHand.OFF_HAND).getItem();
        if (offHandItem instanceof BlockItem blockItem && isValidBlock(blockItem.getBlock(), config)) {
            return Optional.of(InteractionHand.OFF_HAND);
        }

        // find item on hotbar
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.getItem() instanceof BlockItem blockItem) {
                if (isValidBlock(blockItem.getBlock(), config)) {
                    mc.player.getInventory().selected = i;
                    return Optional.of(InteractionHand.MAIN_HAND);
                }
            }
        }

        return Optional.empty();
    }

    private boolean isValidBlock(Block block, ScaffoldConfig config) {
        /*if (block instanceof SlabBlock) {
            return config.useSlabs;
        }*/
        if (!block.defaultBlockState().isCollisionShapeFullBlock(mc.level, new BlockPos(0, 0, 0))) {
            return false;
        }
        if (block instanceof FallingBlock) {
            return false;
        }
        return true;
    }
}