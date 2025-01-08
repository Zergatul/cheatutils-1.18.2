package com.zergatul.cheatutils.utils;

import com.zergatul.cheatutils.configs.BlockPlacerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.Arrays;
import java.util.function.Predicate;

public class SlotSelector {

    private final Minecraft mc = Minecraft.getInstance();
    private final long[] lastSlotUsage = new long[9];

    public SlotSelector() {
        Arrays.fill(lastSlotUsage, Long.MIN_VALUE);
    }

    public int selectBlock(BlockPlacerConfig config, Block block) {
        if (mc.player == null) {
            return -1;
        }

        Inventory inventory = mc.player.getInventory();

        // search on hotbar
        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getItem() instanceof BlockItem blockItem) {
                if (blockItem.getBlock() == block) {
                    lastSlotUsage[i] = System.nanoTime();
                    return i;
                }
            }
        }

        if (config.autoSelectSlots.length == 0) {
            return -1;
        }

        // search in inventory
        for (int i = 9; i < 36; i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getItem() instanceof BlockItem blockItem) {
                if (blockItem.getBlock() == block) {
                    long minTime = Long.MAX_VALUE;
                    int minSlot = config.autoSelectSlots[0];
                    for (int slot : config.autoSelectSlots) {
                        if (lastSlotUsage[slot - 1] < minTime) {
                            minTime = lastSlotUsage[slot - 1];
                            minSlot = slot - 1;
                        }
                    }

                    InventoryUtils.moveItemStack(new InventorySlot(i), new InventorySlot(minSlot));
                    lastSlotUsage[minSlot] = System.nanoTime();
                    return minSlot;
                }
            }
        }

        return -1;
    }

    public int selectItem(BlockPlacerConfig config, Item item) {
        return selectItem(config, item, stack -> true);
    }

    public int selectItem(BlockPlacerConfig config, Item item, Predicate<ItemStack> predicate) {
        if (mc.player == null) {
            return -1;
        }

        Inventory inventory = mc.player.getInventory();

        // check if we are holding correct item
        if (inventory.getSelected().is(item) && predicate.test(inventory.getSelected())) {
            lastSlotUsage[inventory.selected] = System.nanoTime();
            return inventory.selected;
        }

        // search on hotbar
        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.is(item) && predicate.test(itemStack)) {
                lastSlotUsage[i] = System.nanoTime();
                return i;
            }
        }

        if (config.autoSelectSlots.length == 0) {
            return -1;
        }

        // search in inventory
        for (int i = 9; i < 36; i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.is(item) && predicate.test(itemStack)) {
                long minTime = Long.MAX_VALUE;
                int minSlot = config.autoSelectSlots[0];
                for (int slot : config.autoSelectSlots) {
                    if (lastSlotUsage[slot - 1] < minTime) {
                        minTime = lastSlotUsage[slot - 1];
                        minSlot = slot - 1;
                    }
                }

                InventoryUtils.moveItemStack(new InventorySlot(i), new InventorySlot(minSlot));
                lastSlotUsage[minSlot] = System.nanoTime();
                return minSlot;
            }
        }

        return -1;
    }
}