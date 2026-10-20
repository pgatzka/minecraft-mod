package com.philgatzka.minecraftmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.philgatzka.minecraftmod.MinecraftMod;
import com.philgatzka.minecraftmod.blueprint.BlueprintSelection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Outlines the blueprint selection in the world: corner 1 orange, corner 2 blue, the whole area green (red if too large)
@Mod.EventBusSubscriber(modid = MinecraftMod.MODID, value = Dist.CLIENT)
public final class SelectionRenderer
{
    private SelectionRenderer()
    {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || minecraft.level == null
                || !ClientSelection.isIn(minecraft.level.dimension()))
            return;
        BlockPos first = ClientSelection.first();
        BlockPos second = ClientSelection.second();
        if (first == null && second == null)
            return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        if (first != null && second != null)
        {
            BlueprintSelection selection = BlueprintSelection.of(first, second);
            // Slightly larger than the blocks so the lines don't flicker against block faces
            AABB area = new AABB(selection.min()).minmax(new AABB(selection.max())).inflate(0.02);
            if (selection.fitsMaxSize())
                LevelRenderer.renderLineBox(poseStack, lines, area, 0.2f, 1.0f, 0.3f, 1.0f);
            else
                LevelRenderer.renderLineBox(poseStack, lines, area, 1.0f, 0.2f, 0.2f, 1.0f);
        }
        if (first != null)
            LevelRenderer.renderLineBox(poseStack, lines, new AABB(first).inflate(0.01), 1.0f, 0.6f, 0.1f, 1.0f);
        if (second != null)
            LevelRenderer.renderLineBox(poseStack, lines, new AABB(second).inflate(0.01), 0.2f, 0.5f, 1.0f, 1.0f);
        poseStack.popPose();
        buffers.endBatch(RenderType.lines());
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event)
    {
        ClientSelection.clear();
    }
}
