package com.zergatul.cheatutils.configs;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;
import com.zergatul.cheatutils.collections.ImmutableList;
import com.zergatul.cheatutils.configs.adapters.*;
import com.zergatul.cheatutils.controllers.*;
import com.zergatul.cheatutils.modules.automation.VillagerRoller;
import com.zergatul.cheatutils.modules.esp.LightLevel;
import com.zergatul.cheatutils.modules.hacks.HitboxSize;
import com.zergatul.cheatutils.modules.hacks.KillAura;
import com.zergatul.cheatutils.modules.scripting.EventsScripting;
import com.zergatul.cheatutils.modules.scripting.BlockAutomation;
import com.zergatul.cheatutils.modules.scripting.StatusOverlay;
import com.zergatul.cheatutils.webui.ConfigHttpServer;
import com.zergatul.scripting.compiler.CompilationResult;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;

public class ConfigStore {

    public static final ConfigStore instance = new ConfigStore();

    private static final long WRITE_FILE_DELAY = 15 * 1_000_000_000L;

    public final Gson gson = new GsonBuilder()
            .setExclusionStrategies(new GsonSkipExcludeStrategy())
            .registerTypeAdapterFactory(new BlockTypeAdapterFactory())
            .registerTypeAdapterFactory(new ItemTypeAdapterFactory())
            .registerTypeAdapterFactory(new KillAuraConfig$PriorityEntryTypeAdapterFactory())
            .registerTypeAdapterFactory(new ClassTypeAdapterFactory())
            .registerTypeAdapter(BlockState.class, new BlockStateTypeAdapter())
            .registerTypeAdapter(Color.class, new ColorTypeAdapter())
            .registerTypeAdapter(ImmutableList.class, new ImmutableListSerializer())
            .setPrettyPrinting()
            .create();

    private final Logger logger = LogManager.getLogger(ConfigStore.class);
    private Config config;
    private File currentFile;

    private ConfigStore() {
        config = new Config();
    }

    public Config getConfig() {
        return config;
    }

    public synchronized void read(File file) {
        Config newConfig = new Config();
        if (file.exists()) {
            Config readCfg = null;
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                JsonElement element = JsonParser.parseReader(reader);
                migration1(element);
                migration2(element);
                migration3(element);
                migration4(element);
                readCfg = gson.fromJson(element, Config.class);
                reader.close();
            } catch (Exception e) {
                logger.error("Cannot read config");
                logger.error(e);
            }

            if (readCfg != null) {
                newConfig = readCfg;
            }
        }

        currentFile = file;
        config = newConfig;
        onConfigLoaded();
    }

    public synchronized void switchFile(File file) {
        currentFile = file;
        requestWrite();
    }

    public synchronized void createNew(File file) {
        currentFile = file;
        config = new Config();
        onConfigLoaded();
        requestWrite();
    }

    public void requestWrite() {
        File file = this.currentFile;
        Config config = this.config;
        ConfigWriterQueue.instance.queue(file, WRITE_FILE_DELAY, () -> {
            logger.debug("Saving config to file " + file.getName());
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                gson.toJson(config, writer);
                writer.close();
            }
            catch (Exception e) {
                logger.error("Cannot write config");
                logger.error(e);
            }
        });
    }

    private void onConfigLoaded() {
        LightLevel.instance.onChanged();
        config.blocks.apply();

        // clazz==null can occur after removing mod with custom entities
        config.entities.configs = config.entities.configs.removeIf(c -> c.clazz == null);

        // TODO: use reflection to automatically find ValidatableConfig's?
        config.killAuraConfig.validate();
        config.movementHackConfig.validate();
        config.fastBreakConfig.validate();
        config.elytraHackConfig.validate();
        config.freeCamConfig.validate();
        config.flyHackConfig.validate();
        config.boatHackConfig.validate();
        config.explorationMiniMapConfig.validate();
        config.reachConfig.validate();
        config.lightLevelConfig.validate();
        config.schematicaConfig.validate();
        config.autoBucketConfig.validate();
        config.performanceConfig.validate();
        config.entityTitleConfig.validate();
        config.keyBindingsConfig.validate();
        config.worldMarkersConfig.validate();
        config.autoAttackConfig.validate();
        config.projectilePathConfig.validate();
        config.chatUtilitiesConfig.validate();
        config.areaMineConfig.validate();
        config.hitboxSizeConfig.validate();
        config.coreConfig.validate();

        config.blocks.refreshMap();

        ConfigHttpServer.instance.onConfigUpdated();
        EntityTitleController.instance.onFontChange(config.entityTitleConfig);
        EntityTitleController.instance.onEnchantmentFontChange(config.entityTitleConfig);
        WorldMarkersController.instance.onFontChange(config.worldMarkersConfig);

        ScriptsController.instance.clear();
        if (config.keyBindingScriptsConfig.scripts.isEmpty()) {
            final String toggleEspName = "Toggle ESP";
            try {
                ScriptsController.instance.add(toggleEspName, "esp.toggle();", false);
                //KeyBindingsController.instance.keys[0].setKey(InputConstants.getKey("key.keyboard.backslash"));
                KeyBindingsController.instance.assign(0, toggleEspName);
            } catch (Throwable e) {
                logger.error(e);
            }

            final String toggleFreeCamName = "Toggle FreeCam";
            try {
                ScriptsController.instance.add(toggleFreeCamName, "freeCam.toggle();", false);
                KeyBindingsController.instance.keys[1].setKey(InputConstants.getKey("key.keyboard.f6"));
                KeyBindingsController.instance.assign(1, toggleFreeCamName);
            } catch (Throwable e) {
                logger.error(e);
            }
        } else {
            ArrayList<KeyBindingScriptsConfig.ScriptEntry> copy = new ArrayList<>(config.keyBindingScriptsConfig.scripts);
            config.keyBindingScriptsConfig.scripts.clear();
            copy.forEach(s -> {
                try {
                    ScriptsController.instance.add(s.name, s.code, true);
                } catch (Throwable e) {
                    logger.error(e);
                }
            });

            String[] bindings = config.keyBindingsConfig.bindings;
            for (int i = 0; i < KeyBindingsConfig.KeysCount; i++) {
                if (bindings[i] != null) {
                    KeyBindingsController.instance.assign(i, bindings[i]);
                }
            }
        }

        if (config.statusOverlayConfig.code != null) {
            try {
                CompilationResult result = ScriptsController.instance.compileOverlay(config.statusOverlayConfig.code);
                if (result.getProgram() != null) {
                    StatusOverlay.instance.setScript(result.getProgram());
                } else {
                    result.getDiagnostics().forEach(m -> logger.error("Status Overlay: {}", m.message));
                }
            } catch (Throwable e) {
                logger.error(e);
            }
        }

        if (config.blockAutomationConfig.code != null) {
            try {
                CompilationResult result = ScriptsController.instance.compileBlockAutomation(config.blockAutomationConfig.code);
                if (result.getProgram() != null) {
                    BlockAutomation.instance.setScript(result.getProgram());
                } else {
                    result.getDiagnostics().forEach(m -> logger.error("Block Automation: {}", m.message));
                }
            } catch (Throwable e) {
                logger.error(e);
            }
        }

        if (config.villagerRollerConfig.code != null) {
            try {
                CompilationResult result = ScriptsController.instance.compileVillagerRoller(config.villagerRollerConfig.code);
                if (result.getProgram() != null) {
                    VillagerRoller.instance.setScript(result.getProgram());
                } else {
                    result.getDiagnostics().forEach(m -> logger.error("Villager Roller: {}", m.message));
                }
            } catch (Throwable e) {
                logger.error(e);
            }
        }

        if (config.eventsScriptingConfig.code != null) {
            try {
                CompilationResult result = ScriptsController.instance.compileEvents(config.eventsScriptingConfig.code);
                if (result.getProgram() != null) {
                    EventsScripting.instance.setScript(result.getProgram());
                } else {
                    result.getDiagnostics().forEach(m -> logger.error("Events Scripting: {}", m.message));
                }
            } catch (Throwable e) {
                logger.error(e);
            }
        }

        if (config.entities != null && config.entities.configs != null) {
            for (EntityEspConfig entityConfig : config.entities.configs) {
                if (entityConfig.code != null && entityConfig.code.isBlank()) {
                    entityConfig.code = null;
                }

                if (entityConfig.code != null) {
                    try {
                        CompilationResult result = ScriptsController.instance.compileEntityEsp(entityConfig.code);
                        if (result.getProgram() != null) {
                            entityConfig.script = result.getProgram();
                        } else {
                            result.getDiagnostics().forEach(m -> logger.error("Entity ESP for {}: {}", entityConfig.clazz.getName(), m.message));
                        }
                    } catch (Throwable e) {
                        logger.error(e);
                    }
                }
            }
        }

        if (config.killAuraConfig.code != null) {
            try {
                CompilationResult result = ScriptsController.instance.compileKillAura(config.killAuraConfig.code);
                if (result.getProgram() != null) {
                    KillAura.instance.setScript(result.getProgram());
                } else {
                    result.getDiagnostics().forEach(m -> logger.error("Kill Aura: {}", m.message));
                }
            } catch (Throwable e) {
                logger.error(e);
            }
        }

        if (config.hitboxSizeConfig.code != null) {
            try {
                CompilationResult result = ScriptsController.instance.compileHitboxSize(config.hitboxSizeConfig.code);
                if (result.getProgram() != null) {
                    HitboxSize.instance.setScript(result.getProgram());
                } else {
                    result.getDiagnostics().forEach(m -> logger.error("Hitbox Size: {}", m.message));
                }
            } catch (Throwable e) {
                logger.error(e);
            }
        }
    }

    private void migration1(JsonElement element) {
        if (!element.isJsonObject()) {
            return;
        }

        JsonObject root = element.getAsJsonObject();
        if (!root.has("blocks")) {
            return;
        }

        element = root.get("blocks");
        if (!element.isJsonObject()) {
            return;
        }

        JsonObject blocks = element.getAsJsonObject();
        if (!blocks.has("configs")) {
            return;
        }

        element = blocks.get("configs");
        if (!element.isJsonArray()) {
            return;
        }

        JsonArray configs = element.getAsJsonArray();
        for (JsonElement item: configs) {
            if (!item.isJsonObject()) {
                continue;
            }

            JsonObject config = item.getAsJsonObject();
            if (config.has("block")) {
                JsonArray array = new JsonArray();
                array.add(config.get("block"));
                config.add("blocks", array);
                config.remove("block");
            }
        }
    }

    private void migration2(JsonElement element) {
        if (!element.isJsonObject()) {
            return;
        }

        JsonObject root = element.getAsJsonObject();
        if (!root.has("scriptedBlockPlacerConfig")) {
            return;
        }

        JsonElement config = root.get("scriptedBlockPlacerConfig");
        root.remove("scriptedBlockPlacerConfig");
        root.add("blockAutomationConfig", config);
    }

    private void migration3(JsonElement element) {
        if (!element.isJsonObject()) {
            return;
        }

        JsonObject root = element.getAsJsonObject();
        if (!root.has("fogConfig")) {
            return;
        }

        JsonElement config = root.get("fogConfig");
        if (!config.isJsonObject()) {
            return;
        }

        JsonObject fog = config.getAsJsonObject();
        if (fog.has("disableFog")) {
            JsonElement value = fog.remove("disableFog");
            fog.add("enabled", value);
        }
    }

    private void migration4(JsonElement element) {
        if (!element.isJsonObject()) {
            return;
        }

        JsonObject root = element.getAsJsonObject();
        if (!root.has("scriptsConfig")) {
            return;
        }

        JsonElement config = root.get("scriptsConfig");
        if (!config.isJsonObject()) {
            return;
        }

        root.remove("scriptsConfig");
        root.add("keyBindingScriptsConfig", config);
    }
}