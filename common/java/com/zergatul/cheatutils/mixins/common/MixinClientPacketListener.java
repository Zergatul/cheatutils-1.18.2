package com.zergatul.cheatutils.mixins.common;

import com.mojang.brigadier.ParseResults;
import com.zergatul.cheatutils.common.Events;
import com.zergatul.cheatutils.common.events.SendChatEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener {

    @Shadow
    public abstract Connection getConnection();

    @ModifyVariable(
            at = @At("HEAD"),
            method = "handleLogin(Lnet/minecraft/network/protocol/game/ClientboundLoginPacket;)V",
            argsOnly = true,
            ordinal = 0)
    private ClientboundLoginPacket onModifyLoginPacket(ClientboundLoginPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        return new ClientboundLoginPacket(
                packet.playerId(),
                packet.hardcore(),
                packet.levels(),
                packet.maxPlayers(),
                mc.options.renderDistance().get(),
                mc.options.simulationDistance().get(),
                packet.reducedDebugInfo(),
                packet.showDeathScreen(),
                packet.doLimitedCrafting(),
                packet.commonPlayerSpawnInfo(),
                packet.enforcesSecureChat());
    }

    @ModifyVariable(
            at = @At("HEAD"),
            method = "handleSetChunkCacheRadius(Lnet/minecraft/network/protocol/game/ClientboundSetChunkCacheRadiusPacket;)V",
            argsOnly = true,
            ordinal = 0)
    private ClientboundSetChunkCacheRadiusPacket onModifySetChunkCacheRadiusPacket(ClientboundSetChunkCacheRadiusPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        return new ClientboundSetChunkCacheRadiusPacket(mc.options.renderDistance().get());
    }

    @Inject(
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundLoginPacket;playerId()I"),
            method = "handleLogin(Lnet/minecraft/network/protocol/game/ClientboundLoginPacket;)V")
    private void onPlayerLoggingIn(ClientboundLoginPacket packet, CallbackInfo info) {
        Events.ClientPlayerLoggingIn.trigger(this.getConnection());
    }

    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void onSendChat(String message, CallbackInfo info) {
        if (Events.SendChat.trigger(new SendChatEvent(message))) {
            info.cancel();
        }
    }
}