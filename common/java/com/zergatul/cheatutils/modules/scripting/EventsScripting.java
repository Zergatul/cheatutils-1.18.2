package com.zergatul.cheatutils.modules.scripting;

import com.mojang.blaze3d.systems.RenderSystem;
import com.zergatul.cheatutils.common.Events;
import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.modules.Module;
import com.zergatul.cheatutils.scripting.*;
import com.zergatul.cheatutils.scripting.types.ComponentWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;

import java.util.ArrayList;
import java.util.List;

public class EventsScripting implements Module {

    public static final EventsScripting instance = new EventsScripting();

    private final Minecraft mc = Minecraft.getInstance();
    private final List<Runnable> onHandleKeys = new ArrayList<>();
    private final List<Runnable> onTickEnd = new ArrayList<>();
    private final List<EntityIdConsumer> onPlayerAdded = new ArrayList<>();
    private final List<EntityIdConsumer> onPlayerRemoved = new ArrayList<>();
    private final List<ComponentWrapperConsumer> onChatMessageRaw = new ArrayList<>();
    private final List<ChatMessageConsumer> onChatMessage = new ArrayList<>();
    private final List<ServerAddressConsumer> onJoinServer = new ArrayList<>();
    private final List<ContainerClickConsumer> onContainerMenuClick = new ArrayList<>();

    private EventsScripting() {
        Events.BeforeHandleKeyBindings.add(() -> {
            if (canTrigger()) {
                for (Runnable handler : onHandleKeys) {
                    handler.run();
                }
            }
        });

        Events.ClientTickEnd.add(() -> {
            if (canTrigger()) {
                for (Runnable handler : onTickEnd) {
                    handler.run();
                }
            }
        });

        Events.EntityAdded.add(entity -> {
            if (canTrigger() && entity instanceof RemotePlayer) {
                for (EntityIdConsumer consumer : onPlayerAdded) {
                    consumer.accept(entity.getId());
                }
            }
        });

        Events.EntityRemoved.add(entity -> {
            if (canTrigger() && entity instanceof RemotePlayer) {
                for (EntityIdConsumer consumer : onPlayerRemoved) {
                    consumer.accept(entity.getId());
                }
            }
        });

        Events.ChatMessageAdded.add(component -> {
            if (canTrigger()) {
                ComponentWrapper wrapper = new ComponentWrapper(component);
                for (ComponentWrapperConsumer consumer : onChatMessageRaw) {
                    consumer.accept(wrapper);
                }
                String text = component.getString();
                for (ChatMessageConsumer consumer : onChatMessage) {
                    consumer.accept(text);
                }
            }
        });

        Events.ClientPlayerLoggingIn.add(connection -> {
            if (ConfigStore.instance.getConfig().eventsScriptingConfig.enabled) {
                String address = connection == null ? "" : connection.getRemoteAddress().toString();
                for (ServerAddressConsumer consumer : onJoinServer) {
                    consumer.accept(address);
                }
            }
        });

        Events.ContainerMenuClick.add(event -> {
            if (canTrigger()) {
                for (ContainerClickConsumer consumer : onContainerMenuClick) {
                    consumer.accept(event.slot(), event.button(), event.type().toString());
                }
            }
        });
    }

    public void setScript(Runnable runnable) {
        clear();
        if (runnable != null) {
            RenderSystem.recordRenderCall(runnable::run);
        }
    }

    public void clear() {
        RenderSystem.recordRenderCall(() -> {
            onHandleKeys.clear();
            onTickEnd.clear();
            onPlayerAdded.clear();
            onPlayerRemoved.clear();
            onChatMessageRaw.clear();
            onChatMessage.clear();
            onJoinServer.clear();
            onContainerMenuClick.clear();
        });
    }

    public void addOnHandleKeys(Runnable action) {
        onHandleKeys.add(action);
    }

    public void addOnTickEnd(Runnable action) {
        onTickEnd.add(action);
    }

    public void addOnPlayerAdded(EntityIdConsumer consumer) {
        onPlayerAdded.add(consumer);
    }

    public void addOnPlayerRemoved(EntityIdConsumer consumer) {
        onPlayerRemoved.add(consumer);
    }

    public void addOnChatMessageRaw(ComponentWrapperConsumer consumer) {
        onChatMessageRaw.add(consumer);
    }

    public void addOnChatMessage(ChatMessageConsumer consumer) {
        onChatMessage.add(consumer);
    }

    public void addOnJoinServer(ServerAddressConsumer consumer) {
        onJoinServer.add(consumer);
    }

    public void addOnContainerMenuClick(ContainerClickConsumer consumer) {
        onContainerMenuClick.add(consumer);
    }

    private boolean canTrigger() {
        return mc.player != null && ConfigStore.instance.getConfig().eventsScriptingConfig.enabled;
    }
}