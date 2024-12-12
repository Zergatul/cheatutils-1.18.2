package com.zergatul.cheatutils.modules.automation;

import com.zergatul.cheatutils.common.Events;
import com.zergatul.cheatutils.configs.AutoFishConfig;
import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.controllers.NetworkPacketsController;
import com.zergatul.cheatutils.mixins.common.accessors.MinecraftAccessor;
import com.zergatul.cheatutils.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class AutoFish implements Module {

    public static final AutoFish instance = new AutoFish();

    private static final long PULL_IN_DELAY_MS = 500;

    private final Minecraft mc = Minecraft.getInstance();
    private final Logger logger = LogManager.getLogger(AutoFish.class);
    private volatile State state;
    private Optional<FishingHook> bobber = Optional.empty();
    private long lastPullInTime;

    private AutoFish() {
        NetworkPacketsController.instance.addServerPacketHandler(this::onServerPacket);
        Events.ClientTickEnd.add(this::onClientTickEnd);
    }

    private void onClientTickEnd() {
        if (mc.player == null) {
            state = State.NONE;
            return;
        }

        AutoFishConfig config = ConfigStore.instance.getConfig().autoFishConfig;
        if (!config.enabled) {
            state = State.NONE;
            return;
        }

        switch (state) {
            case NONE -> {
                state = State.WAITING_FOR_BOBBER;
                lastPullInTime = System.nanoTime();
            }
            case WAITING_FOR_BOBBER -> {
                AABB box = new AABB(
                        mc.player.getX() - 100,
                        mc.player.getY() - 100,
                        mc.player.getZ() - 100,
                        mc.player.getX() + 100,
                        mc.player.getY() + 100,
                        mc.player.getZ() + 100);
                List<FishingHook> bobbers = mc.player.clientLevel.getEntitiesOfClass(FishingHook.class, box);
                bobber = bobbers.stream().filter(b -> b.getPlayerOwner() == mc.player).findFirst();
                if (bobber.isPresent()) {
                    state = State.WAITING_FOR_SOUND;
                }
            }
            case WAITING_FOR_SOUND -> {
                if (mc.player.clientLevel.getEntity(bobber.get().getId()) == null) {
                    // bobber disappeared
                    state = State.WAITING_FOR_BOBBER;
                    break;
                }
                if (config.autoRestartOnIdle && TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - lastPullInTime) > config.idleTimeout) {
                    ((MinecraftAccessor) mc).startUseItem_CU();
                    state = State.DELAY_AFTER_PULL_IN;
                    lastPullInTime = System.nanoTime();
                }
            }
            case PULL_IN -> {
                ((MinecraftAccessor) mc).startUseItem_CU();
                state = State.DELAY_AFTER_PULL_IN;
                lastPullInTime = System.nanoTime();
            }
            case DELAY_AFTER_PULL_IN -> {
                if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - lastPullInTime) >= PULL_IN_DELAY_MS) {
                    ItemStack stack = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
                    if (stack.getItem() == Items.FISHING_ROD) {
                        ((MinecraftAccessor) mc).startUseItem_CU();
                        state = State.WAITING_FOR_BOBBER;
                    }
                }
            }
        }
    }

    private void onServerPacket(NetworkPacketsController.ServerPacketArgs args) {
        if (args.packet instanceof ClientboundSoundPacket soundPacket && soundPacket.getSound().value() == SoundEvents.FISHING_BOBBER_SPLASH) {
            if (state == State.WAITING_FOR_SOUND && bobber.isPresent()) {
                if (bobber.get().distanceToSqr(soundPacket.getX(), soundPacket.getY(), soundPacket.getZ()) < 1) {
                    state = State.PULL_IN;
                }
            }
        }
    }

    private enum State {
        NONE,
        WAITING_FOR_BOBBER,
        WAITING_FOR_SOUND,
        PULL_IN,
        DELAY_AFTER_PULL_IN,
    }
}