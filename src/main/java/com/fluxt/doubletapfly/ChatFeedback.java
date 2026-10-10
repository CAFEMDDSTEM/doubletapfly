package com.fluxt.doubletapfly;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Small helper for the coloured lines this mod prints into the chat.
 *
 * <p>{@code addClientSystemMessage} only writes to the local chat, so nothing here is ever sent to
 * the server.</p>
 */
public final class ChatFeedback {

    private ChatFeedback() {}

    /** Echoes a command the player typed, the way a normal chat line would have looked. */
    public static void echo(String message) {
        send(Component.literal(message).withStyle(ChatFormatting.DARK_GRAY));
    }

    public static void success(Component message) {
        sendPrefixed(message, ChatFormatting.GREEN);
    }

    public static void failure(Component message) {
        sendPrefixed(message, ChatFormatting.RED);
    }

    public static void info(Component message) {
        sendPrefixed(message, ChatFormatting.YELLOW);
    }

    /** Dimmed hint line, no colour emphasis. */
    public static void tip(Component message) {
        sendPrefixed(message, ChatFormatting.GRAY);
    }

    /** Heading line for the {@code !doubletapfly help} block. */
    public static void helpHeader(Component message) {
        send(prefix().append(message.copy().withStyle(ChatFormatting.WHITE)));
    }

    /** One command table row: the syntax in yellow, the explanation in grey. */
    public static void helpLine(String syntax, Component description) {
        send(Component.literal("  " + syntax).withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY))
                .append(description.copy().withStyle(ChatFormatting.GRAY)));
    }

    /** One {@code !doubletapfly status} row, indented and grey. */
    public static void statusLine(Component message) {
        send(Component.literal("  ").append(message.copy().withStyle(ChatFormatting.GRAY)));
    }

    /** Green when the feature was switched on, red when it was switched off. */
    public static void reportToggle(boolean nowEnabled) {
        send(prefix()
                .append(Component.translatable(nowEnabled
                                ? "doubletapfly.message.enabled"
                                : "doubletapfly.message.disabled")
                        .withStyle(nowEnabled ? ChatFormatting.GREEN : ChatFormatting.RED)));
    }

    /** Same colour convention, for the fast placement switch. */
    public static void reportFastPlace(boolean nowEnabled) {
        send(prefix()
                .append(Component.translatable(nowEnabled
                                ? "doubletapfly.message.fast_place.enabled"
                                : "doubletapfly.message.fast_place.disabled")
                        .withStyle(nowEnabled ? ChatFormatting.GREEN : ChatFormatting.RED)));
    }

    /** Same colour convention, for the air place switch. */
    public static void reportAirPlace(boolean nowEnabled) {
        send(prefix()
                .append(Component.translatable(nowEnabled
                                ? "doubletapfly.message.air_place.enabled"
                                : "doubletapfly.message.air_place.disabled")
                        .withStyle(nowEnabled ? ChatFormatting.GREEN : ChatFormatting.RED)));
    }

    /** Same colour convention, for the fast use switch. */
    public static void reportFastUse(boolean nowEnabled) {
        send(prefix()
                .append(Component.translatable(nowEnabled
                                ? "doubletapfly.message.fast_use.enabled"
                                : "doubletapfly.message.fast_use.disabled")
                        .withStyle(nowEnabled ? ChatFormatting.GREEN : ChatFormatting.RED)));
    }

    private static MutableComponent prefix() {
        return Component.translatable("doubletapfly.prefix").withStyle(ChatFormatting.AQUA);
    }

    private static void sendPrefixed(Component message, ChatFormatting colour) {
        send(prefix().append(message.copy().withStyle(colour)));
    }

    private static void send(Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui != null) {
            minecraft.gui.getChat().addClientSystemMessage(message);
        }
    }
}
