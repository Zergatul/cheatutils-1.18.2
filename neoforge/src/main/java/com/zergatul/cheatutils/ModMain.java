package com.zergatul.cheatutils;

import com.zergatul.cheatutils.common.Events;
import com.zergatul.cheatutils.concurrent.PreRenderGuiExecutor;
import com.zergatul.cheatutils.concurrent.TickEndExecutor;
import com.zergatul.cheatutils.controllers.*;
import com.zergatul.cheatutils.modules.Module;
import com.zergatul.cheatutils.modules.automation.*;
import com.zergatul.cheatutils.modules.esp.*;
import com.zergatul.cheatutils.modules.hacks.*;
import com.zergatul.cheatutils.modules.scripting.*;
import com.zergatul.cheatutils.modules.utilities.*;
import com.zergatul.cheatutils.modules.visuals.*;
import com.zergatul.cheatutils.webui.ConfigHttpServer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
@Mod(ModMain.MODID)
public class ModMain {

    public static final String MODID = "cheatutils";
    public static final Logger LOGGER = LogManager.getLogger(ModMain.class);

    private final List<Module> modules = new ArrayList<>();

    public ModMain(IEventBus bus, ModContainer container) {
        bus.addListener(this::onCommonSetup);
        bus.addListener(this::onLoadComplete);
        bus.addListener(this::onRegisterKeybindings);

        register(KeyBindingsController.instance);
        register(BlockEventsProcessor.instance);
        register(NetworkPacketsController.instance);
        register(SpeedCounterController.instance);
        register(BlockFinder.instance);
        register(PreRenderGuiExecutor.instance);

        register(AutoTotem.instance);
        register(KillAura.instance);
        register(AutoEat.instance);
        register(NoFall.instance);
        register(Scaffold.instance);

        register(LockInputsController.instance);
        register(AutoCraft.instance);
        register(BlockEsp.instance);
        register(EntityEsp.instance);
        register(ProjectilePath.instance);
        register(EndCityChunks.instance);
        register(AutoBucket.instance);
        register(WorldDownloadController.instance);
        register(EntityTitleController.instance);
        register(ContainerButtonsController.instance);
        register(TeleportHackController.instance);
        register(WorldMarkersController.instance);
        register(TpsCounterController.instance);
        register(BlockAutomation.instance);

        //register(FakeLag.instance);
        register(FlyHack.instance);
        register(FreeCam.instance);
        register(AutoFish.instance);
        register(ChunkOverlayController.instance);
        register(StatusOverlay.instance);
        register(AutoCriticals.instance);
        register(LightLevel.instance);
        register(ElytraFly.instance);
        register(AdvancedTooltips.instance);
        register(Zoom.instance);
        register(ShulkerTooltipController.instance);
        register(ArmorOverlay.instance);
        register(Fog.instance);
        register(AutoAttack.instance);
        register(Exec.instance);
        register(VillagerRoller.instance);
        register(AutoHotbar.instance);
        register(AreaMine.instance);
        register(ServerPlugins.instance);
        register(BedrockBreaker.instance);
        register(RenderUtilities.instance);
        register(Containers.instance);
        register(AntiHunger.instance);

        register(TickEndExecutor.instance);
    }

    private void register(Module module) {
        modules.add(module);
    }

    @SuppressWarnings("EmptyMethod")
    private void register(Object instance) {
        //logger.info("Registered: {}", instance.getClass().getName());
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        NeoForge.EVENT_BUS.register(new NeoForgeEvents());
    }

    private void onLoadComplete(final FMLLoadCompleteEvent event) {
        Profiles.instance.init();
        ConfigHttpServer.instance.start();
    }

    private void onRegisterKeybindings(final RegisterKeyMappingsEvent event) {
        Events.RegisterKeyBindings.trigger(event::register);
    }
}