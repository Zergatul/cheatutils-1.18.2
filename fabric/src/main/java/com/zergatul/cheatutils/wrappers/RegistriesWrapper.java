package com.zergatul.cheatutils.wrappers;

import com.zergatul.cheatutils.common.WrappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;

import java.util.Collection;

public class RegistriesWrapper {

    public static WrappedRegistry<Block> getBlocks() {
        return new VanillaWrapperRegistry<>(BuiltInRegistries.BLOCK);
    }

    public static WrappedRegistry<Item> getItems() {
        return new VanillaWrapperRegistry<>(BuiltInRegistries.ITEM);
    }

    public static WrappedRegistry<EntityType<?>> getEntityTypes() {
        return new VanillaWrapperRegistry<>(BuiltInRegistries.ENTITY_TYPE);
    }

    public static WrappedRegistry<MobEffect> getMobEffects() {
        return new VanillaWrapperRegistry<>(BuiltInRegistries.MOB_EFFECT);
    }

    private record VanillaWrapperRegistry<T>(Registry<T> registry) implements WrappedRegistry<T> {

        @Override
        public ResourceLocation getKey(T value) {
            return registry.getKey(value);
        }

        @Override
        public T getValue(ResourceLocation id) {
            return registry.getValue(id);
        }

        @Override
        public Collection<T> getValues() {
            return registry.keySet().stream().map(this::getValue).toList();
        }
    }
}