package com.zergatul.cheatutils.scripting.modules;

import com.zergatul.cheatutils.common.Registries;
import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.configs.FreeCamConfig;
import com.zergatul.cheatutils.modules.esp.FreeCam;
import com.zergatul.cheatutils.scripting.ApiVisibility;
import com.zergatul.cheatutils.scripting.ApiType;
import com.zergatul.cheatutils.scripting.types.Position3d;
import com.zergatul.scripting.MethodDescription;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Locale;

@SuppressWarnings("unused")
public class FreeCamApi {

    private final Minecraft mc = Minecraft.getInstance();

    @MethodDescription("""
            Checks if Free Cam is active
            """)
    public boolean isEnabled() {
        return FreeCam.instance.isActive();
    }

    public Position3d getPosition() {
        FreeCam fc = FreeCam.instance;
        if (fc.isActive()) {
            return new Position3d(fc.getX(), fc.getY(), fc.getZ());
        } else {
            return new Position3d(0, 0, 0);
        }
    }

    @MethodDescription("""
            Returns formatted X/Y/Z coordinates of Free Cam, or empty string if Free Cam is not active
            """)
    public String getCoordinates() {
        FreeCam fc = FreeCam.instance;
        if (fc.isActive()) {
            return String.format(Locale.ROOT, "%.3f / %.5f / %.3f", fc.getX(), fc.getY(), fc.getZ());
        } else {
            return "";
        }
    }

    @MethodDescription("""
            Returns Free Cam formatted target block coordinates, or empty string if Free Cam is not active
            """)
    public String getTargetBlockCoordinates() {
        if (mc.level == null || !FreeCam.instance.isActive()) {
            return "";
        }

        HitResult hitResult = FreeCam.instance.getHitResult();
        if (hitResult == null) {
            return "";
        }

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos blockPos = ((BlockHitResult) hitResult).getBlockPos();
            return blockPos.getX() + ", " + blockPos.getY() + ", " + blockPos.getZ();
        } else {
            return "";
        }
    }

    @MethodDescription("""
            Returns Free Cam target block name, or empty string if Free Cam is not active
            """)
    public String getTargetBlockName() {
        if (mc.level == null || !FreeCam.instance.isActive()) {
            return "";
        }

        HitResult hitResult = FreeCam.instance.getHitResult();
        if (hitResult == null) {
            return "";
        }

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos blockPos = ((BlockHitResult) hitResult).getBlockPos();
            BlockState blockState = mc.level.getBlockState(blockPos);
            return Registries.BLOCKS.getKey(blockState.getBlock()).toString();
        } else {
            return "";
        }
    }

    @MethodDescription("""
            Toggles Free Cam status
            """)
    @ApiVisibility(ApiType.UPDATE)
    public void toggle() {
        FreeCam.instance.toggle();
    }

    @MethodDescription("""
            Locks Free Cam in current position and transfers control to your character
            """)
    @ApiVisibility(ApiType.UPDATE)
    public void toggleCameraLock() {
        FreeCam.instance.toggleCameraLock();
    }

    @MethodDescription("""
            Locks Free Cam direction to always pointing at your character eyes position
            """)
    @ApiVisibility(ApiType.UPDATE)
    public void toggleEyeLock() {
        FreeCam.instance.toggleEyeLock();
    }

    @MethodDescription("""
            Locks Free Cam rotation, transfers control to your character, but Free Cam will be following your character
            """)
    @ApiVisibility(ApiType.UPDATE)
    public void toggleFollowCam() {
        FreeCam.instance.toggleFollowCamera();
    }

    @MethodDescription("""
            Activates Free Cam path
            """)
    @ApiVisibility(ApiType.UPDATE)
    public void startPath() {
        FreeCam.instance.startPath();
    }

    private FreeCamConfig getConfig() {
        return ConfigStore.instance.getConfig().freeCamConfig;
    }
}