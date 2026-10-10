package com.fluxt.doubletapfly;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Fast block placement and fast item use.
 *
 * <p>Vanilla hard-codes a four tick cooldown in {@code Minecraft#startUseItem()}, so holding right
 * click only places (or uses) about five times per second. {@link #afterPlacement} hands the game a
 * shorter cooldown, keeps block placement and item use on separate switches, and - when
 * {@code blocks_per_tick} is raised above one - also fills the ring of blocks around the one being
 * aimed at.</p>
 *
 * <p>This is the piece that pairs with a schematic projection: the projection tells you where to
 * aim, and this makes every sweep over it place a whole row instead of a single block. It also
 * composes with Litematica's own Easy Place mode, which picks the correct block and orientation
 * once a placement is attempted.</p>
 */
public final class FastPlaceHelper {

    /** The cooldown vanilla uses when nothing here is enabled. */
    private static final int VANILLA_PLACE_DELAY = 4;

    private FastPlaceHelper() {}

    /**
     * Runs at the very end of {@code Minecraft#startUseItem()}, i.e. after one complete placement or
     * item use attempt.
     *
     * @return the cooldown to use until the next attempt
     */
    public static int afterPlacement(Minecraft minecraft, @Nullable HitResult hitResult) {
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return VANILLA_PLACE_DELAY;
        }

        boolean blocksInHand = holdsBlocks(player);
        boolean fastPlace = DoubleTapFlyConfig.fastPlaceEnabled();

        // Air place handles the case where the click landed on nothing at all.
        BlockPos airPlaced = AirPlaceHelper.tryPlace(minecraft);
        if (airPlaced != null) {
            if (blocksInHand && fastPlace) {
                spreadAround(minecraft, airPlaced, player.getDirection().getOpposite(),
                        Vec3.atCenterOf(airPlaced));
                return DoubleTapFlyConfig.fastPlaceDelay();
            }
            return VANILLA_PLACE_DELAY;
        }

        if (blocksInHand) {
            if (!fastPlace) {
                return VANILLA_PLACE_DELAY;
            }
            if (hitResult instanceof BlockHitResult blockHit) {
                spreadAround(minecraft, blockHit.getBlockPos().relative(blockHit.getDirection()),
                        blockHit.getDirection(), blockHit.getLocation());
            }
            return DoubleTapFlyConfig.fastPlaceDelay();
        }

        return DoubleTapFlyConfig.fastUseEnabled() ? DoubleTapFlyConfig.fastUseCooldown() : VANILLA_PLACE_DELAY;
    }

    /** True when the hand vanilla would use for a placement is holding a block. */
    private static boolean holdsBlocks(LocalPlayer player) {
        if (player.getMainHandItem().getItem() instanceof BlockItem) {
            return true;
        }
        return player.getMainHandItem().isEmpty() && player.getOffhandItem().getItem() instanceof BlockItem;
    }

    private static void spreadAround(Minecraft minecraft, BlockPos origin, Direction face, Vec3 aim) {
        int extra = DoubleTapFlyConfig.fastPlaceBlocksPerTick() - 1;
        if (extra <= 0) {
            return;
        }

        MultiPlayerGameMode gameMode = minecraft.gameMode;
        LocalPlayer player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || gameMode == null || level == null) {
            return;
        }
        if (player.isHandsBusy() || gameMode.isDestroying()) {
            return;
        }

        InteractionHand hand = InteractionHand.MAIN_HAND;
        if (player.getItemInHand(hand).isEmpty()) {
            return;
        }

        double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
        double reachSquared = reach * reach;
        Vec3 eye = player.getEyePosition();

        List<BlockPos> candidates = ringAround(origin, face);
        candidates.sort(Comparator.comparingDouble(pos -> Vec3.atCenterOf(pos).distanceToSqr(aim)));

        int placed = 0;
        for (BlockPos candidate : candidates) {
            if (placed >= extra) {
                break;
            }
            if (player.getItemInHand(hand).isEmpty()) {
                break;
            }
            if (!canPlaceAt(level, player, candidate, eye, reachSquared)) {
                continue;
            }
            if (tryPlace(gameMode, player, level, candidate, hand, eye, reachSquared)) {
                placed++;
            }
        }
    }

    /**
     * The eight positions around {@code origin} that lie in the same plane as the clicked face.
     * Filling sideways is what makes a wall or a floor go up quickly; filling outwards along the
     * face normal would just stack blocks into thin air.
     */
    private static List<BlockPos> ringAround(BlockPos origin, Direction face) {
        Direction.Axis axis = face.getAxis();
        List<BlockPos> result = new ArrayList<>(8);

        for (Direction first : Direction.values()) {
            if (first.getAxis() == axis) {
                continue;
            }
            result.add(origin.relative(first));
            for (Direction second : Direction.values()) {
                if (second.getAxis() == axis || second.getAxis() == first.getAxis()) {
                    continue;
                }
                result.add(origin.relative(first).relative(second));
            }
        }

        return result;
    }

    private static boolean canPlaceAt(Level level, LocalPlayer player, BlockPos pos, Vec3 eye,
            double reachSquared) {
        if (level.isOutsideBuildHeight(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (!state.canBeReplaced()) {
            return false;
        }
        if (player.getBoundingBox().intersects(new AABB(pos))) {
            return false;
        }
        return eye.distanceToSqr(Vec3.atCenterOf(pos)) <= reachSquared;
    }

    /** Places the held block at {@code pos} by clicking a solid neighbour that touches it. */
    private static boolean tryPlace(MultiPlayerGameMode gameMode, LocalPlayer player, Level level,
            BlockPos pos, InteractionHand hand, Vec3 eye, double reachSquared) {
        for (Direction support : Direction.values()) {
            BlockPos neighbour = pos.relative(support);
            BlockState neighbourState = level.getBlockState(neighbour);
            if (neighbourState.isAir() || neighbourState.canBeReplaced()) {
                continue;
            }
            if (eye.distanceToSqr(Vec3.atCenterOf(neighbour)) > reachSquared) {
                continue;
            }

            Vec3 hitVec = Vec3.atCenterOf(neighbour).add(support.getUnitVec3().scale(0.5D));
            BlockHitResult hit = new BlockHitResult(hitVec, support, neighbour, false);
            InteractionResult result = gameMode.useItemOn(player, hand, hit);
            if (result.consumesAction()) {
                return true;
            }
        }
        return false;
    }
}
