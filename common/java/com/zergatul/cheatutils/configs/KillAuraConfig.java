package com.zergatul.cheatutils.configs;

import com.zergatul.cheatutils.collections.ImmutableList;
import com.zergatul.cheatutils.utils.MathUtils;
import com.zergatul.cheatutils.wrappers.ClassRemapper;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.entity.projectile.ShulkerBullet;

import java.util.*;
import java.util.function.Predicate;

public class KillAuraConfig extends ModuleConfig implements ValidatableConfig {

    public static final String ConstDelay = "ConstDelay";
    public static final String Cooldown = "Cooldown";

    public boolean overrideAttackRange;
    public double maxRange;
    public String delayMode;
    public int attackTickInterval;
    public double extraTicks;
    public Double maxHorizontalAngle;
    public Double maxVerticalAngle;
    public ImmutableList<PriorityEntry> priorities;
    public ImmutableList<CustomPriorityEntry> customEntries;
    public boolean attackAll;

    public KillAuraConfig() {
        overrideAttackRange = true;
        maxRange = 6;
        delayMode = Cooldown;
        attackTickInterval = 5;
        extraTicks = 0;
        priorities = new ImmutableList<PriorityEntry>()
                .add(PredefinedPriorityEntry.fromName("Enemies"))
                .add(PredefinedPriorityEntry.fromName("Shulker Bullets"))
                .add(PredefinedPriorityEntry.fromName("Fireballs"));
        customEntries = new ImmutableList<>();
    }

    public void validate() {
        maxRange = MathUtils.clamp(maxRange, 1, 100);
        attackTickInterval = MathUtils.clamp(attackTickInterval, 1, 100);
        extraTicks = MathUtils.clamp(extraTicks, -100, 100);
        if (maxHorizontalAngle != null) {
            maxHorizontalAngle = MathUtils.clamp(maxHorizontalAngle, 1, 180);
        }
        if (maxVerticalAngle != null) {
            maxVerticalAngle = MathUtils.clamp(maxVerticalAngle, 1, 180);
        }

        priorities = priorities.removeIf(Objects::isNull);
        customEntries = customEntries.removeIf(Objects::isNull);
    }

    public static class PriorityEntry {
        public final String name;
        public final String description;
        public final Predicate<Entity> predicate;
        public boolean enabled;

        public PriorityEntry(String name, String description, Predicate<Entity> predicate) {
            this.name = name;
            this.description = description;
            this.predicate = predicate;
            this.enabled = true;
        }
    }

    public static class PredefinedPriorityEntry extends PriorityEntry {

        public static final String ENEMIES = "Enemies";
        public static final String ENEMIES_WO_PIGLINS = "Enemies w/o Piglins";
        public static final String PLAYERS = "Players";
        public static final String SHULKER_BULLETS = "Shulker Bullets";
        public static final String FIREBALLS = "Fireballs";

        public static final Map<String, PredefinedPriorityEntry> entries = Map.ofEntries(
                Map.entry(ENEMIES, new PredefinedPriorityEntry(
                        ENEMIES,
                        "Includes Monsters, Slimes, Magma Cubes, Hoglin. Excludes Neutral Mobs (Enderman, ZombifiedPiglin). Endermen can be targeted only in creepy state.",
                        entity -> {
                            if (entity instanceof EnderMan) {
                                return ((EnderMan) entity).isCreepy();
                            }
                            return entity instanceof Enemy && !(entity instanceof NeutralMob);
                        })),
                Map.entry(ENEMIES_WO_PIGLINS, new PredefinedPriorityEntry(
                        ENEMIES_WO_PIGLINS,
                        "Same as Enemies, but without Piglins.",
                        entity -> {
                            if (entity instanceof EnderMan) {
                                return ((EnderMan) entity).isCreepy();
                            }
                            if (entity instanceof AbstractPiglin) {
                                return false;
                            }
                            return entity instanceof Enemy && !(entity instanceof NeutralMob);
                        }
                )),
                Map.entry(PLAYERS, new PredefinedPriorityEntry(PLAYERS, null, entity -> entity instanceof RemotePlayer)),
                Map.entry(SHULKER_BULLETS, new PredefinedPriorityEntry(SHULKER_BULLETS, null, entity -> entity instanceof ShulkerBullet)),
                Map.entry(FIREBALLS, new PredefinedPriorityEntry(FIREBALLS, "Ghast projectiles.", entity -> entity instanceof Fireball))
        );

        private PredefinedPriorityEntry(String name, String description, Predicate<Entity> predicate) {
            super(name, description, predicate);
        }

        public static PredefinedPriorityEntry fromName(String name) {
            return entries.get(name);
        }
    }

    public static class CustomPriorityEntry extends PriorityEntry {

        public final String className;

        private CustomPriorityEntry(String name, String description, Predicate<Entity> predicate, String className) {
            super(name, description, predicate);
            this.className = className;
        }

        public static CustomPriorityEntry create(String name, String description, String className) {
            Class<?> clazz;
            try {
                clazz = Class.forName(ClassRemapper.toObf(className));
            } catch (ClassNotFoundException e) {
                return null;
            }
            return new CustomPriorityEntry(name, description, entity -> clazz.isInstance(entity), className);
        }
    }
}