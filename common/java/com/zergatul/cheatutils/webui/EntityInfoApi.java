package com.zergatul.cheatutils.webui;

import com.zergatul.cheatutils.utils.EntityUtils;

public class EntityInfoApi extends ApiBase {

    @Override
    public String getRoute() {
        return "entity-info";
    }

    @Override
    public String get() {
        return gson.toJson(EntityUtils.getEntityClasses());
    }
}