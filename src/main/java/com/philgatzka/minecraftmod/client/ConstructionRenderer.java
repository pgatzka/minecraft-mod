package com.philgatzka.minecraftmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.philgatzka.minecraftmod.MinecraftMod;
import com.philgatzka.minecraftmod.blueprint.BlueprintPlacement;
import com.philgatzka.minecraftmod.blueprint.BlueprintSelection;
import com.philgatzka.minecraftmod.construction.ConstructionSite;
import com.philgatzka.minecraftmod.construction.ConstructionSites;
import com.philgatzka.minecraftmod.item.BuildersWandItem;
import com.philgatzka.minecraftmod.network.ModNetwork;
import com.philgatzka.minecraftmod.network.RotateBlueprintPacket;
import com.philgatzka.minecraftmod.settlement.Territory;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.Map;
import java.util.Set;

// Draws the Builder's Wand preview (the chosen blueprint as see-through blocks where it would go), outlines construction sites,
// and shows the settlement's territory as a fence along its border. Also handles the rotate and show-territory keys.
@Mod.EventBusSubscriber(modid = MinecraftMod.MODID, value = Dist.CLIENT)
public final class ConstructionRenderer
{
    public static final KeyMapping ROTATE_KEY = new KeyMapping("key.minecraftmod.rotate_blueprint", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.minecraftmod");

    public static final KeyMapping TERRITORY_KEY = new KeyMapping("key.minecraftmod.toggle_territory", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.minecraftmod");

    private static final float GHOST_ALPHA = 0.5f;
    // The territory fence: drawn this far above and below the player, with a line every FENCE_STEP blocks
    private static final int FENCE_BELOW = 8;
    private static final int FENCE_ABOVE = 24;
    private static final int FENCE_STEP = 4;
    private static final double FENCE_MAX_DISTANCE = 160.0;

    // Toggled with TERRITORY_KEY; the territory is also always shown while holding a Builder's Wand
    private static boolean showTerritory;

    private ConstructionRenderer()
    {
    }

    // Called from MinecraftMod.ClientModEvents (mod event bus)
    public static void registerKeys(RegisterKeyMappingsEvent event)
    {
        event.register(ROTATE_KEY);
        event.register(TERRITORY_KEY);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        LocalPlayer player = Minecraft.getInstance().player;
        if (event.phase != TickEvent.Phase.END || player == null)
            return;
        while (ROTATE_KEY.consumeClick())
        {
            ItemStack wand = BuildersWandItem.findHeld(player);
            if (!wand.isEmpty() && BuildersWandItem.getBlueprint(wand) != null)
                ModNetwork.CHANNEL.sendToServer(new RotateBlueprintPacket());
        }
        while (TERRITORY_KEY.consumeClick())
        {
            showTerritory = !showTerritory;
            Component message;
            if (!showTerritory)
                message = Component.translatable("territory.minecraftmod.hidden");
            else if (ClientBlueprints.territory().isEmpty())
                message = Component.translatable("territory.minecraftmod.none");
            else
                message = Component.translatable("territory.minecraftmod.shown", ClientBlueprints.territory().size());
            player.displayClientMessage(message, true);
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || minecraft.level == null || minecraft.player == null)
            return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (ConstructionSite.Outline site : ClientBlueprints.sites())
            LevelRenderer.renderLineBox(poseStack, lines, box(site.area()), 1.0f, 0.85f, 0.1f, 1.0f);
        if (showTerritory || !BuildersWandItem.findHeld(minecraft.player).isEmpty())
            renderTerritory(poseStack, lines, minecraft.player.position());
        renderPreview(minecraft, poseStack, buffers);

        poseStack.popPose();
        buffers.endBatch(Sheets.translucentCullBlockSheet());
        buffers.endBatch(RenderType.lines());
    }

    private static void renderPreview(Minecraft minecraft, PoseStack poseStack, MultiBufferSource.BufferSource buffers)
    {
        LocalPlayer player = minecraft.player;
        ItemStack wand = BuildersWandItem.findHeld(player);
        ResourceLocation id = wand.isEmpty() ? null : BuildersWandItem.getBlueprint(wand);
        if (id == null || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK)
            return;
        ClientBlueprints.Preview preview = ClientBlueprints.preview(id);
        if (preview == null)
            return;

        Rotation rotation = BuildersWandItem.getRotation(wand);
        BlockPos target = BuildersWandItem.targetFor(minecraft.level, hit.getBlockPos(), hit.getDirection());
        BlueprintPlacement placement = new BlueprintPlacement(target, preview.blocks().anchor(), preview.blocks().size(), rotation);
        boolean hasCityBlock = player.isCreative() || preview.blocks().blocks().containsKey(preview.blocks().anchor())
                && player.getInventory().contains(new ItemStack(preview.blocks().blocks().get(preview.blocks().anchor()).getBlock()));
        boolean inTerritory = !ConstructionSites.needsTerritory(preview.blocks().blocks().get(preview.blocks().anchor()).getBlock())
                || Territory.covers(ClientBlueprints.territory(), placement.area());
        boolean valid = hasCityBlock && inTerritory && ConstructionSites.checkArea(minecraft.level, placement,
                ClientBlueprints.sites().stream().map(ConstructionSite.Outline::area).toList()) == null;

        // Green tint where it can be placed, red where it can't
        float red = valid ? 0.7f : 1.0f;
        float green = valid ? 1.0f : 0.45f;
        float blue = valid ? 0.7f : 0.45f;
        BlockRenderDispatcher dispatcher = minecraft.getBlockRenderer();
        VertexConsumer ghost = new GhostVertexConsumer(buffers.getBuffer(Sheets.translucentCullBlockSheet()), GHOST_ALPHA);
        for (Map.Entry<BlockPos, BlockState> block : preview.visibleBlocks().entrySet())
        {
            BlockState state = block.getValue().rotate(rotation);
            BlockPos pos = placement.worldPos(block.getKey());
            poseStack.pushPose();
            poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
            dispatcher.getModelRenderer().renderModel(poseStack.last(), ghost, state, dispatcher.getBlockModel(state), red, green, blue,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null); // null = all of the model's render layers
            poseStack.popPose();
        }

        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(poseStack, lines, box(placement.area()), red, green, blue, 1.0f);
        LevelRenderer.renderLineBox(poseStack, lines, new AABB(target).inflate(0.01), 1.0f, 1.0f, 1.0f, 1.0f);
    }

    // A fence of lines along every chunk edge where the territory ends, near the player
    private static void renderTerritory(PoseStack poseStack, VertexConsumer lines, Vec3 player)
    {
        Set<ChunkPos> territory = ClientBlueprints.territory();
        int bottom = (int) Math.floor(player.y) - FENCE_BELOW;
        int top = (int) Math.floor(player.y) + FENCE_ABOVE;
        for (ChunkPos chunk : territory)
        {
            if (Math.abs(chunk.getMiddleBlockX() - player.x) > FENCE_MAX_DISTANCE || Math.abs(chunk.getMiddleBlockZ() - player.z) > FENCE_MAX_DISTANCE)
                continue;
            int minX = chunk.getMinBlockX();
            int minZ = chunk.getMinBlockZ();
            int maxX = minX + 16;
            int maxZ = minZ + 16;
            if (!territory.contains(new ChunkPos(chunk.x, chunk.z - 1)))
                fence(poseStack, lines, minX, minZ, maxX, minZ, bottom, top);
            if (!territory.contains(new ChunkPos(chunk.x, chunk.z + 1)))
                fence(poseStack, lines, minX, maxZ, maxX, maxZ, bottom, top);
            if (!territory.contains(new ChunkPos(chunk.x - 1, chunk.z)))
                fence(poseStack, lines, minX, minZ, minX, maxZ, bottom, top);
            if (!territory.contains(new ChunkPos(chunk.x + 1, chunk.z)))
                fence(poseStack, lines, maxX, minZ, maxX, maxZ, bottom, top);
        }
    }

    // A grid on the vertical plane from (x1, z1) to (x2, z2), between the heights bottom and top
    private static void fence(PoseStack poseStack, VertexConsumer lines, int x1, int z1, int x2, int z2, int bottom, int top)
    {
        float red = 0.2f;
        float green = 0.8f;
        float blue = 1.0f;
        for (int y = bottom - Math.floorMod(bottom, FENCE_STEP); y <= top; y += FENCE_STEP)
            line(poseStack, lines, x1, y, z1, x2, y, z2, red, green, blue);
        int length = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        int stepX = Integer.signum(x2 - x1);
        int stepZ = Integer.signum(z2 - z1);
        for (int i = 0; i <= length; i += FENCE_STEP)
            line(poseStack, lines, x1 + stepX * i, bottom, z1 + stepZ * i, x1 + stepX * i, top, z1 + stepZ * i, red, green, blue);
    }

    private static void line(PoseStack poseStack, VertexConsumer lines, float x1, float y1, float z1, float x2, float y2, float z2,
                             float red, float green, float blue)
    {
        PoseStack.Pose pose = poseStack.last();
        Vector3f normal = new Vector3f(x2 - x1, y2 - y1, z2 - z1).normalize();
        lines.vertex(pose.pose(), x1, y1, z1).color(red, green, blue, 1.0f).normal(pose.normal(), normal.x(), normal.y(), normal.z()).endVertex();
        lines.vertex(pose.pose(), x2, y2, z2).color(red, green, blue, 1.0f).normal(pose.normal(), normal.x(), normal.y(), normal.z()).endVertex();
    }

    // Slightly larger than the blocks so the lines don't flicker against block faces
    private static AABB box(BlueprintSelection area)
    {
        return new AABB(area.min()).minmax(new AABB(area.max())).inflate(0.02);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event)
    {
        ClientBlueprints.clear();
    }
}
