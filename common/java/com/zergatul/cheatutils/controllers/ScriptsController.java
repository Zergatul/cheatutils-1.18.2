package com.zergatul.cheatutils.controllers;

import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.configs.KeyBindingsConfig;
import com.zergatul.cheatutils.configs.KeyBindingScriptsConfig;
import com.zergatul.cheatutils.scripting.AsyncRunnable;
import com.zergatul.cheatutils.scripting.ScriptType;
import com.zergatul.scripting.DiagnosticMessage;
import com.zergatul.scripting.compiler.CompilationResult;
import com.zergatul.scripting.compiler.Compiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScriptsController {

    public static final ScriptsController instance = new ScriptsController();

    private final Compiler handleKeybindingsCompiler = new Compiler(ScriptType.KEYBINDING.createParameters());
    private final Compiler overlayCompiler = new Compiler(ScriptType.OVERLAY.createParameters());
    private final Compiler blockAutomationCompiler = new Compiler(ScriptType.BLOCK_AUTOMATION.createParameters());
    private final Compiler villagerRollerCompiler = new Compiler(ScriptType.VILLAGER_ROLLER.createParameters());
    private final Compiler eventsCompiler = new Compiler(ScriptType.EVENTS.createParameters());
    private final Compiler entityEspCompiler = new Compiler(ScriptType.ENTITY_ESP.createParameters());
    private final Compiler killAuraCompiler = new Compiler(ScriptType.KILL_AURA.createParameters());
    private final Compiler hitboxSizeCompiler = new Compiler(ScriptType.HITBOX_SIZE.createParameters());

    private final List<Script> scripts = Collections.synchronizedList(new ArrayList<>());

    private ScriptsController() {

    }

    public List<DiagnosticMessage> add(String name, String code, boolean addIfCompilationFails) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("Name is required.");
        }
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Code is required.");
        }
        if (exists(name)) {
            throw new IllegalArgumentException("Script with the same name already exists.");
        }

        var script = new Script();
        script.name = name;
        script.code = code;

        if (addIfCompilationFails) {
            scripts.add(script);
            ConfigStore.instance.getConfig().keyBindingScriptsConfig.scripts.add(new KeyBindingScriptsConfig.ScriptEntry(name, code));
        }

        CompilationResult result = handleKeybindingsCompiler.compile(code);
        if (result.getProgram() != null) {
            script.compiled = result.getProgram();

            if (!addIfCompilationFails) {
                scripts.add(script);
                ConfigStore.instance.getConfig().keyBindingScriptsConfig.scripts.add(new KeyBindingScriptsConfig.ScriptEntry(name, code));
            }
        }

        if (result.getDiagnostics() != null) {
            return result.getDiagnostics();
        } else {
            return List.of();
        }
    }

    public void clear() {
        scripts.clear();
    }

    public List<DiagnosticMessage> update(String oldName, String newName, String code) throws IllegalArgumentException {
        if (!oldName.equals(newName)) {
            if (exists(newName)) {
                throw new IllegalArgumentException("Script with the same name already exists.");
            }
        }

        if (!exists(oldName)) {
            throw new IllegalArgumentException("Cannot find original script by name " + oldName + ".");
        }

        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Code is required.");
        }

        int bindingIndex = -1;
        String[] bindings = ConfigStore.instance.getConfig().keyBindingsConfig.bindings;
        for (int i = 0; i < KeyBindingsConfig.KeysCount; i++) {
            if (bindings[i] != null && bindings[i].equals(oldName)) {
                bindingIndex = i;
                KeyBindingsController.instance.assign(-1, oldName);
                break;
            }
        }

        Script script = scripts.stream().filter(s -> s.name.equals(oldName)).findFirst().orElse(null);
        KeyBindingScriptsConfig.ScriptEntry configScript = ConfigStore.instance.getConfig().keyBindingScriptsConfig.scripts.stream().filter(s -> s.name.equals(oldName)).findFirst().orElse(null);
        if (script != null) {
            script.name = newName;
            script.code = code;
            if (configScript != null) {
                configScript.name = newName;
                configScript.code = code;
            }
            script.compiled = null;
            if (bindingIndex >= 0) {
                bindings[bindingIndex] = newName;
            }
            CompilationResult result = handleKeybindingsCompiler.compile(code);
            if (result.getProgram() != null) {
                script.compiled = result.getProgram();
                if (bindingIndex >= 0) {
                    KeyBindingsController.instance.assign(bindingIndex, newName);
                }
                return List.of();
            } else {
                return result.getDiagnostics();
            }
        }

        return List.of();
    }

    public AsyncRunnable get(String name) {
        var optional = scripts.stream().filter(s -> s.name.equals(name)).findFirst();
        if (optional.isEmpty()) {
            return null;
        }
        return optional.get().compiled;
    }

    public List<Script> list() {
        return new ArrayList<>(scripts);
    }

    public boolean exists(String name) {
        return scripts.stream().anyMatch(s -> s.name.equals(name));
    }

    public void remove(String name) {
        KeyBindingsController.instance.assign(-1, name);
        scripts.removeIf(s -> s.name.equals(name));
        ConfigStore.instance.getConfig().keyBindingScriptsConfig.scripts.removeIf(s -> s.name.equals(name));
    }

    public CompilationResult compileOverlay(String code) {
        return overlayCompiler.compile(code);
    }

    public CompilationResult compileKeys(String code) {
        return handleKeybindingsCompiler.compile(code);
    }

    public CompilationResult compileBlockAutomation(String code) {
        return blockAutomationCompiler.compile(code);
    }

    public CompilationResult compileVillagerRoller(String code) {
        return villagerRollerCompiler.compile(code);
    }

    public CompilationResult compileEvents(String code) {
        return eventsCompiler.compile(code);
    }

    public CompilationResult compileEntityEsp(String code) {
        return entityEspCompiler.compile(code);
    }

    public CompilationResult compileKillAura(String code) {
        return killAuraCompiler.compile(code);
    }

    public CompilationResult compileHitboxSize(String code) {
        return hitboxSizeCompiler.compile(code);
    }

    public static class Script {
        public String name;
        public String code;
        public AsyncRunnable compiled;
    }
}