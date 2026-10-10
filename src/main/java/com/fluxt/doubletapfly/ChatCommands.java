package com.fluxt.doubletapfly;

import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.event.ClientChatEvent;
import org.jspecify.annotations.Nullable;

/**
 * Every setting of this mod is driven from the chat - there is deliberately no settings screen.
 *
 * <p>{@link ClientChatEvent} fires just before a message would be sent to the server. Cancelling it
 * keeps the command off the network completely, so the server never sees it, which is what makes
 * this work on a server that has no mods of its own.</p>
 *
 * <ul>
 *   <li>{@code !doubletapfly} - switch flight on/off</li>
 *   <li>{@code !doubletapfly help} - print the command list</li>
 *   <li>{@code !doubletapfly status} - print the current settings</li>
 *   <li>{@code !doubletapfly gui} - open NeoForge's own config screen</li>
 *   <li>{@code !fastplace} - switch fast block placement on/off</li>
 *   <li>{@code !airplace} - switch air place on/off</li>
 *   <li>{@code !fastuse} - switch fast item use on/off</li>
 *   <li>{@code !config <option> = <value>} - change any setting</li>
 * </ul>
 */
@EventBusSubscriber(modid = DoubleTapFlyMod.MOD_ID, value = Dist.CLIENT)
public final class ChatCommands {

    private static final String ROOT_COMMAND = "doubletapfly";
    private static final String FAST_PLACE_COMMAND = "fastplace";
    private static final String AIR_PLACE_COMMAND = "airplace";
    private static final String FAST_USE_COMMAND = "fastuse";
    private static final String CONFIG_COMMAND = "config";
    private static final String HELP_ARGUMENT = "help";
    private static final String STATUS_ARGUMENT = "status";
    private static final String GUI_ARGUMENT = "gui";

    /** Set by {@code !doubletapfly gui}; flushed on the next client tick, see {@link #openPendingScreen()}. */
    private static boolean screenOpenRequested;

    private ChatCommands() {}

    @SubscribeEvent
    public static void onClientChat(ClientChatEvent event) {
        String message = event.getOriginalMessage().trim();
        if (message.isEmpty() || message.charAt(0) != '!') {
            return;
        }

        String body = message.substring(1).trim();

        if (isCommand(body, ROOT_COMMAND)) {
            String argument = body.substring(ROOT_COMMAND.length()).trim();
            event.setCanceled(true);
            ChatFeedback.echo(message);
            if (argument.isEmpty()) {
                ChatFeedback.reportToggle(DoubleTapFlyConfig.toggleEnabled());
            } else if (argument.equalsIgnoreCase(HELP_ARGUMENT) || argument.equals("?")) {
                sendHelp();
            } else if (argument.equalsIgnoreCase(STATUS_ARGUMENT)) {
                sendStatus();
            } else if (argument.equalsIgnoreCase(GUI_ARGUMENT)) {
                screenOpenRequested = true;
            } else {
                unknownArgument(argument);
            }
            return;
        }

        if (isCommand(body, FAST_PLACE_COMMAND)) {
            String argument = body.substring(FAST_PLACE_COMMAND.length()).trim();
            event.setCanceled(true);
            ChatFeedback.echo(message);
            if (argument.isEmpty()) {
                ChatFeedback.reportFastPlace(DoubleTapFlyConfig.toggleFastPlace());
            } else {
                unknownArgument(argument);
            }
            return;
        }

        if (isCommand(body, AIR_PLACE_COMMAND)) {
            String argument = body.substring(AIR_PLACE_COMMAND.length()).trim();
            event.setCanceled(true);
            ChatFeedback.echo(message);
            if (argument.isEmpty()) {
                ChatFeedback.reportAirPlace(DoubleTapFlyConfig.toggleAirPlace());
            } else {
                unknownArgument(argument);
            }
            return;
        }

        if (isCommand(body, FAST_USE_COMMAND)) {
            String argument = body.substring(FAST_USE_COMMAND.length()).trim();
            event.setCanceled(true);
            ChatFeedback.echo(message);
            if (argument.isEmpty()) {
                ChatFeedback.reportFastUse(DoubleTapFlyConfig.toggleFastUse());
            } else {
                unknownArgument(argument);
            }
            return;
        }

        if (isCommand(body, CONFIG_COMMAND)) {
            event.setCanceled(true);
            ChatFeedback.echo(message);
            configure(body.substring(CONFIG_COMMAND.length()).trim());
        }
    }

    /** True when {@code body} starts with {@code command} used as a whole word. */
    private static boolean isCommand(String body, String command) {
        return body.length() >= command.length()
                && body.regionMatches(true, 0, command, 0, command.length())
                && (body.length() == command.length() || Character.isWhitespace(body.charAt(command.length())));
    }

    /**
     * Opens the config screen that {@code !doubletapfly gui} asked for.
     *
     * <p>The request cannot be handled straight from the chat event: the chat screen still runs its
     * own "close me" step right after the message is handled, and it would close our screen again.
     * Waiting for the next client tick puts us after that.</p>
     */
    public static void openPendingScreen() {
        if (!screenOpenRequested) {
            return;
        }
        screenOpenRequested = false;

        Minecraft minecraft = Minecraft.getInstance();
        ModContainer container = DoubleTapFlyMod.container();
        if (container == null) {
            ChatFeedback.failure(Component.translatable("doubletapfly.message.gui.unavailable"));
            return;
        }
        minecraft.setScreen(new ConfigurationScreen(container, minecraft.screen));
    }

    // ------------------------------------------------------------------ config

    private static void configure(String arguments) {
        int separator = arguments.indexOf('=');
        if (separator < 0) {
            ChatFeedback.failure(Component.translatable("doubletapfly.message.usage.config"));
            ChatFeedback.tip(Component.translatable("doubletapfly.help.usage"));
            return;
        }

        String option = arguments.substring(0, separator).trim();
        String value = arguments.substring(separator + 1).trim();
        while (value.endsWith(";")) {
            value = value.substring(0, value.length() - 1).trim();
        }

        switch (option.toLowerCase(Locale.ROOT)) {
            case "flight" -> setFlight(value);
            case "flyspeed" -> setFlySpeed(value);
            case "doubletapwindow" -> setDoubleTapWindow(value);
            case "fallprotection" -> setFallProtection(value);
            case "fastplace" -> setFastPlace(value);
            case "placedelay" -> setPlaceDelay(value);
            case "placepertick" -> setPlacePerTick(value);
            case "antikick" -> setAntiKick(value);
            case "antikickinterval" -> setAntiKickInterval(value);
            case "airplace" -> setAirPlace(value);
            case "airplacerender" -> setAirPlaceRender(value);
            case "fastuse" -> setFastUse(value);
            case "fastusecooldown" -> setFastUseCooldown(value);
            default -> ChatFeedback.failure(
                    Component.translatable("doubletapfly.message.option.unknown", option));
        }
    }

    private static void setFlight(String rawValue) {
        Boolean value = parseBoolean(rawValue);
        if (value == null) {
            invalidBoolean(rawValue);
            return;
        }
        DoubleTapFlyConfig.setEnabled(value);
        ChatFeedback.reportToggle(value);
    }

    private static void setFlySpeed(String rawValue) {
        Double value = parseDouble(rawValue);
        if (value == null) {
            ChatFeedback.failure(Component.translatable("doubletapfly.message.fly_speed.invalid", rawValue));
            return;
        }

        double clamped = clamp(value, DoubleTapFlyConfig.MIN_FLY_SPEED, DoubleTapFlyConfig.MAX_FLY_SPEED);
        DoubleTapFlyConfig.setFlySpeed(clamped);
        ChatFeedback.success(Component.translatable("doubletapfly.message.fly_speed", format(clamped)));
        if (clamped != value) {
            clampedNotice(DoubleTapFlyConfig.MIN_FLY_SPEED, DoubleTapFlyConfig.MAX_FLY_SPEED);
        }
    }

    private static void setDoubleTapWindow(String rawValue) {
        Integer value = parseInt(rawValue);
        if (value == null) {
            invalidNumber(rawValue);
            return;
        }

        int clamped = clamp(value, DoubleTapFlyConfig.MIN_DOUBLE_TAP_WINDOW,
                DoubleTapFlyConfig.MAX_DOUBLE_TAP_WINDOW);
        DoubleTapFlyConfig.setDoubleTapWindow(clamped);
        ChatFeedback.success(Component.translatable("doubletapfly.message.double_tap_window", clamped));
        if (clamped != value) {
            clampedNotice(DoubleTapFlyConfig.MIN_DOUBLE_TAP_WINDOW, DoubleTapFlyConfig.MAX_DOUBLE_TAP_WINDOW);
        }
    }

    private static void setFallProtection(String rawValue) {
        Boolean value = parseBoolean(rawValue);
        if (value == null) {
            invalidBoolean(rawValue);
            return;
        }
        DoubleTapFlyConfig.setFallDamageProtection(value);
        ChatFeedback.success(Component.translatable("doubletapfly.message.fall_protection", onOff(value)));
    }

    private static void setFastPlace(String rawValue) {
        Boolean value = parseBoolean(rawValue);
        if (value == null) {
            invalidBoolean(rawValue);
            return;
        }
        DoubleTapFlyConfig.setFastPlaceEnabled(value);
        ChatFeedback.reportFastPlace(value);
    }

    private static void setPlaceDelay(String rawValue) {
        Integer value = parseInt(rawValue);
        if (value == null) {
            invalidNumber(rawValue);
            return;
        }

        int clamped = clamp(value, DoubleTapFlyConfig.MIN_PLACE_DELAY, DoubleTapFlyConfig.MAX_PLACE_DELAY);
        DoubleTapFlyConfig.setFastPlaceDelay(clamped);
        ChatFeedback.success(Component.translatable("doubletapfly.message.place_delay", clamped));
        if (clamped != value) {
            clampedNotice(DoubleTapFlyConfig.MIN_PLACE_DELAY, DoubleTapFlyConfig.MAX_PLACE_DELAY);
        }
    }

    private static void setPlacePerTick(String rawValue) {
        Integer value = parseInt(rawValue);
        if (value == null) {
            invalidNumber(rawValue);
            return;
        }

        int clamped = clamp(value, DoubleTapFlyConfig.MIN_BLOCKS_PER_TICK,
                DoubleTapFlyConfig.MAX_BLOCKS_PER_TICK);
        DoubleTapFlyConfig.setFastPlaceBlocksPerTick(clamped);
        ChatFeedback.success(Component.translatable("doubletapfly.message.place_per_tick", clamped));
        if (clamped != value) {
            clampedNotice(DoubleTapFlyConfig.MIN_BLOCKS_PER_TICK, DoubleTapFlyConfig.MAX_BLOCKS_PER_TICK);
        }
    }

    private static void setAntiKick(String rawValue) {
        Boolean value = parseBoolean(rawValue);
        if (value == null) {
            invalidBoolean(rawValue);
            return;
        }
        DoubleTapFlyConfig.setAntiKick(value);
        ChatFeedback.success(Component.translatable("doubletapfly.message.anti_kick", onOff(value)));
    }

    private static void setAntiKickInterval(String rawValue) {
        Integer value = parseInt(rawValue);
        if (value == null) {
            invalidNumber(rawValue);
            return;
        }

        int clamped = clamp(value, DoubleTapFlyConfig.MIN_ANTI_KICK_INTERVAL,
                DoubleTapFlyConfig.MAX_ANTI_KICK_INTERVAL);
        DoubleTapFlyConfig.setAntiKickInterval(clamped);
        ChatFeedback.success(Component.translatable("doubletapfly.message.anti_kick_interval", clamped));
        if (clamped != value) {
            clampedNotice(DoubleTapFlyConfig.MIN_ANTI_KICK_INTERVAL,
                    DoubleTapFlyConfig.MAX_ANTI_KICK_INTERVAL);
        }
    }

    private static void setAirPlace(String rawValue) {
        Boolean value = parseBoolean(rawValue);
        if (value == null) {
            invalidBoolean(rawValue);
            return;
        }
        DoubleTapFlyConfig.setAirPlaceEnabled(value);
        ChatFeedback.reportAirPlace(value);
    }

    private static void setAirPlaceRender(String rawValue) {
        Boolean value = parseBoolean(rawValue);
        if (value == null) {
            invalidBoolean(rawValue);
            return;
        }
        DoubleTapFlyConfig.setAirPlaceRender(value);
        ChatFeedback.success(Component.translatable("doubletapfly.message.air_place_render", onOff(value)));
    }

    private static void setFastUse(String rawValue) {
        Boolean value = parseBoolean(rawValue);
        if (value == null) {
            invalidBoolean(rawValue);
            return;
        }
        DoubleTapFlyConfig.setFastUseEnabled(value);
        ChatFeedback.reportFastUse(value);
    }

    private static void setFastUseCooldown(String rawValue) {
        Integer value = parseInt(rawValue);
        if (value == null) {
            invalidNumber(rawValue);
            return;
        }

        int clamped = clamp(value, DoubleTapFlyConfig.MIN_USE_COOLDOWN, DoubleTapFlyConfig.MAX_USE_COOLDOWN);
        DoubleTapFlyConfig.setFastUseCooldown(clamped);
        ChatFeedback.success(Component.translatable("doubletapfly.message.fast_use_cooldown", clamped));
        if (clamped != value) {
            clampedNotice(DoubleTapFlyConfig.MIN_USE_COOLDOWN, DoubleTapFlyConfig.MAX_USE_COOLDOWN);
        }
    }

    // -------------------------------------------------------------------- help

    private static void sendHelp() {
        ChatFeedback.helpHeader(Component.translatable("doubletapfly.help.header"));
        ChatFeedback.helpLine("!doubletapfly", Component.translatable("doubletapfly.help.root"));
        ChatFeedback.helpLine("!doubletapfly help", Component.translatable("doubletapfly.help.help"));
        ChatFeedback.helpLine("!doubletapfly status", Component.translatable("doubletapfly.help.status"));
        ChatFeedback.helpLine("!doubletapfly gui", Component.translatable("doubletapfly.help.gui"));
        ChatFeedback.helpLine("!fastplace", Component.translatable("doubletapfly.help.fast_place"));
        ChatFeedback.helpLine("!airplace", Component.translatable("doubletapfly.help.air_place"));
        ChatFeedback.helpLine("!fastuse", Component.translatable("doubletapfly.help.fast_use"));
        ChatFeedback.helpLine("!config <option> = <value>",
                Component.translatable("doubletapfly.help.config_intro"));
        ChatFeedback.helpLine("  FlySpeed = 1", Component.translatable("doubletapfly.help.fly_speed"));
        ChatFeedback.helpLine("  DoubleTapWindow = 7",
                Component.translatable("doubletapfly.help.double_tap_window"));
        ChatFeedback.helpLine("  FallProtection = true",
                Component.translatable("doubletapfly.help.fall_protection"));
        ChatFeedback.helpLine("  FastPlace = true", Component.translatable("doubletapfly.help.fast_place_config"));
        ChatFeedback.helpLine("  PlaceDelay = 1", Component.translatable("doubletapfly.help.place_delay"));
        ChatFeedback.helpLine("  PlacePerTick = 4", Component.translatable("doubletapfly.help.place_per_tick"));
        ChatFeedback.helpLine("  AntiKick = true", Component.translatable("doubletapfly.help.anti_kick"));
        ChatFeedback.helpLine("  AntiKickInterval = 40",
                Component.translatable("doubletapfly.help.anti_kick_interval"));
        ChatFeedback.helpLine("  AirPlace = true", Component.translatable("doubletapfly.help.air_place_config"));
        ChatFeedback.helpLine("  AirPlaceRender = true",
                Component.translatable("doubletapfly.help.air_place_render"));
        ChatFeedback.helpLine("  FastUse = true", Component.translatable("doubletapfly.help.fast_use_config"));
        ChatFeedback.helpLine("  FastUseCooldown = 0",
                Component.translatable("doubletapfly.help.fast_use_cooldown"));
        ChatFeedback.helpLine("  Flight = true", Component.translatable("doubletapfly.help.flight"));
        ChatFeedback.tip(Component.translatable("doubletapfly.help.tip"));
    }

    private static void sendStatus() {
        ChatFeedback.helpHeader(Component.translatable("doubletapfly.status.header"));
        ChatFeedback.statusLine(Component.translatable("doubletapfly.status.flight",
                onOff(DoubleTapFlyConfig.enabled())));
        ChatFeedback.statusLine(Component.translatable("doubletapfly.status.double_tap_window",
                DoubleTapFlyConfig.doubleTapWindow()));
        ChatFeedback.statusLine(Component.translatable("doubletapfly.status.fly_speed",
                format(DoubleTapFlyConfig.flySpeed())));
        ChatFeedback.statusLine(Component.translatable("doubletapfly.status.fall_protection",
                onOff(DoubleTapFlyConfig.fallDamageProtection())));
        ChatFeedback.statusLine(Component.translatable("doubletapfly.status.fast_place",
                onOff(DoubleTapFlyConfig.fastPlaceEnabled())));
        ChatFeedback.statusLine(Component.translatable("doubletapfly.status.place_delay",
                DoubleTapFlyConfig.fastPlaceDelay()));
        ChatFeedback.statusLine(Component.translatable("doubletapfly.status.place_per_tick",
                DoubleTapFlyConfig.fastPlaceBlocksPerTick()));
        ChatFeedback.statusLine(Component.translatable("doubletapfly.status.anti_kick",
                onOff(DoubleTapFlyConfig.antiKick()), DoubleTapFlyConfig.antiKickInterval()));
        ChatFeedback.statusLine(Component.translatable("doubletapfly.status.air_place",
                onOff(DoubleTapFlyConfig.airPlaceEnabled()),
                onOff(DoubleTapFlyConfig.airPlaceRender())));
        ChatFeedback.statusLine(Component.translatable("doubletapfly.status.fast_use",
                onOff(DoubleTapFlyConfig.fastUseEnabled()), DoubleTapFlyConfig.fastUseCooldown()));
    }

    private static void unknownArgument(String argument) {
        ChatFeedback.failure(Component.translatable("doubletapfly.help.unknown_argument", argument));
        ChatFeedback.tip(Component.translatable("doubletapfly.help.usage"));
    }

    // --------------------------------------------------------------- utilities

    /** A green ON / red OFF component, so the status block reads at a glance. */
    private static Component onOff(boolean state) {
        return Component.translatable(state ? "doubletapfly.value.on" : "doubletapfly.value.off")
                .withStyle(state ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private static void invalidBoolean(String rawValue) {
        ChatFeedback.failure(Component.translatable("doubletapfly.message.option.invalid_boolean", rawValue));
    }

    private static void invalidNumber(String rawValue) {
        ChatFeedback.failure(Component.translatable("doubletapfly.message.option.invalid_number", rawValue));
    }

    private static void clampedNotice(double min, double max) {
        ChatFeedback.info(Component.translatable("doubletapfly.message.value.clamped",
                format(min), format(max)));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static @Nullable Boolean parseBoolean(String rawValue) {
        if (rawValue.equalsIgnoreCase("true") || rawValue.equalsIgnoreCase("on")) {
            return Boolean.TRUE;
        }
        if (rawValue.equalsIgnoreCase("false") || rawValue.equalsIgnoreCase("off")) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static @Nullable Integer parseInt(String rawValue) {
        try {
            return Integer.valueOf(rawValue);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static @Nullable Double parseDouble(String rawValue) {
        try {
            return Double.valueOf(rawValue);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String format(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(Math.round(value * 100.0D) / 100.0D);
    }
}
