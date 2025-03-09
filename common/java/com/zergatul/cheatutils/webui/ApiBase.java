package com.zergatul.cheatutils.webui;

import com.google.gson.Gson;
import com.zergatul.cheatutils.configs.ConfigStore;

public abstract class ApiBase {

    protected Gson gson = ConfigStore.instance.gson;

    public abstract String getRoute();

    public String get() throws Throwable {
        throw new ApiException("Method not allowed", HttpResponseCodes.METHOD_NOT_ALLOWED);
    }

    public String get(String id) throws Throwable {
        throw new ApiException("Method not allowed", HttpResponseCodes.METHOD_NOT_ALLOWED);
    }

    public String put(String id, String body) throws Throwable {
        throw new ApiException("Method not allowed", HttpResponseCodes.METHOD_NOT_ALLOWED);
    }

    public String post(String body) throws Throwable {
        throw new ApiException("Method not allowed", HttpResponseCodes.METHOD_NOT_ALLOWED);
    }

    public String delete(String id) throws Throwable {
        throw new ApiException("Method not allowed", HttpResponseCodes.METHOD_NOT_ALLOWED);
    }
}