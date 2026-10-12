package com.fluxt.doubletapfly;

import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * The {@code !help} command: a list of everything this mod does, and a short how-to per feature.
 *
 * <p>Each feature is one translation triple - {@code title}, {@code summary} for the list and
 * {@code detail} for the full explanation. {@code detail} holds the whole how-to as one string with
 * embedded newlines, which the chat renderer turns into separate lines.</p>
 */
public final class FeatureHelp {

    /** A feature, its aliases, and the translation keys that describe it. */
    private record Feature(String id, List<String> aliases) {

        String titleKey() {
            return "doubletapfly.help.feature." + id + ".title";
        }

        String summaryKey() {
            return "doubletapfly.help.feature." + id + ".summary";
        }

        String detailKey() {
            return "doubletapfly.help.feature." + id + ".detail";
        }

        boolean matches(String name) {
            if (id.equalsIgnoreCase(name)) {
                return true;
            }
            for (String alias : aliases) {
                if (alias.equalsIgnoreCase(name)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final List<Feature> FEATURES = List.of(
            new Feature("flight", List.of("fly", "飞行", "飞行速度")),
            new Feature("fastplace", List.of("快速放置", "放置")),
            new Feature("airplace", List.of("air", "悬空放置", "悬空")),
            new Feature("fastuse", List.of("快速使用", "使用")),
            new Feature("players", List.of("coords", "坐标", "玩家坐标")),
            new Feature("tp", List.of("teleport", "传送")));

    private FeatureHelp() {}

    /** Prints every feature with a one line summary. */
    public static void sendList() {
        ChatFeedback.helpHeader(Component.translatable("doubletapfly.help.list.header"));
        for (Feature feature : FEATURES) {
            ChatFeedback.helpLine("!help " + feature.id(), Component.translatable(feature.summaryKey()));
        }
        ChatFeedback.tip(Component.translatable("doubletapfly.help.list.tip"));
    }

    /**
     * Prints the full how-to for one feature.
     *
     * @return false when the name is not one of ours, so the caller can complain about it
     */
    public static boolean sendFeature(String name) {
        String wanted = name.trim().toLowerCase(Locale.ROOT);
        for (Feature feature : FEATURES) {
            if (feature.matches(wanted)) {
                ChatFeedback.helpHeader(Component.translatable(feature.titleKey()));
                ChatFeedback.plain(Component.translatable(feature.detailKey()).withStyle(ChatFormatting.GRAY));
                return true;
            }
        }
        return false;
    }
}
