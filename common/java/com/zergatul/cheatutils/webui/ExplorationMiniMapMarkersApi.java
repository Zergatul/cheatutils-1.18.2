package com.zergatul.cheatutils.webui;

import com.google.common.reflect.TypeToken;
import com.zergatul.cheatutils.chunkoverlays.ExplorationMiniMapChunkOverlay;
import com.zergatul.cheatutils.controllers.ChunkOverlayController;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ExplorationMiniMapMarkersApi extends ApiBase {

    @Override
    public String getRoute() {
        return "exploration-mini-map-markers";
    }

    @Override
    public String post(String body) {
        ChunkOverlayController.instance.ofType(ExplorationMiniMapChunkOverlay.class).addMarker();
        return "true";
    }

    @Override
    public String put(String id, String body) {
        if (Objects.equals(id, "import")) {
            Type listType = new TypeToken<ArrayList<Point>>(){}.getType();
            List<Point> points = gson.fromJson(body, listType);
            points.forEach(p -> {
                if (Double.isNaN(p.x)) {
                    return;
                }
                if (Double.isNaN(p.z)) {
                    return;
                }
                ChunkOverlayController.instance.ofType(ExplorationMiniMapChunkOverlay.class).addMarker(p.x, p.z);
            });
            return "{ \"ok\": true }";
        } else {
            return "{}";
        }
    }

    @Override
    public String delete(String id) {
        ChunkOverlayController.instance.ofType(ExplorationMiniMapChunkOverlay.class).clearMarkers();
        return "true";
    }

    public static class Point {
        public double x = Double.NaN;
        public double z = Double.NaN;
    }
}