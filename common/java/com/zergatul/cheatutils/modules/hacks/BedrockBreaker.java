package com.zergatul.cheatutils.modules.hacks;

import com.mojang.datafixers.util.Pair;
import com.zergatul.cheatutils.common.Events;
import com.zergatul.cheatutils.common.Registries;
import com.zergatul.cheatutils.configs.BedrockBreakerConfig;
import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.mixins.common.accessors.BlockBehaviourAccessor;
import com.zergatul.cheatutils.mixins.common.accessors.ClientLevelAccessor;
import com.zergatul.cheatutils.modules.Module;
import com.zergatul.cheatutils.scripting.Root;
import com.zergatul.cheatutils.utils.BlockPlacingMethod;
import com.zergatul.cheatutils.utils.BlockUtils;
import com.zergatul.cheatutils.utils.NearbyBlockEnumerator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class BedrockBreaker implements Module {

    public static final BedrockBreaker instance = new BedrockBreaker();

    private final Minecraft mc = Minecraft.getInstance();
    private final Direction[] horizontal = new Direction[] { Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH };
    private final Queue<BlockPos> queue = new ArrayDeque<>();
    private BlockPos bedrockPos;
    private BlockPos torchPos;
    private BlockPos supportBlockPos;
    private State state = State.INIT;
    private int tickCount;

    private BedrockBreaker() {
        Events.ClientTickEnd.add(this::onClientTickEnd);
    }

    public void process() {
        if (mc.player == null) {
            return;
        }
        if (mc.level == null) {
            return;
        }
        if (mc.hitResult == null) {
            return;
        }
        if (mc.hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockHitResult hitResult = (BlockHitResult) mc.hitResult;
        BlockPos pos = hitResult.getBlockPos();
        if (mc.level.getBlockState(pos).getBlock() != Blocks.BEDROCK) {
            return;
        }

        if (bedrockPos != null && bedrockPos.equals(pos)) {
            return;
        }

        if (state == State.INIT) {
            start(pos);
        } else {
            queue.add(pos.immutable());
        }
    }

    public void processNearby() {
        if (mc.player == null) {
            return;
        }
        if (mc.level == null) {
            return;
        }

        //Predicate<BlockPos> condition = pos -> Math.abs(pos.getX()) <= 100 && Math.abs(pos.getZ()) <= 100;
        Predicate<BlockPos> condition = pos -> true;

        Vec3 eyePos = mc.player.getEyePosition(1);
        List<BlockPos> positions = NearbyBlockEnumerator.getPositions(eyePos, 4);
        Map<Integer, List<BlockPos>> map = positions.stream().collect(Collectors.groupingBy(Vec3i::getY));
        for (int y : map.keySet().stream().sorted(Comparator.reverseOrder()).toList()) {
            List<BlockPos> layer = map.get(y);
            if (layer.stream().anyMatch(p -> condition.test(p) && mc.level.getBlockState(p).is(Blocks.BEDROCK))) {
                for (BlockPos pos : layer) {
                    if (condition.test(pos) && mc.level.getBlockState(pos).is(Blocks.BEDROCK)) {
                        queue.add(pos);
                    }
                }
                break;
            }
        }
    }

    private void onClientTickEnd() {
        if (mc.player == null) {
            return;
        }
        if (mc.level == null) {
            return;
        }

        if (state == State.INIT) {
            if (!queue.isEmpty()) {
                start(queue.remove());
            }
        }

        if (bedrockPos == null) {
            return;
        }

        tickCount++;

        switch (state) {
            case START -> handleStartState();
            case PLACE_TORCH -> handlePlaceTorch();
            case WAIT_SUPPORT_BLOCK -> handleWaitSupportBlock();
            case WAIT_PISTON_EXTEND -> handleWaitPistonExtendState();
            case WAIT_BEDROCK_BREAK -> handleWaitBedrockBreakState();
        }
    }

    private void handleStartState() {
        assert mc.player != null;
        assert mc.level != null;

        int pistonSlot = findItem(Items.PISTON);
        if (pistonSlot < 0) {
            reset("Cannot find piston on hotbar");
            return;
        }

        BlockUtils.PlaceBlockPlan plan = BlockUtils.getPlacingPlan(bedrockPos.above(), false, BlockPlacingMethod.FACING_TOP);
        if (plan == null) {
            reset("Cannot place initial piston");
            return;
        }

        mc.player.connection.send(new ServerboundSetCarriedItemPacket(pistonSlot));
        mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(
                mc.player.getYRot(),
                90, // look down
                mc.player.onGround(),
                false));
        mc.player.connection.send(new ServerboundUseItemOnPacket(
                InteractionHand.MAIN_HAND,
                new BlockHitResult(plan.target(), plan.direction(), plan.neighbour(), false),
                getSequenceNumber()));

        state = State.PLACE_TORCH;
        handlePlaceTorch();
    }

    private void handlePlaceTorch() {
        assert mc.player != null;
        assert mc.level != null;

        int torchSlot = findItem(Items.REDSTONE_TORCH);
        if (torchSlot < 0) {
            reset("Cannot find redstone torch on hotbar");
            return;
        }

        torchPos = null;
        for (Direction direction : sortByDistance(bedrockPos.above(), horizontal)) {
            BlockPos pos = bedrockPos.above().relative(direction);
            BlockState state = mc.level.getBlockState(pos);
            if (((BlockBehaviourAccessor) Blocks.REDSTONE_TORCH).canSurvive_CU(Blocks.REDSTONE_TORCH.defaultBlockState(), mc.level, pos) && state.canBeReplaced()) {
                torchPos = pos;
                break;
            }
        }

        BedrockBreakerConfig config = ConfigStore.instance.getConfig().bedrockBreakerConfig;
        if (torchPos == null) {
            if (!config.placeSupportBlock) {
                reset("Cannot find location to place torch");
            } else {
                state = State.PLACE_SUPPORT_BLOCK;
                handlePlaceSupportBlock();
            }
            return;
        }

        BlockUtils.PlaceBlockPlan plan = BlockUtils.getPlacingPlan(torchPos, false, BlockPlacingMethod.FROM_TOP, Blocks.REDSTONE_TORCH.defaultBlockState());
        if (plan == null) {
            reset("Cannot place redstone torch");
            return;
        }

        mc.player.connection.send(new ServerboundSetCarriedItemPacket(torchSlot));
        mc.player.connection.send(new ServerboundUseItemOnPacket(
                InteractionHand.MAIN_HAND,
                new BlockHitResult(plan.target(), plan.direction(), plan.neighbour(), false),
                getSequenceNumber()));

        tickCount = 0;
        state = State.WAIT_PISTON_EXTEND;
    }

    private void handlePlaceSupportBlock() {
        assert mc.player != null;
        assert mc.level != null;

        BedrockBreakerConfig config = ConfigStore.instance.getConfig().bedrockBreakerConfig;
        ResourceLocation supportBlockId = ResourceLocation.parse(config.supportBlockId);
        int supportBlockSlot = findItem(Registries.ITEMS.getValue(supportBlockId));
        if (supportBlockSlot < 0) {
            reset("Cannot find support block (" + config.supportBlockId + ") on hotbar");
            return;
        }

        supportBlockPos = null;
        BlockUtils.PlaceBlockPlan supportBlockPlan = null;
        for (Direction direction : sortByDistance(bedrockPos, horizontal)) {
            BlockPos pos = bedrockPos.relative(direction);
            supportBlockPlan = BlockUtils.getPlacingPlan(pos, false);
            if (supportBlockPlan != null) {
                supportBlockPos = pos;
                break;
            }
        }

        if (supportBlockPos == null) {
            reset("Cannot find location for support block");
            return;
        }

        mc.player.connection.send(new ServerboundSetCarriedItemPacket(supportBlockSlot));
        mc.player.connection.send(new ServerboundUseItemOnPacket(
                InteractionHand.MAIN_HAND,
                new BlockHitResult(supportBlockPlan.target(), supportBlockPlan.direction(), supportBlockPlan.neighbour(), false),
                getSequenceNumber()));

        tickCount = 0;
        state = State.WAIT_SUPPORT_BLOCK;
    }

    private void handleWaitSupportBlock() {
        assert mc.player != null;
        assert mc.level != null;

        if (supportBlockPos == null) {
            reset("Support block pos is null");
            return;
        }

        BedrockBreakerConfig config = ConfigStore.instance.getConfig().bedrockBreakerConfig;
        ResourceLocation supportBlockId = ResourceLocation.parse(config.supportBlockId);
        Block supportBlock = Registries.BLOCKS.getValue(supportBlockId);
        if (mc.level.getBlockState(supportBlockPos).getBlock() == supportBlock) {
            state = State.PLACE_TORCH;
            handlePlaceTorch();
        } else {
            if (tickCount > 10) {
                reset("Wait for support block timeout");
            }
        }
    }

    private void handleWaitPistonExtendState() {
        assert mc.player != null;
        assert mc.level != null;

        if (mc.level.getBlockState(bedrockPos.above().above()).getBlock() == Blocks.PISTON_HEAD) {
            // destroy torch
            mc.player.connection.send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    torchPos,
                    Direction.UP,
                    getSequenceNumber()));

            mc.level.destroyBlock(torchPos, false);

            int pickaxeSlot = findItem(Items.NETHERITE_PICKAXE);
            if (pickaxeSlot < 0) {
                reset("Cannot select pickaxe");
                return;
            }

            // destroy piston
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(pickaxeSlot));
            mc.player.connection.send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    bedrockPos.above(),
                    Direction.UP,
                    getSequenceNumber()));

            mc.level.destroyBlock(bedrockPos.above(), false);

            int pistonSlot = findItem(Items.PISTON);
            if (pistonSlot < 0) {
                reset("Cannot select piston");
                return;
            }

            BlockUtils.PlaceBlockPlan plan = BlockUtils.getPlacingPlan(bedrockPos.above(), false, BlockPlacingMethod.FACING_BOTTOM);
            if (plan == null) {
                reset("Cannot place reverse piston");
                return;
            }

            mc.player.connection.send(new ServerboundSetCarriedItemPacket(pistonSlot));
            mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(
                    mc.player.getYRot(),
                    -90, // look up
                    mc.player.onGround(),
                    false));
            mc.player.connection.send(new ServerboundUseItemOnPacket(
                    InteractionHand.MAIN_HAND,
                    new BlockHitResult(plan.target(), plan.direction(), plan.neighbour(), false),
                    getSequenceNumber()));

            state = State.WAIT_BEDROCK_BREAK;
        } else {
            if (tickCount > 10) {
                reset("Wait for piston extend timeout");
            }
        }
    }

    private void handleWaitBedrockBreakState() {
        assert mc.player != null;
        assert mc.level != null;

        if (mc.level.getBlockState(bedrockPos).isAir()) {
            if (mc.level.getBlockState(bedrockPos.above()).is(Blocks.MOVING_PISTON)) {
                // wait until moving piston converts to normal
                return;
            }

            int pickaxeSlot = findItem(Items.NETHERITE_PICKAXE);
            if (pickaxeSlot < 0) {
                reset("Cannot select pickaxe");
                return;
            }

            mc.player.connection.send(new ServerboundSetCarriedItemPacket(pickaxeSlot));
            mc.player.connection.send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    bedrockPos.above(),
                    Direction.DOWN,
                    getSequenceNumber()));

            BedrockBreakerConfig config = ConfigStore.instance.getConfig().bedrockBreakerConfig;
            if (config.replace) {
                Item item = Registries.ITEMS.getValue(ResourceLocation.parse(config.replaceBlockId));
                if (item == null) {
                    reset(config.replaceBlockId + " is not valid block ID");
                    return;
                }

                int replaceBlockSlot = findItem(item);
                if (replaceBlockSlot < 0) {
                    reset("Cannot select replacement block");
                    return;
                }

                BlockUtils.PlaceBlockPlan plan = BlockUtils.getPlacingPlan(bedrockPos, false, BlockPlacingMethod.FROM_HORIZONTAL);
                if (plan == null) {
                    reset("Cannot place replacement block");
                    return;
                }

                mc.player.connection.send(new ServerboundSetCarriedItemPacket(replaceBlockSlot));
                mc.player.connection.send(new ServerboundUseItemOnPacket(
                        InteractionHand.MAIN_HAND,
                        new BlockHitResult(plan.target(), plan.direction(), plan.neighbour(), false),
                        getSequenceNumber()));
            }

            reset(null);

            if (!queue.isEmpty()) {
                start(queue.remove());
            }
        } else {
            if (tickCount > 20) {
                reset("Wait for bedrock break timeout");
            }
        }
    }

    private int findItem(Item item) {
        assert mc.player != null;

        Inventory inventory = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inventory.getItem(i).is(item)) {
                return i;
            }
        }

        return -1;
    }

    private int getSequenceNumber() {
        assert mc.level != null;

        BlockStatePredictionHandler handler = ((ClientLevelAccessor) mc.level).getBlockStatePredictionHandler_CU();
        handler.startPredicting();
        int num = handler.currentSequence();
        handler.close();
        return num;
    }

    private Direction[] sortByDistance(BlockPos origin, Direction[] directions) {
        return Arrays.stream(directions)
                .map(d -> new Pair<>(d, origin.relative(d).distToCenterSqr(mc.player.getEyePosition())))
                .sorted(Comparator.comparingDouble(Pair::getSecond))
                .map(p -> p.getFirst())
                .toArray(Direction[]::new);
    }

    private void start(BlockPos pos) {
        if (mc.level == null) {
            return;
        }

        if (!mc.level.getBlockState(pos).is(Blocks.BEDROCK)) {
            return;
        }

        bedrockPos = pos;
        state = State.START;
        tickCount = 0;
    }

    private void reset(String message) {
        bedrockPos = null;
        state = State.INIT;
        tickCount = 0;

        if (mc.player != null) {
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(mc.player.getInventory().selected));
        }

        if (message != null) {
            Root.ui.systemMessage(message);
        }
    }

    private enum State {
        INIT,
        START,
        PLACE_TORCH,
        PLACE_SUPPORT_BLOCK,
        WAIT_SUPPORT_BLOCK,
        WAIT_PISTON_EXTEND,
        WAIT_BEDROCK_BREAK
    }
}