package com.fluxt.doubletapfly;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/**
 * The rebindable key that switches the whole feature on and off.
 *
 * <p>Registering a {@link KeyMapping} is all it takes for it to show up in Options -&gt; Controls,
 * where the player can give it any key they like. It lives in its own category so it does not get
 * mixed up with the vanilla bindings.</p>
 */
@EventBusSubscriber(modid = DoubleTapFlyMod.MOD_ID, value = Dist.CLIENT)
public final class KeyBindings {

    /** Translation key is {@code key.category.doubletapfly.main}. */
    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(DoubleTapFlyMod.MOD_ID, "main"));

    /** Defaults to right shift, which vanilla leaves unbound. */
    public static final KeyMapping TOGGLE = new KeyMapping(
            "key.doubletapfly.toggle",
            InputConstants.KEY_RSHIFT,
            CATEGORY);

    /** Unbound by default; players can assign any key they like in the controls screen. */
    public static final KeyMapping FAST_PLACE = new KeyMapping(
            "key.doubletapfly.fast_place",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY);

    private KeyBindings() {}

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(TOGGLE);
        event.register(FAST_PLACE);
    }
}
