package com.zergatul.cheatutils.mixins.common.accessors;

import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractRecipeBookScreen.class)
public interface AbstractRecipeBookScreenAccessor {

    @Invoker("slotClicked")
    void slotClicked_CU(Slot slot, int p_98470_, int p_98471_, ClickType type);
}