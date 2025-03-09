package com.zergatul.cheatutils.webui;

import com.zergatul.cheatutils.modules.esp.BlockFinder;

public class RescanChunksApi extends ApiBase {

    @Override
    public String getRoute() {
        return "rescan-chunks";
    }

    @Override
    public String post(String body) {
        BlockFinder.instance.rescan();
        return "{}";
    }
}