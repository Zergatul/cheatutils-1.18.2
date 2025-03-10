package com.zergatul.cheatutils.scripting.modules;

import com.zergatul.cheatutils.modules.scripting.Containers;
import com.zergatul.cheatutils.scripting.ApiType;
import com.zergatul.cheatutils.scripting.ApiVisibility;
import com.zergatul.cheatutils.scripting.types.ItemStackWrapper;
import com.zergatul.cheatutils.wrappers.ClassRemapper;
import com.zergatul.scripting.MethodDescription;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.core.Holder;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class ContainersApi {

    private static final Minecraft mc = Minecraft.getInstance();

    @MethodDescription("""
            Returns currently opened screen title. Example: "Barrel"
            """)
    public String getScreenTitle() {
        if (mc.screen == null) {
            return "";
        }
        return mc.screen.getTitle().getString();
    }

    @MethodDescription("""
            Returns currently opened container menu id. Id is auto-incremented by the server each time you open container
            """)
    public int getMenuId() {
        AbstractContainerMenu menu = getContainerMenu();
        if (menu == null) {
            return Integer.MIN_VALUE;
        }
        return menu.containerId;
    }

    @MethodDescription("""
            Returns currently opened container menu class. Example: "net.minecraft.world.inventory.ChestMenu"
            """)
    public String getMenuClass() {
        AbstractContainerMenu menu = getContainerMenu();
        if (menu == null) {
            return "";
        }
        return ClassRemapper.fromObf(menu.getClass().getName());
    }

    @MethodDescription("""
            Returns total available slots of currently opened container menu
            """)
    public int getSlotsSize() {
        AbstractContainerMenu menu = getContainerMenu();
        if (menu == null) {
            return Integer.MIN_VALUE;
        }
        return menu.slots.size();
    }

    @MethodDescription("""
            Check if value is valid slot index. Valid slot indexes: 0..(getSlotsSize() - 1)
            """)
    public boolean isValidSlotIndex(int index) {
        AbstractContainerMenu menu = getContainerMenu();
        if (menu == null) {
            return false;
        }
        return menu.isValidSlotIndex(index);
    }

    @MethodDescription("""
            Returns true if slot has some item
            """)
    public boolean hasItemAtSlot(int index) {
        Slot slot = getSlot(index);
        return slot != null && slot.hasItem();
    }

    @MethodDescription("""
            Returns item id at specified slot
            """)
    public ItemStackWrapper getItemAtSlot(int index) {
        Slot slot = getSlot(index);
        if (slot == null) {
            return new ItemStackWrapper(ItemStack.EMPTY);
        }

        return new ItemStackWrapper(slot.getItem());
    }

    @MethodDescription("""
            Emulates click on specified slot.
            button parameter:
                0=left
                1=right
                2=middle
            clickType parameter allowed values:
                "PICKUP"
                "QUICK_MOVE"
                "SWAP"
                "CLONE"
                "THROW"
                "QUICK_CRAFT"
                "PICKUP_ALL"
            """)
    @ApiVisibility(ApiType.ACTION)
    public boolean click(int slot, int button, String clickType) {
        if (mc.player == null) {
            return false;
        }
        if (mc.gameMode == null) {
            return false;
        }

        AbstractContainerMenu menu = getContainerMenu();
        if (menu == null) {
            return false;
        }

        ClickType type;
        try {
            type = ClickType.valueOf(clickType);
        } catch (IllegalArgumentException e) {
            return false;
        }

        mc.gameMode.handleInventoryMouseClick(menu.containerId, slot, button, type, mc.player);
        return true;
    }

    @MethodDescription("""
            Waits for container menu screen, example open chest menu
            """)
    public CompletableFuture<Void> waitForOpen() {
        return Containers.instance.waitForOpen();
    }

    public void close() {
        Containers.instance.close();
    }

    @MethodDescription("""
            Waits for container menu id. Can be useful when interactive with custom server menus.
            If there is no container menu, script will stop here
            """)
    public CompletableFuture<Void> waitForNewId() {
        return Containers.instance.waitForNewId();
    }

    private Slot getSlot(int index) {
        AbstractContainerMenu menu = getContainerMenu();
        if (menu == null) {
            return null;
        }
        if (index < 0 || index >= menu.slots.size()) {
            return null;
        }
        return menu.getSlot(index);
    }

    private AbstractContainerMenu getContainerMenu() {
        Screen screen = mc.screen;
        if (screen == null) {
            return null;
        }
        if (screen instanceof MenuAccess<?> menuAccess) {
            return menuAccess.getMenu();
        } else {
            return null;
        }
    }

    private String[] getEnchantmentIds(ItemEnchantments enchantments) {
        List<String> ids = new ArrayList<>();
        for (Holder<Enchantment> holder : enchantments.keySet()) {
            holder.unwrapKey().ifPresent(enchantment -> ids.add(enchantment.location().toString()));
            return ids.toArray(String[]::new);
        }
        return new String[0];
    }

    private int[] getEnchantmentLevels(ItemEnchantments enchantments) {
        List<Integer> levels = new ArrayList<>();
        for (Holder<Enchantment> holder : enchantments.keySet()) {
            if (holder.unwrapKey().isPresent()) {
                levels.add(enchantments.getLevel(holder));
            }
            return levels.stream().mapToInt(Integer::intValue).toArray();
        }
        return new int[0];
    }
}