package com.zergatul.cheatutils.scripting.modules;

import com.zergatul.cheatutils.controllers.DisconnectController;
import com.zergatul.cheatutils.controllers.SpeedCounterController;
import com.zergatul.cheatutils.mixins.common.accessors.MultiPlayerGameModeAccessor;
import com.zergatul.cheatutils.scripting.ApiVisibility;
import com.zergatul.cheatutils.scripting.ApiType;
import com.zergatul.cheatutils.scripting.types.Position3d;
import com.zergatul.cheatutils.scripting.types.BlockPosWrapper;
import com.zergatul.cheatutils.utils.InputBuilder;
import com.zergatul.cheatutils.utils.Rotation;
import com.zergatul.cheatutils.utils.RotationUtils;
import com.zergatul.scripting.MethodDescription;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@SuppressWarnings("unused")
public class PlayerApi {

    private final static Minecraft mc = Minecraft.getInstance();

    public TargetApi target = new TargetApi();
    public EffectsApi effects = new EffectsApi();
    public InteractionsApi interactions = new InteractionsApi();

    @MethodDescription("""
            Sends text to ingame chat. To send commands use player.command(...) method.
            """)
    @ApiVisibility(ApiType.ACTION)
    public void chat(String text) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.connection.sendChat(text);
        }
    }

    @MethodDescription("""
            Sends ingame command to the server
            """)
    @ApiVisibility(ApiType.ACTION)
    public void command(String text) {
        if (text != null && text.startsWith("/")) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.connection.sendCommand(text.substring(1));
            }
        }
    }

    public Position3d getPosition() {
        Entity entity = mc.getCameraEntity();
        if (entity == null) {
            return new Position3d(0, 0, 0);
        }
        return new Position3d(entity.getX(), entity.getY(), entity.getZ());
    }

    @MethodDescription("""
            Returns formatted X/Y/Z player coordinates
            """)
    public String getCoordinatesFormatted() {
        if (mc.getCameraEntity() != null) {
            return String.format(Locale.ROOT, "%.3f / %.5f / %.3f", mc.getCameraEntity().getX(), mc.getCameraEntity().getY(), mc.getCameraEntity().getZ());
        } else {
            return "";
        }
    }

    @MethodDescription("If you are in the Overworld, returns calculated coordinates in the Nether")
    public String getCalculatedNetherCoordinates() {
        if (mc.level == null || mc.level.dimension() == Level.NETHER || mc.getCameraEntity() == null) {
            return "";
        }
        return String.format(Locale.ROOT, "%.3f / %.5f / %.3f", mc.getCameraEntity().getX() / 8, mc.getCameraEntity().getY(), mc.getCameraEntity().getZ() / 8);
    }

    @MethodDescription("If you are in the Nether, returns calculated coordinates in the Overworld")
    public String getCalculatedOverworldCoordinates() {
        if (mc.level == null || mc.level.dimension() == Level.OVERWORLD || mc.getCameraEntity() == null) {
            return "";
        }
        return String.format(Locale.ROOT, "%.3f / %.5f / %.3f", mc.getCameraEntity().getX() * 8, mc.getCameraEntity().getY(), mc.getCameraEntity().getZ() * 8);
    }

    public String getBlockCoordinatesFormatted() {
        if (mc.getCameraEntity() == null) {
            return "";
        }
        BlockPos blockPos = mc.getCameraEntity().blockPosition();
        return String.format(Locale.ROOT, "%d %d %d [%d %d]",
                blockPos.getX(), blockPos.getY(), blockPos.getZ(),
                blockPos.getX() & 15, blockPos.getZ() & 15);
    }

    public String getChunkCoordinatesFormatted() {
        if (mc.getCameraEntity() == null) {
            return "";
        }
        BlockPos blockPos = mc.getCameraEntity().blockPosition();
        ChunkPos chunkPos = new ChunkPos(blockPos);
        return String.format(Locale.ROOT, "%d %d", chunkPos.x, chunkPos.z);
    }

    public String getDirection() {
        if (mc.getCameraEntity() == null) {
            return "";
        }
        Direction direction = mc.getCameraEntity().getDirection();
        return direction.getName();
    }

    public String getBiome() {
        if (mc.level == null || mc.getCameraEntity() == null) {
            return "";
        }
        BlockPos blockPos = mc.getCameraEntity().blockPosition();
        Holder<Biome> holder = mc.level.getBiome(blockPos);
        return holder.unwrap().map(id -> id.location().toString(), biome -> "[unregistered " + biome + "]");
    }

    @MethodDescription("Measured in 0.5 sec window.")
    public String getHorizontalSpeed() {
        return String.format(Locale.ROOT, "%.3f", SpeedCounterController.instance.getHorizontalSpeed());
    }

    @MethodDescription("Measured in 0.5 sec window.")
    public String getSpeed() {
        return String.format(Locale.ROOT, "%.3f", SpeedCounterController.instance.getSpeed());
    }

    public double getX() {
        if (mc.player == null) {
            return 0;
        }
        return mc.player.getX();
    }

    public double getY() {
        if (mc.player == null) {
            return 0;
        }
        return mc.player.getY();
    }

    public double getZ() {
        if (mc.player == null) {
            return 0;
        }
        return mc.player.getZ();
    }

    public double getXRot() {
        if (mc.player == null) {
            return 0;
        }
        return mc.player.getXRot();
    }

    public double getYRot() {
        if (mc.player == null) {
            return 0;
        }
        return mc.player.getYRot();
    }

    @ApiVisibility(ApiType.ACTION)
    public void setXRot(double value) {
        if (mc.player == null) {
            return;
        }
        mc.player.setXRot((float)value);
    }

    @ApiVisibility(ApiType.ACTION)
    public void setYRot(double value) {
        if (mc.player == null) {
            return;
        }
        mc.player.setYRot((float)value);
    }

    public int getHealth() {
        if (mc.player == null) {
            return 0;
        }

        return (int) mc.player.getHealth();
    }

    public int getFood() {
        if (mc.player == null) {
            return 0;
        }

        return mc.player.getFoodData().getFoodLevel();
    }

    public boolean isUnderwater() {
        if (mc.player == null) {
            return false;
        }

        return mc.player.isUnderWater();
    }

    public boolean isElytraFlying() {
        if (mc.player == null) {
            return false;
        }

        return mc.player.isFallFlying();
    }

    public boolean isOnGround() {
        if (mc.player == null) {
            return false;
        }

        return mc.player.onGround();
    }

    public boolean isPassenger() {
        if (mc.player == null) {
            return false;
        }

        return mc.player.isPassenger();
    }

    public boolean isDestroyingBlock() {
        if (mc.gameMode == null) {
            return false;
        }
        MultiPlayerGameModeAccessor mode = (MultiPlayerGameModeAccessor) mc.gameMode;
        return mode.getIsDestroying_CU();
    }

    public int getDestroyingBlockX() {
        if (mc.gameMode == null) {
            return Integer.MIN_VALUE;
        }
        MultiPlayerGameModeAccessor mode = (MultiPlayerGameModeAccessor) mc.gameMode;
        return mode.getIsDestroying_CU() ? mode.getDestroyBlockPos_CU().getX() : Integer.MIN_VALUE;
    }

    public int getDestroyingBlockY() {
        if (mc.gameMode == null) {
            return Integer.MIN_VALUE;
        }
        MultiPlayerGameModeAccessor mode = (MultiPlayerGameModeAccessor) mc.gameMode;
        return mode.getIsDestroying_CU() ? mode.getDestroyBlockPos_CU().getY() : Integer.MIN_VALUE;
    }

    public int getDestroyingBlockZ() {
        if (mc.gameMode == null) {
            return Integer.MIN_VALUE;
        }
        MultiPlayerGameModeAccessor mode = (MultiPlayerGameModeAccessor) mc.gameMode;
        return mode.getIsDestroying_CU() ? mode.getDestroyBlockPos_CU().getZ() : Integer.MIN_VALUE;
    }

    public double getDestroyingBlockProgress() {
        if (mc.gameMode == null) {
            return Integer.MIN_VALUE;
        }
        MultiPlayerGameModeAccessor mode = (MultiPlayerGameModeAccessor) mc.gameMode;
        return mode.getIsDestroying_CU() ? mode.getDestroyProgress_CU() : Double.NaN;
    }

    @ApiVisibility(ApiType.ACTION)
    public void lookAt(double x, double y, double z) {
        if (mc.player == null) {
            return;
        }
        Rotation rotation = RotationUtils.getRotation(mc.player.getEyePosition(), new Vec3(x, y, z));
        mc.player.setXRot(rotation.xRot());
        mc.player.setYRot(rotation.yRot());
    }

    @MethodDescription("""
            Allowed disconnect types: "self-attack", "invalid-chars". Anything else (for example "") - normal disconnect.
            """)
    @ApiVisibility(ApiType.ACTION)
    public void disconnect(String type) {
        switch (type) {
            case "self-attack" -> DisconnectController.instance.selfAttack(null);
            case "invalid-chars" -> DisconnectController.instance.invalidChars(null);
            default -> DisconnectController.instance.disconnect(null);
        }
    }

    @MethodDescription("""
            Allowed disconnect types: "self-attack", "invalid-chars". Anything else (for example "") - normal disconnect.
            You can specify custom message to be displayed at the disconnect screen
            """)
    @ApiVisibility(ApiType.ACTION)
    public void disconnect(String type, String message) {
        switch (type) {
            case "self-attack" -> DisconnectController.instance.selfAttack(message);
            case "invalid-chars" -> DisconnectController.instance.invalidChars(message);
            default -> DisconnectController.instance.disconnect(message);
        }
    }

    @ApiVisibility(ApiType.ACTION)
    public boolean attack(int entityId) {
        if (mc.level == null) {
            return false;
        }
        if (mc.player == null) {
            return false;
        }
        if (mc.gameMode == null) {
            return false;
        }

        Entity entity = mc.level.getEntity(entityId);
        if (entity == null) {
            return false;
        }

        mc.gameMode.attack(mc.player, entity);
        mc.player.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    @MethodDescription("Returns [0..1]. 0 means attack will do full damage.")
    public double getAttackCooldown() {
        if (mc.player == null) {
            return Double.NaN;
        }
        return 1d - mc.player.getAttackStrengthScale(0);
    }

    public static class TargetApi {

        public boolean hasBlock() {
            if (mc.hitResult == null) {
                return false;
            }

            return mc.hitResult.getType() == HitResult.Type.BLOCK;
        }

        public BlockPosWrapper getBlockPos() {
            if (mc.hitResult instanceof BlockHitResult hitResult) {
                return new BlockPosWrapper(hitResult.getBlockPos());
            } else {
                return new BlockPosWrapper(0, 0, 0);
            }
        }

        public int getBlockX() {
            if (mc.hitResult instanceof BlockHitResult hitResult) {
                return hitResult.getBlockPos().getX();
            } else {
                return Integer.MIN_VALUE;
            }
        }

        public int getBlockY() {
            if (mc.hitResult instanceof BlockHitResult hitResult) {
                return hitResult.getBlockPos().getY();
            } else {
                return Integer.MIN_VALUE;
            }
        }

        public int getBlockZ() {
            if (mc.hitResult instanceof BlockHitResult hitResult) {
                return hitResult.getBlockPos().getZ();
            } else {
                return Integer.MIN_VALUE;
            }
        }

        public boolean hasEntity() {
            if (mc.hitResult == null) {
                return false;
            }

            return mc.hitResult.getType() == HitResult.Type.ENTITY;
        }

        public int getEntityId() {
            if (mc.hitResult instanceof EntityHitResult hitResult) {
                return hitResult.getEntity().getId();
            } else {
                return Integer.MIN_VALUE;
            }
        }

        public String getBlockCoordinatesFormatted() {
            if (mc.level == null) {
                return "";
            }

            Entity entity = mc.getCameraEntity();
            if (entity == null) {
                return "";
            }

            HitResult result = entity.pick(20.0D, 0.0F, false);
            if (result.getType() == HitResult.Type.BLOCK) {
                BlockPos blockPos = ((BlockHitResult) result).getBlockPos();
                return blockPos.getX() + ", " + blockPos.getY() + ", " + blockPos.getZ();
            } else {
                return "";
            }
        }

        public String getBlockName() {
            if (mc.level == null) {
                return "";
            }

            Entity entity = mc.getCameraEntity();
            if (entity == null) {
                return "";
            }

            HitResult result = entity.pick(20.0D, 0.0F, false);
            if (result.getType() == HitResult.Type.BLOCK) {
                BlockPos blockPos = ((BlockHitResult) result).getBlockPos();
                BlockState blockState = mc.level.getBlockState(blockPos);
                return com.zergatul.cheatutils.common.Registries.BLOCKS.getKey(blockState.getBlock()).toString();
            } else {
                return "";
            }
        }

        public BlockPosWrapper getBlockPosInRange(double maxRange, boolean captureFluids) {
            if (mc.level == null || mc.player == null) {
                return new BlockPosWrapper(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
            }

            HitResult result = mc.player.pick(maxRange, 0, captureFluids);
            if (result instanceof BlockHitResult hitResult && hitResult.getType() == HitResult.Type.BLOCK) {
                return new BlockPosWrapper(hitResult.getBlockPos());
            } else {
                return new BlockPosWrapper(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
            }
        }
    }

    public static class EffectsApi {

        @MethodDescription("If player has no effect, returns 0")
        public int getLevel(String id) {
            if (mc.level == null) {
                return 0;
            }
            if (mc.player == null) {
                return 0;
            }

            HolderLookup<MobEffect> lookup = mc.level.holderLookup(Registries.MOB_EFFECT);
            ResourceLocation location = ResourceLocation.parse(id);
            Holder<MobEffect> holder = lookup.listElements().filter(ref -> ref.key().location().equals(location)).findFirst().orElse(null);
            if (holder == null) {
                return Integer.MIN_VALUE;
            }

            MobEffectInstance instance = mc.player.getActiveEffectsMap().get(holder);
            if (instance == null) {
                return 0;
            }

            return instance.getAmplifier() + 1;
        }

        @MethodDescription("If player has no effect, returns 0")
        public double getDuration(String id) {
            if (mc.level == null) {
                return 0;
            }
            if (mc.player == null) {
                return 0;
            }

            HolderLookup<MobEffect> lookup = mc.level.holderLookup(Registries.MOB_EFFECT);
            ResourceLocation location = ResourceLocation.parse(id);
            Holder<MobEffect> holder = lookup.listElements().filter(ref -> ref.key().location().equals(location)).findFirst().orElse(null);
            if (holder == null) {
                return -1;
            }

            MobEffectInstance instance = mc.player.getActiveEffectsMap().get(holder);
            if (instance == null) {
                return 0;
            }

            return instance.getDuration() / 20d;
        }
    }

    public static class InteractionsApi {

        @ApiVisibility(ApiType.ACTION)
        public void openClosestChestBoat() {
            assert mc.gameMode != null;

            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.player != null) {
                Stream<ChestBoat> boats = StreamSupport
                        .stream(mc.level.entitiesForRendering().spliterator(), false)
                        .filter(e -> e instanceof ChestBoat)
                        .map(e -> (ChestBoat) e);
                double minDistance = Double.MAX_VALUE;
                ChestBoat target = null;
                for (ChestBoat boat: boats.toList()) {
                    double d2 = mc.player.distanceToSqr(boat);
                    if (d2 < minDistance) {
                        minDistance = d2;
                        target = boat;
                    }
                }

                if (target == null) {
                    return;
                }

                Input oldInput = mc.player.input.keyPresses;
                mc.player.input.keyPresses = new InputBuilder(oldInput).shift(true).build();

                mc.gameMode.interactAt(mc.player, target, new EntityHitResult(target), InteractionHand.MAIN_HAND);
                mc.gameMode.interact(mc.player, target, InteractionHand.MAIN_HAND);
                mc.player.swing(InteractionHand.MAIN_HAND);

                mc.player.input.keyPresses = oldInput;
            }
        }

        @ApiVisibility(ApiType.ACTION)
        public void openTradingWithClosestVillager() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.player != null) {
                Villager villager = StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), false)
                        .filter(Villager.class::isInstance)
                        .map(e -> (Villager) e)
                        .min(Comparator.comparingDouble(v -> mc.player.distanceToSqr(v)))
                        .orElse(null);
                if (villager == null) {
                    return;
                }
                interactWithEntity(mc, villager);
            }
        }

        private void interactWithEntity(Minecraft mc, Entity entity) {
            assert mc.player != null;
            assert mc.gameMode != null;

            mc.gameMode.interactAt(mc.player, entity, new EntityHitResult(entity), InteractionHand.MAIN_HAND);
            mc.gameMode.interact(mc.player, entity, InteractionHand.MAIN_HAND);
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
    }
}