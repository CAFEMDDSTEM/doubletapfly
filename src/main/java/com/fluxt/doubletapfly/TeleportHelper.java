package com.fluxt.doubletapfly;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Teleport, driven from the chat: {@code !tp}, {@code !tp <player>} and {@code !tp x y z}.
 *
 * <p>The move itself is just a local player position change; the client reports the new position to
 * the server on the next tick like any other movement.</p>
 *
 * <p><b>What the server does with that</b>: {@code ServerGamePacketListenerImpl#handleMovePlayer}
 * measures the squared distance between the position a move packet reports and the position at the
 * start of the tick, subtracts the expected squared velocity and compares the result against
 * {@code metersPerTick * deltaPackets}, where {@code metersPerTick} is {@code 100.0F} for a
 * normal player. Because both sides of that comparison are squared distances, the real allowance is
 * {@code sqrt(100) = 10} blocks per packet. Anything past that is logged as
 * "moved too quickly" and the player is snapped straight back. {@code allow-flight} has nothing to
 * do with this check - it only disables the "flying is not enabled" kick.</p>
 *
 * <p>So short hops land, and long jumps get undone by the server unless it is running something
 * that turns that check off. The chat feedback says so whenever a jump looks too long.</p>
 */
public final class TeleportHelper {

    private TeleportHelper() {}

    /** Parses and runs the command arguments (everything after {@code !tp}). */
    public static void execute(String arguments) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            ChatFeedback.failure(Component.translatable("doubletapfly.tp.no_world"));
            return;
        }

        String trimmed = arguments.trim();
        if (trimmed.isEmpty()) {
            teleport(player, DoubleTapFlyConfig.teleportHomeX(), DoubleTapFlyConfig.teleportHomeY(),
                    DoubleTapFlyConfig.teleportHomeZ(),
                    Component.translatable("doubletapfly.tp.label_home"));
            return;
        }

        String[] parts = trimmed.split("\\s+");
        if (parts.length == 1) {
            teleportToPlayer(player, level, parts[0]);
            return;
        }

        if (parts.length == 3) {
            Double x = parse(parts[0]);
            Double y = parse(parts[1]);
            Double z = parse(parts[2]);
            if (x == null || y == null || z == null) {
                ChatFeedback.failure(Component.translatable("doubletapfly.tp.usage"));
                return;
            }
            teleport(player, x, y, z,
                    Component.literal(format(x) + " " + format(y) + " " + format(z)));
            return;
        }

        ChatFeedback.failure(Component.translatable("doubletapfly.tp.usage"));
    }

    private static void teleportToPlayer(LocalPlayer self, ClientLevel level, String name) {
        for (AbstractClientPlayer other : level.players()) {
            String otherName = other.getGameProfile().name();
            if (!otherName.equalsIgnoreCase(name)) {
                continue;
            }
            teleport(self, other.getX(), other.getY(), other.getZ(), Component.literal(otherName));
            return;
        }

        ChatFeedback.failure(Component.translatable("doubletapfly.tp.player_unknown", name));
    }

    private static void teleport(LocalPlayer player, double x, double y, double z, Component label) {
        double distance = Math.sqrt(player.distanceToSqr(x, y, z));

        player.setPos(x, y, z);
        // Keep the interpolation origin in step, otherwise the camera slides across the whole map
        // for one frame.
        player.xo = x;
        player.yo = y;
        player.zo = z;
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();

        ChatFeedback.success(Component.translatable("doubletapfly.tp.done", label, format(distance)));

        double warn = DoubleTapFlyConfig.teleportWarnDistance();
        if (warn > 0.0D && distance > warn) {
            ChatFeedback.info(Component.translatable("doubletapfly.tp.remote_warning", format(warn)));
        }
    }

    private static @Nullable Double parse(String raw) {
        try {
            return Double.valueOf(raw);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String format(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(Math.round(value * 10.0D) / 10.0D);
    }
}
