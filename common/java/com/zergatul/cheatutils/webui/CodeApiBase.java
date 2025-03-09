package com.zergatul.cheatutils.webui;

import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.scripting.compiler.CompilationResult;

public abstract class CodeApiBase<T> extends ApiBase {

    @Override
    public String post(String code) {
        code = gson.fromJson(code, String.class);

        if (code == null || code.isEmpty()) {
            setCode(null);
            ConfigStore.instance.requestWrite();
            setProgram(null);
            return "{ \"ok\": true }";
        }

        CompilationResult result = compile(code);
        if (result.getProgram() != null) {
            setCode(code);
            ConfigStore.instance.requestWrite();
            setProgram(result.getProgram());
            return "{ \"ok\": true }";
        } else {
            return gson.toJson(result.getDiagnostics());
        }
    }

    protected abstract CompilationResult compile(String code);
    protected abstract void setCode(String code);
    protected abstract void setProgram(T program);
}