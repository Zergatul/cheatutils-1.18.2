package com.zergatul.cheatutils.webui;

import com.zergatul.cheatutils.wrappers.ClassRemapper;
import net.minecraft.client.Minecraft;

public class ClassNameApi extends ApiBase {

    @Override
    public String getRoute() {
        return "class-name";
    }

    @Override
    public String get(String className) throws ApiException {
        className = ClassRemapper.toObf(className);
        if (className == null) {
            throw new ApiException("Class not found", HttpResponseCodes.NOT_FOUND);
        }

        try {
            Class.forName(className, false, Minecraft.class.getClassLoader());
        }
        catch (ClassNotFoundException e) {
            throw new ApiException("Class not found", HttpResponseCodes.NOT_FOUND);
        }

        return "{ \"ok\": true }";
    }
}