package com.zergatul.cheatutils.webui;

import com.zergatul.cheatutils.schematics.InvalidFormatException;
import com.zergatul.cheatutils.schematics.SchemaFile;
import com.zergatul.cheatutils.schematics.SchemaFormatFactory;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.util.Base64;

public class SchematicaUploadApi extends ApiBase {

    @Override
    public String getRoute() {
        return "schematica-upload";
    }

    @Override
    public String post(String body) {
        Request request = gson.fromJson(body, Request.class);
        byte[] data = Base64.getDecoder().decode(request.file);
        SchemaFile schema;
        try {
            schema = SchemaFormatFactory.parse(data, request.name);
        }
        catch (IOException | InvalidFormatException e) {
            return gson.toJson(new ErrorResponse(e.getMessage()));
        }

        return gson.toJson(new SuccessResponse(
                schema.getSummary(),
                schema.getPalette(),
                schema.getWidth(),
                schema.getHeight(),
                schema.getLength()));
    }

    public record Request(String file, String name) {}

    public record ErrorResponse(String error) {}

    public record SuccessResponse(int[] summary, BlockState[] palette, int width, int height, int length) {}
}