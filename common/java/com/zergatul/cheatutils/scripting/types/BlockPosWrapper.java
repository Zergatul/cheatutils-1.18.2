package com.zergatul.cheatutils.scripting.types;

import com.zergatul.scripting.Getter;
import com.zergatul.scripting.type.CustomType;
import net.minecraft.core.BlockPos;

@CustomType(name = "BlockPos")
public class BlockPosWrapper {

    private final int x;
    private final int y;
    private final int z;

    public BlockPosWrapper(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ());
    }

    public BlockPosWrapper(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Getter(name = "x")
    public int getX() {
        return x;
    }

    @Getter(name = "y")
    public int getY() {
        return y;
    }

    @Getter(name = "z")
    public int getZ() {
        return z;
    }
}