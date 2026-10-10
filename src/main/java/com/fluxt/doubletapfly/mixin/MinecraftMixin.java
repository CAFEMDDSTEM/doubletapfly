package com.fluxt.doubletapfly.mixin;

import com.fluxt.doubletapfly.FastPlaceHelper;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks the end of {@code Minecraft#startUseItem()}.
 *
 * <p>That method is where vanilla hard-codes {@code this.rightClickDelay = 4}, and the client tick
 * only calls it again once that counter reaches zero. Overwriting the counter here is therefore all
 * it takes to place blocks at a faster rate - and it is also the last moment at which the placement
 * that just happened is known, so the extra "ring" placements are made from here too.</p>
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Shadow
    private int rightClickDelay;

    @Inject(method = "startUseItem", at = @At("TAIL"))
    private void doubletapfly$fastBlockPlacement(CallbackInfo callbackInfo) {
        Minecraft minecraft = (Minecraft) (Object) this;
        this.rightClickDelay = FastPlaceHelper.afterPlacement(minecraft, minecraft.hitResult);
    }
}
