package com.zergatul.cheatutils.webui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.configs.EntityEspConfig;
import com.zergatul.cheatutils.controllers.ScriptsController;
import com.zergatul.scripting.compiler.CompilationResult;

public class EntityEspCodeApi extends ApiBase {

    @Override
    public String getRoute() {
        return "entity-esp-code";
    }

    @Override
    public String post(String json) throws ApiException {
        Request request = gson.fromJson(json, Request.class);

        EntityEspConfig config = ConfigStore.instance.getConfig().entities.configs.stream()
                .filter(c -> c.clazz == request.clazz)
                .findFirst()
                .orElse(null);
        if (config == null) {
            throw new ApiException("Cannot find entity config.", HttpResponseCodes.NOT_FOUND);
        }

        if (request.code == null || request.code.isBlank()) {
            RenderSystem.recordRenderCall(() -> {
                config.code = null;
                config.script = null;
                ConfigStore.instance.requestWrite();
            });
            return "{ \"ok\": true }";
        }

        CompilationResult result = ScriptsController.instance.compileEntityEsp(request.code);
        if (result.getProgram() != null) {
            RenderSystem.recordRenderCall(() -> {
                config.code = request.code;
                config.script = result.getProgram();
                ConfigStore.instance.requestWrite();
            });
            return "{ \"ok\": true }";
        } else {
            return gson.toJson(result.getDiagnostics());
        }
    }

    public record Request(Class<?> clazz, String code) {}
}