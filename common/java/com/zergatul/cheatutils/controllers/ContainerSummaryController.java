package com.zergatul.cheatutils.controllers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.zergatul.cheatutils.common.Registries;
import com.zergatul.cheatutils.render.Primitives;
import com.zergatul.cheatutils.utils.ItemUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ContainerSummaryController {

    public static final ContainerSummaryController instance = new ContainerSummaryController();

    private static final Minecraft mc = Minecraft.getInstance();

    private ContainerSummaryController() {

    }

    public List<ItemDrawable> groupItems(List<ItemStack> items) {
        Map<Item, ItemDrawable> map = new HashMap<>();
        for (ItemStack itemStack: items) {
            addItem(map, itemStack);

            if (ItemUtils.isShulkerBox(itemStack)) {
                for (ItemStack slot : ItemUtils.getShulkerContent(itemStack)) {
                    if (!slot.isEmpty()) {
                        addItem(map, slot);
                    }
                }
            }

            if (itemStack.getItem() instanceof BundleItem) {
                BundleContents contents = itemStack.get(DataComponents.BUNDLE_CONTENTS);
                assert contents != null;
                for (ItemStack slot : contents.items()) {
                    if (!slot.isEmpty()) {
                        addItem(map, slot);
                    }
                }
            }
        }

        return map.values().stream().sorted((i1, i2) -> {
            int compare = -Integer.compare(i1.count, i2.count);
            if (compare != 0) {
                return compare;
            }
            String id1 = Registries.ITEMS.getKey(i1.item).toString();
            String id2 = Registries.ITEMS.getKey(i2.item).toString();
            return id1.compareTo(id2);
        }).collect(Collectors.toList());
    }

    public List<ItemsColumn> split(List<ItemDrawable> items) {
        List<ItemsColumn> columns = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (columns.size() == 0 || columns.get(columns.size() - 1).list.size() == ItemsColumn.MAX_ROWS) {
                columns.add(new ItemsColumn());
            }
            columns.get(columns.size() - 1).list.add(items.get(i));
        }
        return columns;
    }

    private void addItem(Map<Item, ItemDrawable> map, ItemStack itemStack) {
        Item item = itemStack.getItem();
        int count = itemStack.getCount();
        ItemDrawable drawable = map.get(item);
        if (drawable == null) {
            drawable = new ItemDrawable(item);
        }

        drawable.count += count;
        map.put(item, drawable);
    }

    public static class ItemDrawable {

        private static final int ITEM_PADDING = 4;
        private static final int HEIGHT = 19;

        public Item item;
        public int count;
        public int width;
        private int stackSize;
        private int stacksCount;
        private int remCount;

        public ItemDrawable(Item item) {
            this.item = item;
            this.count = 0;
        }

        public ItemDrawable add(int value) {
            count += value;
            return this;
        }

        public void initDraw(Font font, boolean group) {
            stackSize = group ? ItemUtils.getStackSize(item) : 1;
            stacksCount = count / stackSize;
            remCount = count % stackSize;

            width = 0;

            if (stacksCount > 0) {
                if (stacksCount > 1) {
                    width += font.width(stacksCount + "x");
                }
                width += 16;
                if (remCount > 0) {
                    width += ITEM_PADDING;
                }
            }

            if (remCount > 0) {
                if (stacksCount > 0) {
                    width += font.width("+");
                }
                width += 16;
            }
        }

        public int draw(GuiGraphics graphics, Font font, ItemRenderer itemRenderer, LocalPlayer player, int x, int y) {
            int fy = y + HEIGHT - font.lineHeight - 2;

            if (stacksCount > 0) {
                if (stacksCount > 1) {
                    graphics.drawString(font, stacksCount + "x", x, fy, 16777215);
                    x += font.width(stacksCount + "x");
                }

                ItemStack itemStack = new ItemStack(item, stackSize);
                graphics.renderFakeItem(itemStack, x, y);
                graphics.renderItemDecorations(mc.font, itemStack, x, y);
                x += 16 + ITEM_PADDING;

                if (remCount > 0) {
                    graphics.drawString(font, "+", x, fy, 16777215);
                    x += font.width("+");
                }
            }

            if (remCount > 0) {
                ItemStack itemStack = new ItemStack(item, remCount);
                graphics.renderFakeItem(itemStack, x, y);
                graphics.renderItemDecorations(mc.font, itemStack, x, y);
                //x += 16 + ITEM_PADDING;
            }

            return HEIGHT;
        }
    }

    public static class ItemsColumn {

        private static final int MAX_ROWS = 10;
        private static final int H_PADDING = 2;
        private static final ResourceLocation CONTAINER_TEXTURE = ResourceLocation.parse("textures/gui/container/generic_54.png");

        public List<ItemDrawable> list;
        public int width;

        public ItemsColumn() {
            this.list = new ArrayList<>();
        }

        public void calculateWidth() {
            width = 0;
            for (ItemDrawable drawable: list) {
                if (drawable.width > width) {
                    width = drawable.width;
                }
            }
        }

        public int draw(GuiGraphics graphics, Font font, ItemRenderer itemRenderer, LocalPlayer player, int x, int y) {
            int yo = y;
            int fullWidth = width + H_PADDING * 2;

            Primitives.fill(graphics.pose(), x, y, x + fullWidth, y + ItemDrawable.HEIGHT * list.size(), -1873784752);
            for (ItemDrawable drawable : list) {
                y += drawable.draw(graphics, font, itemRenderer, player, x + (width - drawable.width) / 2 + H_PADDING, y + 2);
            }

            RenderSystem.setShader(CoreShaders.POSITION_TEX);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, CONTAINER_TEXTURE);
            Primitives.drawTexture(graphics.pose().last().pose(), x, yo, 1, ItemDrawable.HEIGHT * list.size() + 1, 124, 3, 3, 1, 1, 256, 256);
            Primitives.drawTexture(graphics.pose().last().pose(), x + fullWidth, yo, 1, ItemDrawable.HEIGHT * list.size() + 1, 124, 3, 3, 1, 1, 256, 256);
            for (int i = 0; i <= list.size(); i++) {
                Primitives.drawTexture(graphics.pose().last().pose(), x, yo + ItemDrawable.HEIGHT * i, fullWidth, 1, 124, 3, 3, 1, 1, 256, 256);
            }

            return fullWidth;
        }
    }
}