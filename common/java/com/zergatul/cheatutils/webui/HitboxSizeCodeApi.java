package com.zergatul.cheatutils.webui;

import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.controllers.ScriptsController;
import com.zergatul.cheatutils.modules.hacks.HitboxSize;
import com.zergatul.cheatutils.scripting.HitboxSizeFunction;
import com.zergatul.scripting.compiler.CompilationResult;

public class HitboxSizeCodeApi extends CodeApiBase<HitboxSizeFunction> {

    @Override
    public String getRoute() {
        return "hitbox-size-code";
    }

    @Override
    protected CompilationResult compile(String code) {
        return ScriptsController.instance.compileHitboxSize(code);
    }

    @Override
    protected void setCode(String code) {
        ConfigStore.instance.getConfig().hitboxSizeConfig.code = code;
    }

    @Override
    protected void setProgram(HitboxSizeFunction program) {
        HitboxSize.instance.setScript(program);
    }
}