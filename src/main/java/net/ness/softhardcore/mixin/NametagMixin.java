package net.ness.softhardcore.mixin;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.RotationAxis;
import net.ness.softhardcore.component.LivesComponent;
import net.ness.softhardcore.component.MyComponents;
import net.ness.softhardcore.config.MyConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class NametagMixin<T extends Entity> {

    @Inject(method = "renderLabelIfPresent", at = @At("TAIL"))
    private void renderExtraNametag(Entity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (shouldRenderExtraNametag(entity)) {
            renderCustomNametag(entity, matrices, vertexConsumers, light);
        }
    }

    private boolean shouldRenderExtraNametag(Entity entity) {
        return entity instanceof PlayerEntity;
    }

    private void renderCustomNametag(Entity entity, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (!(entity instanceof PlayerEntity player)) {
            return;
        }

        double d = this.dispatcher.getSquaredDistanceToCamera(entity);
		if (!(d > 4096.0)) {
			boolean bl = !entity.isSneaky();
			float f = entity.getNameLabelHeight();
			int i = "deadmau5".equals(text.getString()) ? -10 : 0;
			matrices.push();
			matrices.translate(0.0F, f, 0.0F);
			matrices.multiply(this.dispatcher.getRotation());
			matrices.scale(-0.025F, -0.025F, 0.025F);
			Matrix4f matrix4f = matrices.peek().getPositionMatrix();
			float g = MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.25F);
			int j = (int)(g * 255.0F) << 24;
			TextRenderer textRenderer = this.getTextRenderer();
			float h = (float)(-textRenderer.getWidth(text) / 2);
			textRenderer.draw(
				text, h, (float)i, 553648127, false, matrix4f, vertexConsumers, bl ? TextRenderer.TextLayerType.SEE_THROUGH : TextRenderer.TextLayerType.NORMAL, j, light
			);
			if (bl) {
				textRenderer.draw(text, h, (float)i, -1, false, matrix4f, vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, light);
			}

			matrices.pop();
		}

        // Get lives from component
        LivesComponent component = MyComponents.LIVES_KEY.get(player);
        if (component == null) {
            return; // Component not available yet
        }

        int lives = component.getLives();
        
        // Translate the matrix to position the new nametag below the existing one
        matrices.push();

        // Position the custom nametag slightly below the original
        matrices.translate(0.0, -0.15, 2.0);
        matrices.scale(0.025f, 0.025f, 0.025f);
        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(180));
        
        // Set up the text with color coding
        String livesText = "❤ " + lives;
        int textColor = getLivesColor(lives);

        // Get text renderer from the client
        TextRenderer renderer = net.minecraft.client.MinecraftClient.getInstance().textRenderer;

        // Render the text
        float h = (float)(-renderer.getWidth(livesText) / 2);
        float i = 0;
        renderer.draw(livesText, h, i, textColor, false, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, light);
        matrices.pop();
    }

    private int getLivesColor(int lives) {
        if (lives >= MyConfig.DEFAULT_LIVES) {
            return 0x00FF00; // Green - at max lives
        } else if (lives >= MyConfig.DEFAULT_LIVES * 0.5) {
            return 0xFFFF00; // Yellow - 50-99% of max
        } else if (lives > 1) {
            return 0xFF8800; // Orange - 25-49% of max
        } else {
            return 0xFF0000; // Red - < 25% of max
        }
    }
}