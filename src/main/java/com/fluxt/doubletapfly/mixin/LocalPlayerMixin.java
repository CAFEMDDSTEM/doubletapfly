package com.fluxt.doubletapfly.mixin;

import com.fluxt.doubletapfly.ClientFlightController;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * {@code LocalPlayer#sendPosition} stamps every movement packet with {@code this.onGround()}.
 *
 * <p>A vanilla server only resets its fall-distance counter when the packet says "on ground" or
 * when the player moves up - see {@code ServerGamePacketListenerImpl#handleMovePlayer}. To shield
 * the landing after a flight we therefore report one single "on ground" tick once the descent we
 * have caused gets close to the safe-fall threshold. Everything else is untouched: the moment we
 * stop flying, the real value is reported again and normal fall damage applies.</p>
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @Redirect(
            method = "sendPosition",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;onGround()Z"
            )
    )
    private boolean doubletapfly$reportOnGround(LocalPlayer player) {
        if (ClientFlightController.shouldForceOnGround()) {
            return true;
        }
        return player.onGround();
    }
}
