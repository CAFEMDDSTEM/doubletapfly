package com.fluxt.doubletapfly;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

/**
 * Prints where the other players are, and how far away they are.
 *
 * <p>Positions come from the player entities the server has sent us, so this can only ever show
 * players in your own dimension that are inside the server's player tracking range - everyone else
 * simply does not exist on this client.</p>
 *
 * <p>Both the name and the coordinates are clickable: clicking copies them to the clipboard, which
 * makes it easy to paste a position into a command or a message.</p>
 */
public final class PlayerListHelper {

    private PlayerListHelper() {}

    /** Builds and prints the list. Used by both {@code !players} and the key binding. */
    public static void sendPlayerList() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer self = minecraft.player;
        ClientLevel level = minecraft.level;
        if (self == null || level == null) {
            ChatFeedback.failure(Component.translatable("doubletapfly.players.no_world"));
            return;
        }

        double range = DoubleTapFlyConfig.playerListRange();
        boolean includeSelf = DoubleTapFlyConfig.playerListIncludeSelf();

        List<AbstractClientPlayer> listed = new ArrayList<>();
        for (AbstractClientPlayer other : level.players()) {
            if (other == self && !includeSelf) {
                continue;
            }
            if (range > 0.0D && distance(self, other) > range) {
                continue;
            }
            listed.add(other);
        }
        listed.sort(Comparator.comparingDouble(other -> distance(self, other)));

        if (listed.isEmpty()) {
            ChatFeedback.info(Component.translatable("doubletapfly.players.none"));
            return;
        }

        ChatFeedback.helpHeader(Component.translatable("doubletapfly.players.header", listed.size()));
        for (AbstractClientPlayer other : listed) {
            ChatFeedback.playerLine(buildLine(self, other));
        }
        ChatFeedback.tip(Component.translatable("doubletapfly.players.hint"));
    }

    private static Component buildLine(LocalPlayer self, AbstractClientPlayer other) {
        String name = other.getGameProfile().name();
        BlockPos pos = other.blockPosition();
        String coordinates = pos.getX() + " " + pos.getY() + " " + pos.getZ();

        Component nameComponent = Component.literal(name).withStyle(Style.EMPTY
                .withColor(ChatFormatting.WHITE)
                .withClickEvent(new ClickEvent.CopyToClipboard(name))
                .withHoverEvent(new HoverEvent.ShowText(
                        Component.translatable("doubletapfly.players.copy_name"))));

        Component coordinatesComponent = Component.literal(coordinates).withStyle(Style.EMPTY
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent.CopyToClipboard(coordinates))
                .withHoverEvent(new HoverEvent.ShowText(
                        Component.translatable("doubletapfly.players.copy_coords"))));

        return Component.empty()
                .append(nameComponent)
                .append(separator())
                .append(Component.translatable("doubletapfly.players.distance", format(distance(self, other)))
                        .withStyle(ChatFormatting.YELLOW))
                .append(separator())
                .append(coordinatesComponent);
    }

    private static Component separator() {
        return Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY);
    }

    private static double distance(LocalPlayer self, AbstractClientPlayer other) {
        return self.position().distanceTo(other.position());
    }

    private static String format(double value) {
        return String.valueOf(Math.round(value * 10.0D) / 10.0D);
    }
}
