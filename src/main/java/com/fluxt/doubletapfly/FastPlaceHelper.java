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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Fast block placement.
 *
 * <p>Vanilla hard-codes a four tick cooldown in {@code Minecraft#startUseItem()}, so holding right
 * click only places about five blocks per second. {@link #afterPlacement} hands the game a shorter
 * cooldown, and - when {@code blocks_per_tick} is raised above one - also fills the blocks around
 * the one being aimed at.</p>
 *
 * <p>This is the piece that pairs with a schematic projection: the projection itself is what tells
 * you where to aim, and this makes every sweep over it place a whole row instead of a single block.
 * It also composes with Litematica's own Easy Place mode, which is what picks the correct block and
 * orientation once a placement is attempted.</p>
 */
public final class FastPlaceHelper {

    /** The cooldown vanilla uses when fast placement is off. */
    private static final int VANILLA_PLACE_DELAY = 4;

    private FastPlaceHelper() {}

    /**
     * Runs at the very end of {@code Minecraft#startUseItem()}, i.e. after one complete placement
     * attempt.
     *
     * @return the cooldown to use until the next placement
     */
    public static int afterPlacement(Minecraft minecraft, @Nullable HitResult hitResult) {
        if (!DoubleTapFlyConfig.fastPlaceEnabled()) {
            return VANILLA_PLACE_DELAY;
        }

        placeExtraBlocks(minecraft, hitResult);
        return DoubleTapFlyConfig.fastPlaceDelay();
    }

    private static void placeExtraBlocks(Minecraft minecraft, @Nullable HitResult hitResult) {
        int extra = DoubleTapFlyConfig.fastPlaceBlocksPerTick() - 1;
        if (extra <= 0 || !(hitResult instanceof BlockHitResult blockHit)) {
            return;
        }

        LocalPlayer player = minecraft.player;
        MultiPlayerGameMode gameMode = minecraft.gameMode;
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
        Vec3 aim = blockHit.getLocation();

        List<BlockPos> candidates = ringAround(blockHit.getBlockPos().relative(blockHit.getDirection()),
                blockHit.getDirection());
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
