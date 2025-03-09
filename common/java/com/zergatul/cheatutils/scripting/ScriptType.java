package com.zergatul.cheatutils.scripting;

import com.zergatul.cheatutils.scripting.types.*;
import com.zergatul.scripting.compiler.CompilationParameters;
import com.zergatul.scripting.compiler.CompilationParametersBuilder;
import com.zergatul.scripting.compiler.VisibilityChecker;
import com.zergatul.scripting.type.SType;
import com.zergatul.scripting.type.SVoidType;

import java.lang.reflect.Method;

public enum ScriptType {
    KEYBINDING(
            new ApiType[] { ApiType.ACTION, ApiType.UPDATE },
            AsyncRunnable.class,
            "KeyBindingScript",
            SVoidType.instance),

    OVERLAY(
            new ApiType[] { ApiType.OVERLAY },
            Runnable.class,
            "StatusOverlayScript"),

    BLOCK_AUTOMATION(
            new ApiType[] { ApiType.BLOCK_AUTOMATION },
            BlockPosConsumer.class,
            "BlockAutomationScript"),

    VILLAGER_ROLLER(
            new ApiType[] { ApiType.VILLAGER_ROLLER, ApiType.LOGGING },
            Runnable.class,
            "VillagerRollerScript"),

    EVENTS(
            new ApiType[] { ApiType.ACTION, ApiType.UPDATE, ApiType.EVENTS },
            Runnable.class,
            "EventsScripting"),

    ENTITY_ESP(
            new ApiType[] { ApiType.CURRENT_ENTITY_ESP },
            EntityEspConsumer.class,
            "EntityEspScript"),

    KILL_AURA(
            new ApiType[0],
            KillAuraFunction.class,
            "KillAuraScript"),

    HITBOX_SIZE(
            new ApiType[0],
            HitboxSizeFunction.class,
            "HitboxSizeScript");

    private final ApiType[] apis;
    private final Class<?> funcInterface;
    private final String name;
    private final SType asyncReturnType;

    ScriptType(ApiType[] apis, Class<?> funcInterface, String name) {
        this(apis, funcInterface, name, null);
    }

    ScriptType(ApiType[] apis, Class<?> funcInterface, String name, SType asyncReturnType) {
        this.apis = apis;
        this.funcInterface = funcInterface;
        this.name = name;
        this.asyncReturnType = asyncReturnType;
    }

    public ApiType[] getApis() {
        return apis;
    }

    public CompilationParameters createParameters() {
        return new CompilationParametersBuilder()
                .setRoot(Root.class)
                .addCustomType(EnchantmentWrapper.class)
                .addCustomType(ItemStackWrapper.class)
                .addCustomType(ItemWrapper.class)
                .addCustomType(Position3d.class)
                .addCustomType(BlockPosWrapper.class)
                .addCustomType(HttpRequestWrapper.class)
                .addCustomType(HttpRequestBuilderWrapper.class)
                .addCustomType(HttpResponseWrapper.class)
                .addCustomType(HttpHeader.class)
                .addCustomType(Regex.class)
                .addCustomType(Match.class)
                .addCustomType(MatchGroup.class)
                .addCustomType(MatchGroups.class)
                .addCustomType(ComponentWrapper.class)
                .addCustomType(FormattedTextComponent.class)
                .addCustomType(StyleWrapper.class)
                .setInterface(funcInterface)
                .setAsyncReturnType(asyncReturnType)
                .setVisibilityChecker(new VisibilityChecker() {
                    @Override
                    public boolean isVisible(Method method) {
                        return VisibilityCheck.isOk(method, apis);
                    }
                })
                .setClassNamePrefix(name)
                .setSourceFile("<" + name + ">")
                .setLineNumbers(true)
                .build();
    }
}