package com.zergatul.cheatutils.modules.visuals;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ArmorOverlay {

    public static final ArmorOverlay instance = new ArmorOverlay();

    private ArmorOverlay() {

    }

    public void render(GuiGraphics graphics, Player player, int left, int top) {
        List<ItemStack> armor = player.getInventory().armor;
        graphics.renderItem(armor.get(3), left, top);
        left += 16;
        graphics.renderItem(armor.get(2), left, top);
        left += 16;
        graphics.renderItem(armor.get(1), left, top);
        left += 16;
        graphics.renderItem(armor.get(0), left, top);
    }
}