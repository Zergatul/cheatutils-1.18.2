package com.zergatul.cheatutils.mixins.fabric;

import com.zergatul.cheatutils.modules.hacks.InvMove;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class MixinKeyboardHandler {

    @Redirect(
            method = "keyPress(JIIII)V",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;", opcode = Opcodes.GETFIELD, ordinal = 2))
    private Screen onCheckScreenPassEvents(Minecraft mc) {
        return InvMove.instance.overrideGetScreen(mc);
    }

    /*@Inject(
            method = "method_1454(ILnet/minecraft/client/gui/screens/Screen;[ZIII)V",
            at = @At("TAIL"))
    private static void onScreenHandleKeys(int code, Screen screen, boolean[] resultHack, int key, int scancode, int modifiers, CallbackInfo ci) {
        if (InvMove.instance.shouldPassEvents(screen)) {
            if (Minecraft.getInstance().screen != null) {
                resultHack[0] = false;
            }
        }
    }*/
}