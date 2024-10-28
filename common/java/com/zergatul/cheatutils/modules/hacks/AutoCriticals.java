package com.zergatul.cheatutils.modules.hacks;

import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.controllers.NetworkPacketsController;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class AutoCriticals {

    public static final AutoCriticals instance = new AutoCriticals();

    private final Minecraft mc = Minecraft.getInstance();
    private List<ServerboundInteractPacket> processed = new ArrayList<>();

    private AutoCriticals() {
        NetworkPacketsController.instance.addClientPacketHandler(this::onClientPacket);
    }

    private void onClientPacket(NetworkPacketsController.ClientPacketArgs args) {
        if (ConfigStore.instance.getConfig().autoCriticalsConfig.enabled) {
            if (args.packet instanceof ServerboundInteractPacket packet) {
                if (processed.contains(packet)) {
                    processed.remove(packet);
                    return;
                }

                packet.dispatch(new ServerboundInteractPacket.Handler() {
                    @Override
                    public void onInteraction(InteractionHand p_179643_) {

                    }

                    @Override
                    public void onInteraction(InteractionHand p_179644_, Vec3 p_179645_) {

                    }

                    @Override
                    public void onAttack() {
                        if (ConfigStore.instance.getConfig().autoCriticalsConfig.onlyOnGround && !mc.player.onGround()) {
                            return;
                        }

                        args.skip = true;
                        processed.add(packet);

                        double x = mc.player.getX();
                        double y = mc.player.getY();
                        double z = mc.player.getZ();

                        sendPositionPacket(x, y + 0.0625D, z, true);
                        sendPositionPacket(x, y, z, false);
                        sendPositionPacket(x, y + 1.1E-5D, z, false);
                        sendPositionPacket(x, y, z, false);
                        NetworkPacketsController.instance.sendPacket(packet);
                    }
                });
            }
        }
    }

    private void sendPositionPacket(double x, double y, double z, boolean isOnGround) {
        NetworkPacketsController.instance.sendPacket(new ServerboundMovePlayerPacket.Pos(x, y, z, isOnGround, false));
    }
}