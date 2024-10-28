package com.zergatul.cheatutils.mixins.common;

import com.zergatul.cheatutils.configs.ConfigStore;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SlimeBlock.class)
public abstract class MixinSlimeBlock extends HalfTransparentBlock {

    public MixinSlimeBlock(Properties properties) {
        super(properties);
    }

    @Inject(at = @At("HEAD"), method = "fallOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;F)V", cancellable = true)
    private void onFallOn(Level level, BlockState state, BlockPos pos, Entity entity, float p_154571_, CallbackInfo info) {
        if (shouldFallback(entity)) {
            entity.causeFallDamage(p_154571_, 1.0F, level.damageSources().fall());
            info.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "updateEntityMovementAfterFallOn", cancellable = true)
    private void onUpdateEntityAfterFallOn(BlockGetter p_56406_, Entity entity, CallbackInfo info) {
        if (shouldFallback(entity)) {
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D));
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D));
            info.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "stepOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/Entity;)V", cancellable = true)
    private void onStepOn(Level level, BlockPos pos, BlockState state, Entity entity, CallbackInfo info) {
        if (shouldFallback(entity)) {
            info.cancel();
        }
    }

    @Override
    public float getFriction() {
        if (ConfigStore.instance.getConfig().movementHackConfig.disableSlimePhysics) {
            return 0.6F;
        } else {
            return super.getFriction();
        }
    }

    private boolean shouldFallback(Entity entity) {
        return entity.level().isClientSide &&
                Minecraft.getInstance().player == entity &&
                ConfigStore.instance.getConfig().movementHackConfig.disableSlimePhysics;
    }
}