package com.zergatul.cheatutils.mixins.fabric;

import com.zergatul.cheatutils.fabric.GuiGraphicsHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreen {

    @Shadow
    protected Slot hoveredSlot;

    @Inject(
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/resources/ResourceLocation;)V"),
            method = "renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;II)V")
    private void onBeforeRenderTooltip(GuiGraphics graphics, int x, int y, CallbackInfo info) {
        GuiGraphicsHelper.tooltipItemStack = this.hoveredSlot.getItem();
    }

    @Inject(
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/resources/ResourceLocation;)V", shift = At.Shift.AFTER),
            method = "renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;II)V")
    private void onAfterRenderTooltip(GuiGraphics graphics, int x, int y, CallbackInfo info) {
        GuiGraphicsHelper.tooltipItemStack = null;
    }
}