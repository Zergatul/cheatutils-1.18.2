package com.zergatul.cheatutils.scripting.types;

import com.zergatul.scripting.Getter;
import com.zergatul.scripting.type.CustomType;
import net.minecraft.world.phys.AABB;

@SuppressWarnings("unused")
@CustomType(name = "BoundingBox")
public class BoundingBox {

    private final AABB bb;

    public BoundingBox(AABB bb) {
        this.bb = bb;
    }

    @Getter(name = "minX")
    public double getMinX() {
        return bb.minX;
    }

    @Getter(name = "minY")
    public double getMinY() {
        return bb.minY;
    }

    @Getter(name = "minZ")
    public double getMinZ() {
        return bb.minZ;
    }

    @Getter(name = "maxX")
    public double getMaxX() {
        return bb.maxX;
    }

    @Getter(name = "maxY")
    public double getMaxY() {
        return bb.maxY;
    }

    @Getter(name = "maxZ")
    public double getMaxZ() {
        return bb.maxZ;
    }

    @Getter(name = "sizeX")
    public double getSizeX() {
        return bb.getXsize();
    }

    @Getter(name = "sizeY")
    public double getSizeY() {
        return bb.getYsize();
    }

    @Getter(name = "sizeZ")
    public double getSizeZ() {
        return bb.getZsize();
    }
}