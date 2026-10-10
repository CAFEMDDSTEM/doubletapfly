package com.fluxt.doubletapfly;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Every bit of the flight logic lives here.
 *
 * <p>Creative flight on the client is nothing more than
 * {@code Player#getAbilities().flying == true}: {@code Player#travel} then drops gravity and
 * {@code LocalPlayer#aiStep} turns the jump/shift keys into vertical movement. A vanilla server
 * never learns about it, because player movement is simulated by the client and merely reported
 * to the server. So all this mod has to do is flip that one flag at the right moment - and clean
 * up the two side effects the server can still notice (fall damage).</p>
 */
@EventBusSubscriber(modid = DoubleTapFlyMod.MOD_ID, value = Dist.CLIENT)
public final class ClientFlightController {

    /** How much descent we let the server accumulate before we reset it with a fake "on ground". */
    private static final double FALL_RESET_THRESHOLD = 1.0D;
    /** The nudge used by the optional anti-kick. Must be bigger than 0.03125 to count as "falling". */
    private static final double ANTI_KICK_NUDGE = 0.05D;

    /** True while the player is flying because of this mod. */
    private static boolean wantsFlight;
    /** Previous state of the raw space key, used to find key-down edges. */
    private static boolean jumpWasDown;
    /** Ticks since the first tap of a potential double tap, or -1 when there is none. */
    private static int ticksSinceFirstTap = -1;
    /** Set for one tick when the next movement packet must pretend the player is on the ground. */
    private static boolean forceOnGround;
    /** Our estimate of the fall distance the server has accumulated since we last reset it. */
    private static double estimatedFallDistance;
    /** True while we have overwritten the vanilla flying speed with the configured multiplier. */
    private static boolean flySpeedApplied;
    /** Optional anti-kick state. */
    private static int antiKickTimer;
    private static int antiKickPhase;

    private ClientFlightController() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();

        // The rebindable key from Options -> Controls. Key presses only reach key mappings while no
        // screen is open, so this never fires while the player is typing.
        while (KeyBindings.TOGGLE.consumeClick()) {
            ChatFeedback.reportToggle(DoubleTapFlyConfig.toggleEnabled());
        }
        while (KeyBindings.FAST_PLACE.consumeClick()) {
            ChatFeedback.reportFastPlace(DoubleTapFlyConfig.toggleFastPlace());
        }
        while (KeyBindings.AIR_PLACE.consumeClick()) {
            ChatFeedback.reportAirPlace(DoubleTapFlyConfig.toggleAirPlace());
        }
        while (KeyBindings.FAST_USE.consumeClick()) {
            ChatFeedback.reportFastUse(DoubleTapFlyConfig.toggleFastUse());
        }
        ChatCommands.openPendingScreen();
        AirPlaceHelper.tick(minecraft);

        LocalPlayer player = minecraft.player;

        // Only ever set for the current tick.
        forceOnGround = false;

        if (player == null) {
            clearState();
            return;
        }

        // Creative and spectator already fly for real; never touch them.
        if (!DoubleTapFlyConfig.enabled() || player.isCreative() || player.isSpectator()) {
            stopModFlight(player);
            clearTransientState();
            return;
        }

        if (ticksSinceFirstTap >= 0) {
            ticksSinceFirstTap++;
            if (ticksSinceFirstTap > DoubleTapFlyConfig.doubleTapWindow()) {
                ticksSinceFirstTap = -1;
            }
        }

        boolean jumpDown = minecraft.options.keyJump.isDown();
        boolean doubleTap = false;
        if (jumpDown && !jumpWasDown) {
            if (ticksSinceFirstTap >= 0) {
                doubleTap = true;
                ticksSinceFirstTap = -1;
            } else {
                ticksSinceFirstTap = 0;
            }
        }
        jumpWasDown = jumpDown;

        if (doubleTap && mayToggleFlight(player)) {
            wantsFlight = !wantsFlight;
            if (wantsFlight) {
                player.getAbilities().flying = true;
                estimatedFallDistance = 0.0D;
            }
        }

        if (wantsFlight) {
            if (!player.getAbilities().flying) {
                if (player.onGround()) {
                    // Vanilla landed us and switched flight off by itself - stay landed.
                    wantsFlight = false;
                    estimatedFallDistance = 0.0D;
                } else {
                    // Something else cleared the flag (a server abilities packet, a dimension
                    // change, ...). Re-assert it, the player never asked to stop.
                    player.getAbilities().flying = true;
                }
            }
        } else if (player.getAbilities().flying) {
            player.getAbilities().flying = false;
        }

        if (wantsFlight) {
            applyFlySpeed(player);
            applyFallDamageProtection(player);
            applyAntiKick(player);
        } else {
            restoreFlySpeed(player);
            estimatedFallDistance = 0.0D;
            antiKickTimer = 0;
            antiKickPhase = 0;
        }
    }

    /** Read by {@code LocalPlayerMixin}; true only while we are flying and want to shield a landing. */
    public static boolean shouldForceOnGround() {
        return forceOnGround;
    }

    /**
     * Mirrors the two conditions vanilla puts on the creative double tap: you cannot start or stop
     * flying while swimming, and not while riding something you cannot "jump" with.
     */
    private static boolean mayToggleFlight(LocalPlayer player) {
        return !player.isSwimming() && player.getVehicle() == null;
    }

    /**
     * Overwrites the flying speed with {@code 0.05 * fly_speed}. It has to be re-applied every tick,
     * because {@code ClientboundPlayerAbilitiesPacket} carries the server's own value and would
     * silently undo it whenever the server syncs the player's abilities.
     */
    private static void applyFlySpeed(LocalPlayer player) {
        player.getAbilities()
                .setFlyingSpeed((float) (DoubleTapFlyConfig.VANILLA_FLY_SPEED * DoubleTapFlyConfig.flySpeed()));
        flySpeedApplied = true;
    }

    private static void restoreFlySpeed(LocalPlayer player) {
        if (flySpeedApplied) {
            player.getAbilities().setFlyingSpeed(DoubleTapFlyConfig.VANILLA_FLY_SPEED);
            flySpeedApplied = false;
        }
    }

    /**
     * The server accumulates fall distance from the positions we report and only resets it when we
     * move up. Flying straight down would therefore end in a lethal landing.
     *
     * <p>So we let it accumulate a little, then report a single "on ground" tick. The server then
     * applies the usual landing logic with a fall distance below the safe-fall threshold, deals no
     * damage and resets its counter - while the player keeps hovering.</p>
     */
    private static void applyFallDamageProtection(LocalPlayer player) {
        if (!DoubleTapFlyConfig.fallDamageProtection()) {
            estimatedFallDistance = 0.0D;
            return;
        }

        double movedY = player.getY() - player.yo;
        if (movedY > 0.0D) {
            // The server resets its counter whenever we move up.
            estimatedFallDistance = 0.0D;
        } else if (movedY < 0.0D) {
            estimatedFallDistance += -movedY;
        }

        if (estimatedFallDistance >= FALL_RESET_THRESHOLD) {
            forceOnGround = true;
            estimatedFallDistance = 0.0D;
        }
    }

    /**
     * Optional: on servers that still have {@code allow-flight=false}, staying airborne for more
     * than 80 ticks gets you kicked. Moving down by more than 0.03125 in a single tick resets that
     * counter, so we nudge down once in a while and lift back up on the next tick.
     */
    private static void applyAntiKick(LocalPlayer player) {
        if (!DoubleTapFlyConfig.antiKick()) {
            antiKickTimer = 0;
            antiKickPhase = 0;
            return;
        }

        if (antiKickPhase == 0 && ++antiKickTimer >= DoubleTapFlyConfig.antiKickInterval()) {
            antiKickTimer = 0;
            antiKickPhase = 2;
        }

        if (antiKickPhase == 2) {
            Vec3 movement = player.getDeltaMovement();
            player.setDeltaMovement(movement.x, -ANTI_KICK_NUDGE, movement.z);
            antiKickPhase = 1;
        } else if (antiKickPhase == 1) {
            Vec3 movement = player.getDeltaMovement();
            player.setDeltaMovement(movement.x, ANTI_KICK_NUDGE, movement.z);
            antiKickPhase = 0;
        }
    }

    private static void stopModFlight(LocalPlayer player) {
        if (wantsFlight && player.getAbilities().flying) {
            player.getAbilities().flying = false;
        }
        restoreFlySpeed(player);
        wantsFlight = false;
    }

    private static void clearState() {
        wantsFlight = false;
        flySpeedApplied = false;
        clearTransientState();
    }

    private static void clearTransientState() {
        jumpWasDown = false;
        ticksSinceFirstTap = -1;
        estimatedFallDistance = 0.0D;
        antiKickTimer = 0;
        antiKickPhase = 0;
    }
}
