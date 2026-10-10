package com.fluxt.doubletapfly;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ArmorStandItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Air place: put a block into mid air, at the far end of your reach, by aiming at nothing.
 *
 * <p>The trick is in how vanilla resolves a placement. {@code BlockPlaceContext} looks at the block
 * being clicked and asks whether it can be replaced - and air always can. When it can, the block
 * goes into the clicked position itself instead of the neighbouring one. So clicking the empty
 * position we want the block in is entirely enough; no fake support block is needed, and the server
 * accepts it the same way it accepts any other placement.</p>
 */
public final class AirPlaceHelper {

    private static @Nullable BlockPos target;

    private AirPlaceHelper() {}

    /** The position the next placement would go to, or null when air place is not applicable. */
    public static @Nullable BlockPos target() {
        return target;
    }

    /** Recomputes the target. Called once per client tick, before the world is rendered. */
    public static void tick(Minecraft minecraft) {
        target = null;

        if (!DoubleTapFlyConfig.airPlaceEnabled() || minecraft.screen != null) {
            return;
        }

        LocalPlayer player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || level == null) {
            return;
        }

        // Vanilla reports a MISS hit at the far end of the reach ray, with the block position the
        // ray ended in - exactly the cell we want to fill.
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.MISS) {
            return;
        }
        if (!isPlaceable(player.getMainHandItem()) && !isPlaceable(player.getOffhandItem())) {
            return;
        }

        BlockPos pos = hit.getBlockPos();
        if (!level.getBlockState(pos).canBeReplaced()) {
            return;
        }
        target = pos.immutable();
    }

    /**
     * Performs the placement. Called at the end of {@code Minecraft#startUseItem()}, so it obeys the
     * same key and cooldown rules as a normal right click.
     *
     * @return the position the block was placed at, or null when nothing was placed
     */
    public static @Nullable BlockPos tryPlace(Minecraft minecraft) {
        BlockPos pos = target;
        if (pos == null) {
            return null;
        }

        LocalPlayer player = minecraft.player;
        MultiPlayerGameMode gameMode = minecraft.gameMode;
        if (player == null || gameMode == null || player.isHandsBusy()) {
            return null;
        }

        InteractionHand hand = InteractionHand.MAIN_HAND;
        if (!isPlaceable(player.getItemInHand(hand))) {
            hand = InteractionHand.OFF_HAND;
            if (!isPlaceable(player.getItemInHand(hand))) {
                return null;
            }
        }

        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), player.getDirection().getOpposite(),
                pos, false);
        InteractionResult result = gameMode.useItemOn(player, hand, hit);
        return result.consumesAction() ? pos : null;
    }

    /** The item kinds that can be placed against a block, mirroring what a right click accepts. */
    public static boolean isPlaceable(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        var item = stack.getItem();
        return item instanceof BlockItem
                || item instanceof SpawnEggItem
                || item instanceof ArmorStandItem
                || item instanceof FireworkRocketItem;
    }
}
