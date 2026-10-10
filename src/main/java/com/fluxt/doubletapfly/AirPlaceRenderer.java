package com.fluxt.doubletapfly;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Draws the red box around the air place target.
 *
 * <p>Vanilla refuses to draw a block outline for air ({@code LevelRenderer} skips anything that
 * {@code isAir()}), so the box has to be drawn by hand. {@code AfterTranslucentBlocks} is fired
 * right between the two vanilla {@code renderBlockOutline} calls, on the very same pose stack and
 * buffer source, so the drawing code below is a copy of what the vanilla outline does - only with a
 * red colour and a full cube instead of the target block's shape.</p>
 */
@EventBusSubscriber(modid = DoubleTapFlyMod.MOD_ID, value = Dist.CLIENT)
public final class AirPlaceRenderer {

    /** Opaque red. */
    private static final int OUTLINE_COLOR = 0xFFFF0000;
    private static final float OUTLINE_WIDTH = 2.0F;

    private AirPlaceRenderer() {}

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        BlockPos pos = AirPlaceHelper.target();
        if (pos == null || !DoubleTapFlyConfig.airPlaceRender()) {
            return;
        }

        LevelRenderState levelRenderState = event.getLevelRenderState();
        Vec3 camera = levelRenderState.cameraRenderState.pos;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderTypes.lines());
        ShapeRenderer.renderShape(
                poseStack,
                consumer,
                Shapes.block(),
                pos.getX() - camera.x,
                pos.getY() - camera.y,
                pos.getZ() - camera.z,
                OUTLINE_COLOR,
                OUTLINE_WIDTH);
        buffers.endLastBatch();
    }
}
