package com.zergatul.cheatutils.mixins.common.accessors;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BlockBehaviour.class)
public interface BlockBehaviourAccessor {

    @Invoker("canSurvive")
    boolean canSurvive_CU(BlockState p_60525_, LevelReader p_60526_, BlockPos p_60527_);
}