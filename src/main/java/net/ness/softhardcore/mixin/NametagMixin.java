package net.ness.softhardcore.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.phys.Vec3;
import net.ness.softhardcore.util.LivesCacheManager;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class NametagMixin {

    @Inject(method = "renderNameTag", at = @At("TAIL"))
    private void renderExtraNametag(
            AbstractClientPlayer player,
            Component displayName,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            float partialTick,
            CallbackInfo ci
    ) {
        Integer cached = LivesCacheManager.getCachedLives(player.getUUID());
        if (cached == null) {
            return;
        }

        Vec3 nameTagPos = player.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, player.getViewYRot(partialTick));
        if (nameTagPos == null) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        int lives = cached;
        int armor = player.getArmorValue();
        String heartSymbol = "\u2764";
        String armorSymbol = "\uD83D\uDEE1";

        String livesText = heartSymbol + " " + lives;
        String armorText = armorSymbol + " " + armor;
        String combined = livesText + "   " + armorText;

        float totalWidth = client.font.width(combined);
        float startX = -totalWidth / 2f;

        int healthColor = 0xDC143C;
        int armorColor = 0x3399FF;

        poseStack.pushPose();
        poseStack.translate(nameTagPos.x, nameTagPos.y + 0.5, nameTagPos.z);
        poseStack.mulPose(client.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(0.025F, -0.025F, 0.025F);
        poseStack.translate(0.0F, -9.0F, 0.0F);

        Matrix4f matrix = poseStack.last().pose();
        int bgColor = (int) (0.15F * 255.0F) << 24;
        fillRect(matrix, bufferSource, startX - 3, -2, startX + totalWidth + 3, 9, bgColor);

        float currentX = startX;
        client.font.drawInBatch(livesText, currentX, 0, healthColor, false, matrix, bufferSource,
                Font.DisplayMode.NORMAL, 0, packedLight);
        currentX += client.font.width(livesText + "  ");
        client.font.drawInBatch(armorText, currentX, 0, armorColor, false, matrix, bufferSource,
                Font.DisplayMode.NORMAL, 0, packedLight);

        poseStack.popPose();
    }

    private static void fillRect(Matrix4f matrix, MultiBufferSource bufferSource,
                                 float x1, float y1, float x2, float y2, int color) {
        var buffer = bufferSource.getBuffer(net.minecraft.client.renderer.RenderType.guiOverlay());
        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        buffer.addVertex(matrix, x1, y2, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, x2, y2, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, x2, y1, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, x1, y1, 0).setColor(r, g, b, a);
    }
}
