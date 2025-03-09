package com.zergatul.cheatutils.webui;

import com.zergatul.cheatutils.common.Registries;

import java.util.Objects;

public class ItemInfoApi extends ApiBase {

    @Override
    public String getRoute() {
        return "item-info";
    }

    @Override
    public String get() {
        return gson.toJson(Registries.ITEMS.getValues().stream().filter(Objects::nonNull).toArray());
    }
}