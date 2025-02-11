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
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class BedrockBreaker implements Module {

    public static final BedrockBreaker instance = new BedrockBreaker();

    private final Minecraft mc = Minecraft.getInstance();
    private final Direction[] horizontal = new Direction[] { Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH };
    private final Queue<BlockPos> queue = new ArrayDeque<>();
    private BlockPos bedrockPos;
    private Direction pistonDirection;
    private BlockPos pistonPos;
    private float blockDestroyProgress;
    private int blockDestroySeqNumber;
    private Direction leverDirection;
    private BlockPos leverPos;
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
        if (!isValidBlock(pos)) {
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

        BedrockBreakerConfig config = ConfigStore.instance.getConfig().bedrockBreakerConfig;

        //Predicate<BlockPos> condition = pos -> Math.abs(pos.getX()) <= 100 && Math.abs(pos.getZ()) <= 100;
        Predicate<BlockPos> condition = pos -> true;

        Vec3 eyePos = mc.player.getEyePosition(1);
        List<BlockPos> positions = NearbyBlockEnumerator.getPositions(eyePos, 4);
        Map<Integer, List<BlockPos>> map = positions.stream().collect(Collectors.groupingBy(Vec3i::getY));
        for (int y : map.keySet().stream().sorted(Comparator.reverseOrder()).toList()) {
            List<BlockPos> layer = map.get(y);
            if (layer.stream().anyMatch(p -> condition.test(p) && isValidBlock(config, p))) {
                for (BlockPos pos : layer) {
                    if (condition.test(pos) && isValidBlock(config, pos)) {
                        queue.add(pos);
                    }
                }
                break;
            }
        }
    }

    public String getStatus() {
        return switch (state) {
            case BREAK_PISTON_PROGRESS, BREAK_REMAINING_PISTON_PROGRESS -> state + " " + Math.round(blockDestroyProgress * 100) + "%";
            default -> state.toString();
        };
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
        state.handle(this);
    }

    private void handleStart() {
        assert mc.player != null;
        assert mc.level != null;

        int pistonSlot = findItem(Items.PISTON);
        if (pistonSlot < 0) {
            reset("Cannot find piston on hotbar");
            return;
        }

        // check if we have 2 free blocks in some direction: one block for piston and second block for extended piston
        pistonDirection = null;
        for (Direction direction : sortByDistance(bedrockPos, new Direction[] { Direction.UP, Direction.DOWN })) {
            BlockPos piston1 = bedrockPos.relative(direction);
            if (!mc.level.getBlockState(piston1).canBeReplaced()) {
                continue;
            }
            BlockPos piston2 = piston1.relative(direction);
            if (!mc.level.getBlockState(piston2).canBeReplaced()) {
                continue;
            }
            if (!isValidY(piston2.getY())) {
                continue;
            }
            pistonDirection = direction;
            break;
        }

        if (pistonDirection == null) {
            reset("Cannot find location to place piston");
            return;
        }

        pistonPos = bedrockPos.relative(pistonDirection);

        leverDirection = null;
        leverPos = null;
        // find location for lever around bedrock
        for (Direction direction : sortByDistance(bedrockPos, Direction.values())) {
            if (direction == pistonDirection) {
                continue;
            }
            if (!mc.level.getBlockState(bedrockPos.relative(direction)).canBeReplaced()) {
                continue;
            }
            if (!isValidY(bedrockPos.relative(direction).getY())) {
                continue;
            }

            BlockPos possibleLeverPos = bedrockPos.relative(direction);
            BlockState leverBlockState = Blocks.LEVER.getStateForPlacement(new BlockPlaceContext(
                    mc.player,
                    InteractionHand.MAIN_HAND,
                    new ItemStack(Items.LEVER, 1),
                    new BlockHitResult(possibleLeverPos.getCenter(), direction.getOpposite(), bedrockPos, false)));
            if (leverBlockState == null) {
                continue;
            }
            if (((BlockBehaviourAccessor) Blocks.LEVER).canSurvive_CU(leverBlockState, mc.level, possibleLeverPos)) {
                leverDirection = direction;
                leverPos = possibleLeverPos;
                break;
            }
        }
        if (leverPos == null) {
            // find location for lever around piston
            leverLocSearch:
            for (Direction direction : sortByDistance(pistonPos, Direction.values())) {
                if (direction == pistonDirection) {
                    continue;
                }
                if (direction == pistonDirection.getOpposite()) {
                    continue;
                }
                BlockPos possibleLeverPos = pistonPos.relative(direction);
                if (!isValidY(possibleLeverPos.getY())) {
                    continue;
                }
                for (Direction dir : sortByDistance(possibleLeverPos, Direction.values())) {
                    BlockPos possibleSupportBlockPos = possibleLeverPos.relative(dir);
                    if (possibleSupportBlockPos.equals(pistonPos)) {
                        continue; // do not attach lever to piston
                    }
                    if (mc.level.getBlockState(possibleSupportBlockPos).canBeReplaced()) {
                        continue;
                    }
                    BlockState leverBlockState = Blocks.LEVER.getStateForPlacement(new BlockPlaceContext(
                            mc.player,
                            InteractionHand.MAIN_HAND,
                            new ItemStack(Items.LEVER, 1),
                            new BlockHitResult(possibleLeverPos.getCenter(), dir.getOpposite(), possibleSupportBlockPos, false)));
                    if (leverBlockState == null) {
                        continue;
                    }
                    if (((BlockBehaviourAccessor) Blocks.LEVER).canSurvive_CU(leverBlockState, mc.level, possibleLeverPos)) {
                        leverDirection = dir;
                        leverPos = possibleLeverPos;
                        break leverLocSearch;
                    }
                }
            }
        }

        if (leverPos == null) {
            reset("Cannot find location to place lever");
            return;
        }

        BlockUtils.PlaceBlockPlan plan = BlockUtils.getPlacingPlan(pistonPos, false, BlockPlacingMethod.facing(pistonDirection));
        if (plan == null) {
            reset("Cannot place initial piston");
            return;
        }

        mc.player.connection.send(new ServerboundSetCarriedItemPacket(pistonSlot));
        mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(
                Float.isNaN(plan.rotation().yRot()) ? mc.player.getYRot() : plan.rotation().yRot(),
                Float.isNaN(plan.rotation().xRot()) ? mc.player.getXRot() : plan.rotation().xRot(),
                mc.player.onGround(),
                false));
        mc.player.connection.send(new ServerboundUseItemOnPacket(
                InteractionHand.MAIN_HAND,
                new BlockHitResult(plan.target(), plan.direction(), plan.neighbour(), false),
                getSequenceNumber()));

        state = State.PLACE_LEVER;
        state.handle(this);
    }

    private void handlePlaceLever() {
        assert mc.player != null;
        assert mc.level != null;

        int leverSlot = findItem(Items.LEVER);
        if (leverSlot < 0) {
            reset("Cannot find lever on hotbar");
            return;
        }

        BlockUtils.PlaceBlockPlan plan = BlockUtils.getPlacingPlan(leverPos, false, switch (leverDirection) {
            case NORTH -> BlockPlacingMethod.FROM_NORTH;
            case SOUTH -> BlockPlacingMethod.FROM_SOUTH;
            case EAST -> BlockPlacingMethod.FROM_EAST;
            case WEST -> BlockPlacingMethod.FROM_WEST;
            default -> BlockPlacingMethod.ANY;
        }, Blocks.LEVER.defaultBlockState());
        if (plan == null) {
            reset("Cannot place lever");
            return;
        }

        mc.player.connection.send(new ServerboundSetCarriedItemPacket(leverSlot));
        mc.player.connection.send(new ServerboundUseItemOnPacket(
                InteractionHand.MAIN_HAND,
                new BlockHitResult(plan.target(), plan.direction(), plan.neighbour(), false),
                getSequenceNumber()));

        state = State.BREAK_PISTON_START;
        state.handle(this);
    }

    private void handleBreakPistonStart() {
        assert mc.level != null;
        assert mc.player != null;

        tickCount = 0;

        // equip pickaxe if present
        int pickaxeSlot = findPickaxe();
        if (pickaxeSlot >= 0) {
            mc.player.getInventory().selected = pickaxeSlot;
        }
        mc.player.connection.send(new ServerboundSetCarriedItemPacket(mc.player.getInventory().selected));

        blockDestroyProgress = getPistonDestroyProgress();
        blockDestroySeqNumber = getSequenceNumber();

        mc.player.connection.send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                pistonPos,
                Direction.UP,
                blockDestroySeqNumber));

        state = State.BREAK_PISTON_PROGRESS;
        tickCount = 0;
    }

    private void handleBreakPistonProgress() {
        assert mc.level != null;
        assert mc.player != null;

        blockDestroyProgress += getPistonDestroyProgress();
        if (blockDestroyProgress >= 1) {
            // activate lever
            mc.player.connection.send(new ServerboundUseItemOnPacket(
                    InteractionHand.MAIN_HAND,
                    new BlockHitResult(leverPos.getCenter(), Direction.UP, leverPos, false),
                    getSequenceNumber()));

            state = State.WAIT_PISTON_EXTEND;
            tickCount = 0;
        } else {
            if (tickCount > 50) {
                reset("Wait piston break timeout");
            }
        }
    }

    private void handleWaitPistonExtend() {
        assert mc.player != null;
        assert mc.level != null;
        assert mc.gameMode != null;

        if (mc.level.getBlockState(pistonPos.relative(pistonDirection)).getBlock() == Blocks.PISTON_HEAD) {
            // deactivate lever
            mc.player.connection.send(new ServerboundUseItemOnPacket(
                    InteractionHand.MAIN_HAND,
                    new BlockHitResult(leverPos.getCenter(), Direction.UP, leverPos, false),
                    getSequenceNumber()));

            // break piston
            mc.player.connection.send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                    pistonPos,
                    Direction.UP,
                    blockDestroySeqNumber));

            mc.level.destroyBlock(pistonPos, false);
            mc.level.destroyBlock(pistonPos.relative(pistonDirection), false);

            state = State.PLACE_REVERSE_PISTON;
            state.handle(this);
        } else {
            if (tickCount > 10) {
                reset("Wait for piston extend timeout");
            }
        }
    }

    private void handlePlaceReversePiston() {
        assert mc.level != null;
        assert mc.player != null;

        int pistonSlot = findItem(Items.PISTON);
        if (pistonSlot < 0) {
            reset("Cannot select piston");
            return;
        }

        BlockUtils.PlaceBlockPlan plan = BlockUtils.getPlacingPlan(pistonPos, false, BlockPlacingMethod.facing(pistonDirection.getOpposite()));
        if (plan == null) {
            reset("Cannot place reverse piston");
            return;
        }

        mc.player.connection.send(new ServerboundSetCarriedItemPacket(pistonSlot));
        mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(
                Float.isNaN(plan.rotation().yRot()) ? mc.player.getYRot() : plan.rotation().yRot(),
                Float.isNaN(plan.rotation().xRot()) ? mc.player.getXRot() : plan.rotation().xRot(),
                mc.player.onGround(),
                false));
        mc.player.connection.send(new ServerboundUseItemOnPacket(
                InteractionHand.MAIN_HAND,
                new BlockHitResult(plan.target(), plan.direction(), plan.neighbour(), false),
                getSequenceNumber()));

        state = State.WAIT_BEDROCK_BREAK;
        tickCount = 0;
    }

    private void handleWaitBedrockBreak() {
        assert mc.player != null;
        assert mc.level != null;

        if (mc.level.getBlockState(bedrockPos).isAir() && !mc.level.getBlockState(pistonPos.relative(pistonDirection)).is(Blocks.MOVING_PISTON)) {
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

            if (mc.level.getBlockState(leverPos).is(Blocks.LEVER)) {
                state = State.BREAK_REMAINING_LEVER_START;
            } else {
                state = State.BREAK_REMAINING_PISTON_START;
            }
            state.handle(this);
            tickCount = 0;
        } else {
            if (tickCount > 20) {
                reset("Wait for bedrock break timeout");
            }
        }
    }

    private void handleBreakRemainingLeverStart() {
        assert mc.player != null;

        blockDestroyProgress = getLeverDestroyProgress();
        blockDestroySeqNumber = getSequenceNumber();

        mc.player.connection.send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                leverPos,
                Direction.DOWN,
                blockDestroySeqNumber));

        state = State.BREAK_REMAINING_LEVER_PROGRESS;
    }

    private void handleBreakRemainingLeverProgress() {
        assert mc.player != null;

        blockDestroyProgress += getLeverDestroyProgress();
        if (blockDestroyProgress >= 1) {
            mc.player.connection.send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                    leverPos,
                    Direction.DOWN,
                    blockDestroySeqNumber));

            state = State.BREAK_REMAINING_PISTON_START;
            state.handle(this);
        } else {
            if (tickCount > 30) {
                reset("Lever break timeout");
            }
        }
    }

    private void handleBreakRemainingPistonStart() {
        assert mc.player != null;

        int pickaxeSlot = findPickaxe();
        if (pickaxeSlot >= 0) {
            mc.player.getInventory().selected = pickaxeSlot;
        }

        mc.player.connection.send(new ServerboundSetCarriedItemPacket(mc.player.getInventory().selected));

        blockDestroyProgress = getPistonDestroyProgress();
        blockDestroySeqNumber = getSequenceNumber();

        mc.player.connection.send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                pistonPos,
                Direction.DOWN,
                blockDestroySeqNumber));

        state = State.BREAK_REMAINING_PISTON_PROGRESS;
        tickCount = 0;
    }

    private void handleBreakRemainingPistonProgress() {
        assert mc.player != null;
        assert mc.level != null;

        blockDestroyProgress += getPistonDestroyProgress();
        if (blockDestroyProgress >= 1) {
            mc.player.connection.send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                    pistonPos,
                    Direction.DOWN,
                    blockDestroySeqNumber));

            mc.level.destroyBlock(pistonPos, false);

            reset(null);

            if (!queue.isEmpty()) {
                start(queue.remove());
            }
        } else {
            if (tickCount > 50) {
                reset("Wait for remaining piston break timeout");
            }
        }
    }

    private float getPistonDestroyProgress() {
        assert mc.level != null;
        assert mc.player != null;

        return Blocks.PISTON.defaultBlockState().getDestroyProgress(mc.player, mc.level, pistonPos);
    }

    private float getLeverDestroyProgress() {
        assert mc.level != null;
        assert mc.player != null;

        return Blocks.LEVER.defaultBlockState().getDestroyProgress(mc.player, mc.level, pistonPos);
    }

    private boolean isValidBlock(BlockPos pos) {
        return isValidBlock(ConfigStore.instance.getConfig().bedrockBreakerConfig, pos);
    }

    private boolean isValidBlock(BedrockBreakerConfig config, BlockPos pos) {
        assert mc.level != null;
        return config.allBlocks || mc.level.getBlockState(pos).is(Blocks.BEDROCK);
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

    private int findPickaxe() {
        assert mc.player != null;

        Inventory inventory = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inventory.getItem(i).getTags().anyMatch(tag -> tag == ItemTags.PICKAXES)) {
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

    private boolean isValidY(int y) {
        assert mc.level != null;

        DimensionType dimension = mc.level.dimensionType();
        return dimension.minY() <= y && y < dimension.minY() + dimension.height();
    }

    private Direction[] sortByDistance(BlockPos origin, Direction[] directions) {
        assert mc.player != null;

        return Arrays.stream(directions)
                .map(d -> new Pair<>(d, origin.relative(d).distToCenterSqr(mc.player.getEyePosition())))
                .sorted(Comparator.comparingDouble(Pair::getSecond))
                .map(Pair::getFirst)
                .toArray(Direction[]::new);
    }

    private void start(BlockPos pos) {
        if (mc.level == null) {
            return;
        }

        if (!isValidBlock(pos)) {
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
        INIT(instance -> {}),
        START(BedrockBreaker::handleStart),
        PLACE_LEVER(BedrockBreaker::handlePlaceLever),
        BREAK_PISTON_START(BedrockBreaker::handleBreakPistonStart),
        BREAK_PISTON_PROGRESS(BedrockBreaker::handleBreakPistonProgress),
        WAIT_PISTON_EXTEND(BedrockBreaker::handleWaitPistonExtend),
        PLACE_REVERSE_PISTON(BedrockBreaker::handlePlaceReversePiston),
        WAIT_BEDROCK_BREAK(BedrockBreaker::handleWaitBedrockBreak),
        BREAK_REMAINING_LEVER_START(BedrockBreaker::handleBreakRemainingLeverStart),
        BREAK_REMAINING_LEVER_PROGRESS(BedrockBreaker::handleBreakRemainingLeverProgress),
        BREAK_REMAINING_PISTON_START(BedrockBreaker::handleBreakRemainingPistonStart),
        BREAK_REMAINING_PISTON_PROGRESS(BedrockBreaker::handleBreakRemainingPistonProgress);

        private final Consumer<BedrockBreaker> action;

        State(Consumer<BedrockBreaker> action) {
            this.action = action;
        }

        public void handle(BedrockBreaker instance) {
            action.accept(instance);
        }
    }
}