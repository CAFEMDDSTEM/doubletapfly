package com.fluxt.doubletapfly;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Config file: {@code config/doubletapfly-client.toml}
 */
public final class DoubleTapFlyConfig {

    /** Vanilla creative flying speed in blocks per tick. */
    public static final float VANILLA_FLY_SPEED = 0.05F;
    public static final double MIN_FLY_SPEED = 0.1D;
    public static final double MAX_FLY_SPEED = 10.0D;
    public static final int MIN_PLACE_DELAY = 0;
    public static final int MAX_PLACE_DELAY = 4;
    public static final int MIN_BLOCKS_PER_TICK = 1;
    public static final int MAX_BLOCKS_PER_TICK = 12;
    public static final int MIN_DOUBLE_TAP_WINDOW = 2;
    public static final int MAX_DOUBLE_TAP_WINDOW = 20;
    public static final int MIN_ANTI_KICK_INTERVAL = 10;
    public static final int MAX_ANTI_KICK_INTERVAL = 75;
    public static final int MIN_USE_COOLDOWN = 0;
    public static final int MAX_USE_COOLDOWN = 4;

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue DOUBLE_TAP_WINDOW;
    public static final ModConfigSpec.DoubleValue FLY_SPEED;
    public static final ModConfigSpec.BooleanValue FALL_DAMAGE_PROTECTION;
    public static final ModConfigSpec.BooleanValue ANTI_KICK;
    public static final ModConfigSpec.IntValue ANTI_KICK_INTERVAL;
    public static final ModConfigSpec.BooleanValue FAST_PLACE_ENABLED;
    public static final ModConfigSpec.IntValue FAST_PLACE_DELAY;
    public static final ModConfigSpec.IntValue FAST_PLACE_BLOCKS_PER_TICK;
    public static final ModConfigSpec.BooleanValue AIR_PLACE_ENABLED;
    public static final ModConfigSpec.BooleanValue AIR_PLACE_RENDER;
    public static final ModConfigSpec.BooleanValue FAST_USE_ENABLED;
    public static final ModConfigSpec.IntValue FAST_USE_COOLDOWN;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Core settings").translation("doubletapfly.config.section.general").push("general");
        ENABLED = builder
                .comment("Master switch. When false, flight granted by this mod is switched off again.")
                .translation("doubletapfly.config.flight_enabled")
                .define("enabled", true);
        DOUBLE_TAP_WINDOW = builder
                .comment("How many ticks may pass between the two space taps for them to count as a double tap.",
                        "Vanilla creative mode uses 7 ticks (0.35 s).")
                .translation("doubletapfly.config.double_tap_window")
                .defineInRange("double_tap_window", 7, MIN_DOUBLE_TAP_WINDOW, MAX_DOUBLE_TAP_WINDOW);
        FLY_SPEED = builder
                .comment("Flight speed multiplier. 1.0 is exactly the vanilla creative speed.",
                        "Can also be changed in game with: !config FlySpeed = 1.5")
                .translation("doubletapfly.config.fly_speed")
                .defineInRange("fly_speed", 1.0D, MIN_FLY_SPEED, MAX_FLY_SPEED);
        builder.pop();

        builder.comment("Fall damage").translation("doubletapfly.config.section.fall_damage").push("fall_damage");
        FALL_DAMAGE_PROTECTION = builder
                .comment("true  = flying players take no fall damage, just like creative mode.",
                        "false = the server keeps counting your descent, so landing after a flight hurts.")
                .translation("doubletapfly.config.fall_protection")
                .define("protection", true);
        builder.pop();

        builder.comment(
                "Anti-kick. Only needed on servers that still run with allow-flight=false in server.properties.",
                "Such a server kicks players that stay airborne for more than 4 seconds.",
                "Leave this off if your server already allows flight (recommended): keeping it on makes you sink a tiny bit.")
                .translation("doubletapfly.config.section.anti_kick")
                .push("anti_kick");
        ANTI_KICK = builder
                .comment("Enable the periodic tiny downward nudge that stops the server from kicking you.")
                .translation("doubletapfly.config.anti_kick")
                .define("enabled", false);
        ANTI_KICK_INTERVAL = builder
                .comment("How many ticks between nudges. Must stay below 80, otherwise the server kicks you first.")
                .translation("doubletapfly.config.anti_kick_interval")
                .defineInRange("interval", 40, MIN_ANTI_KICK_INTERVAL, MAX_ANTI_KICK_INTERVAL);
        builder.pop();

        builder.comment(
                "Fast block placement. Removes the vanilla 4 tick cooldown between placements,",
                "which is what makes filling in a schematic projection feel fast.")
                .translation("doubletapfly.config.section.fast_place")
                .push("fast_place");
        FAST_PLACE_ENABLED = builder
                .comment("Master switch for fast block placement.")
                .translation("doubletapfly.config.fast_place")
                .define("enabled", true);
        FAST_PLACE_DELAY = builder
                .comment("Ticks to wait between two placements. Vanilla uses 4; 1 means one placement per tick.")
                .translation("doubletapfly.config.place_delay")
                .defineInRange("place_delay", 1, MIN_PLACE_DELAY, MAX_PLACE_DELAY);
        FAST_PLACE_BLOCKS_PER_TICK = builder
                .comment("How many blocks may be placed per tick in total.",
                        "1 = only the block you are aiming at (safest).",
                        "Higher values also fill the ring of blocks around the one you are aiming at.")
                .translation("doubletapfly.config.blocks_per_tick")
                .defineInRange("blocks_per_tick", 1, MIN_BLOCKS_PER_TICK, MAX_BLOCKS_PER_TICK);
        builder.pop();

        builder.comment(
                "Air place. Lets you place blocks in mid air, at the far end of your reach,",
                "by aiming at nothing. The spot is highlighted in red while it is active.")
                .translation("doubletapfly.config.section.air_place")
                .push("air_place");
        AIR_PLACE_ENABLED = builder
                .comment("Master switch for air place. Off by default, switch it on with !airplace.")
                .translation("doubletapfly.config.air_place")
                .define("enabled", false);
        AIR_PLACE_RENDER = builder
                .comment("Draw a red box around the spot the block would be placed at.")
                .translation("doubletapfly.config.air_place_render")
                .define("render", true);
        builder.pop();

        builder.comment(
                "Fast use. Shortens the vanilla 4 tick cooldown that applies after you use an item,",
                "so food, potions, pearls and similar can be used again much sooner.")
                .translation("doubletapfly.config.section.fast_use")
                .push("fast_use");
        FAST_USE_ENABLED = builder
                .comment("Master switch for fast use. Only affects items, not block placement.")
                .translation("doubletapfly.config.fast_use")
                .define("enabled", true);
        FAST_USE_COOLDOWN = builder
                .comment("Cooldown in ticks after using an item. Vanilla uses 4, 0 is as fast as it gets.")
                .translation("doubletapfly.config.fast_use_cooldown")
                .defineInRange("cooldown", 0, MIN_USE_COOLDOWN, MAX_USE_COOLDOWN);
        builder.pop();

        SPEC = builder.build();
    }

    private DoubleTapFlyConfig() {}

    private static boolean readBoolean(ModConfigSpec.BooleanValue value, boolean fallback) {
        try {
            return value.getAsBoolean();
        } catch (RuntimeException ignored) {
            // Config not loaded yet (very early start-up) - fall back to the default.
            return fallback;
        }
    }

    private static int readInt(ModConfigSpec.IntValue value, int fallback) {
        try {
            return value.getAsInt();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static double readDouble(ModConfigSpec.DoubleValue value, double fallback) {
        try {
            return value.getAsDouble();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static <T> void write(ModConfigSpec.ConfigValue<T> value, T newValue) {
        try {
            value.set(newValue);
            value.save();
        } catch (RuntimeException ignored) {
            // Config not loaded yet - the toggle simply stays in memory for this session.
        }
    }

    public static boolean enabled() {
        return readBoolean(ENABLED, true);
    }

    /** Flips the master switch and returns its new state. */
    public static boolean toggleEnabled() {
        boolean newValue = !enabled();
        write(ENABLED, newValue);
        return newValue;
    }

    public static void setEnabled(boolean enabled) {
        write(ENABLED, enabled);
    }

    public static int doubleTapWindow() {
        return readInt(DOUBLE_TAP_WINDOW, 7);
    }

    public static void setDoubleTapWindow(int ticks) {
        write(DOUBLE_TAP_WINDOW, ticks);
    }

    public static double flySpeed() {
        return readDouble(FLY_SPEED, 1.0D);
    }

    public static void setFlySpeed(double speed) {
        write(FLY_SPEED, speed);
    }

    public static boolean fallDamageProtection() {
        return readBoolean(FALL_DAMAGE_PROTECTION, true);
    }

    public static void setFallDamageProtection(boolean enabled) {
        write(FALL_DAMAGE_PROTECTION, enabled);
    }

    public static boolean antiKick() {
        return readBoolean(ANTI_KICK, false);
    }

    public static void setAntiKick(boolean enabled) {
        write(ANTI_KICK, enabled);
    }

    public static int antiKickInterval() {
        return readInt(ANTI_KICK_INTERVAL, 40);
    }

    public static void setAntiKickInterval(int ticks) {
        write(ANTI_KICK_INTERVAL, ticks);
    }

    public static boolean fastPlaceEnabled() {
        return readBoolean(FAST_PLACE_ENABLED, true);
    }

    public static void setFastPlaceEnabled(boolean enabled) {
        write(FAST_PLACE_ENABLED, enabled);
    }

    /** Flips fast placement and returns its new state. */
    public static boolean toggleFastPlace() {
        boolean newValue = !fastPlaceEnabled();
        write(FAST_PLACE_ENABLED, newValue);
        return newValue;
    }

    public static int fastPlaceDelay() {
        return readInt(FAST_PLACE_DELAY, 1);
    }

    public static void setFastPlaceDelay(int delay) {
        write(FAST_PLACE_DELAY, delay);
    }

    public static int fastPlaceBlocksPerTick() {
        return readInt(FAST_PLACE_BLOCKS_PER_TICK, 1);
    }

    public static void setFastPlaceBlocksPerTick(int blocks) {
        write(FAST_PLACE_BLOCKS_PER_TICK, blocks);
    }

    public static boolean airPlaceEnabled() {
        return readBoolean(AIR_PLACE_ENABLED, false);
    }

    /** Flips air place and returns its new state. */
    public static boolean toggleAirPlace() {
        boolean newValue = !airPlaceEnabled();
        write(AIR_PLACE_ENABLED, newValue);
        return newValue;
    }

    public static void setAirPlaceEnabled(boolean enabled) {
        write(AIR_PLACE_ENABLED, enabled);
    }

    public static boolean airPlaceRender() {
        return readBoolean(AIR_PLACE_RENDER, true);
    }

    public static void setAirPlaceRender(boolean enabled) {
        write(AIR_PLACE_RENDER, enabled);
    }

    public static boolean fastUseEnabled() {
        return readBoolean(FAST_USE_ENABLED, true);
    }

    /** Flips fast use and returns its new state. */
    public static boolean toggleFastUse() {
        boolean newValue = !fastUseEnabled();
        write(FAST_USE_ENABLED, newValue);
        return newValue;
    }

    public static void setFastUseEnabled(boolean enabled) {
        write(FAST_USE_ENABLED, enabled);
    }

    public static int fastUseCooldown() {
        return readInt(FAST_USE_COOLDOWN, 0);
    }

    public static void setFastUseCooldown(int ticks) {
        write(FAST_USE_COOLDOWN, ticks);
    }
}
