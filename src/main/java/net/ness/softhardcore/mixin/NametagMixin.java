package net.ness.softhardcore.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
//import net.minecraft.util.math.RotationAxis;
import net.ness.softhardcore.component.LivesComponent;
import net.ness.softhardcore.component.MyComponents;
import net.ness.softhardcore.config.MyConfig;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

    @Mixin(PlayerEntityRenderer.class)
    public abstract class NametagMixin {

        @Inject(method = "renderLabelIfPresent", at = @At("TAIL"))
        private void renderExtraNametag(AbstractClientPlayerEntity player, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
            LivesComponent component = MyComponents.LIVES_KEY.get(player);
            if (component == null) return; 

            // Get the dispatcher and text renderer from MinecraftClient
            MinecraftClient client = MinecraftClient.getInstance();
            EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
            TextRenderer textRenderer = client.textRenderer;
            
            int health = Math.round(player.getHealth());
            int armor = player.getArmor();
            String heartSymbol = "\u2764";
            String armorSymbol = "\uD83D\uDEE1"; 

            String healthText = heartSymbol + " " + health;
            String armorText = armorSymbol + " " + armor;

            String combined = healthText + "   " + armorText;
            float totalWidth = textRenderer.getWidth(combined);
            float startX = -totalWidth / 2f;        

            int healthColor = 0xDC143C; // Crimson red
            int armorColor = 0x3399FF; // soft blue

            matrices.push();
            matrices.translate(0.0F, player.getNameLabelHeight() + 0.35F, 0.0F);
            matrices.multiply(dispatcher.getRotation());
            matrices.scale(-0.025F, -0.025F, 0.025F);
    
            Matrix4f matrix4f = matrices.peek().getPositionMatrix();
            int bgColor = (int)(0.15F * 255.0F) << 24; // adjust float to change opacity 
            fillRect(matrix4f, vertexConsumers, startX - 3, -2, startX + totalWidth + 3, 9, bgColor);

            float currentX = startX;
            // Draw health (heart + number)
            textRenderer.draw(healthText, currentX, 0, healthColor, false, matrix4f, vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, light);

            currentX += textRenderer.getWidth(healthText + "  ");

            // Draw armor (blue)
            textRenderer.draw(armorText, currentX, 0, armorColor, false, matrix4f, vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, light);

        matrices.pop();
        }

        private static void fillRect(Matrix4f matrix, VertexConsumerProvider vertexConsumers, float x1, float y1, float x2, float y2, int color) {
            var buffer = vertexConsumers.getBuffer(net.minecraft.client.render.RenderLayer.getGuiOverlay());
            float a = (color >> 24 & 255) / 255.0F;
            float r = (color >> 16 & 255) / 255.0F;
            float g = (color >> 8 & 255) / 255.0F;
            float b = (color & 255) / 255.0F;
            buffer.vertex(matrix, x1, y2, 0).color(r, g, b, a).next();
            buffer.vertex(matrix, x2, y2, 0).color(r, g, b, a).next();
            buffer.vertex(matrix, x2, y1, 0).color(r, g, b, a).next();
            buffer.vertex(matrix, x1, y1, 0).color(r, g, b, a).next();
        }
    }