package com.fluxt.doubletapfly;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.jspecify.annotations.Nullable;

/**
 * Client-side mod: double-tap the jump key (space) to switch creative-style flight on and off.
 *
 * <p>This is a client mod on purpose. Nothing is installed on the server and nothing is asked of
 * it beyond accepting the movement - the flight itself is produced by the vanilla client, which
 * already knows how to fly as soon as {@code Player#getAbilities().flying} is set.</p>
 */
@Mod(value = DoubleTapFlyMod.MOD_ID, dist = Dist.CLIENT)
public final class DoubleTapFlyMod {

    public static final String MOD_ID = "doubletapfly";

    private static @Nullable ModContainer container;

    public DoubleTapFlyMod(IEventBus modEventBus, ModContainer modContainer) {
        container = modContainer;
        modContainer.registerConfig(ModConfig.Type.CLIENT, DoubleTapFlyConfig.SPEC);

        // Hands the mod list a "Config" button that opens NeoForge's own settings screen, which is
        // built from the config spec with the regular Minecraft widgets - nothing custom to draw.
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (mod, parent) -> new ConfigurationScreen(mod, parent));
    }

    /** Used by {@code !doubletapfly gui} to open the same screen the mod list opens. */
    public static @Nullable ModContainer container() {
        return container;
    }
}
