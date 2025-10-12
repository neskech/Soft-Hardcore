package net.ness.softhardcore;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.ness.softhardcore.component.LivesComponent;
import net.ness.softhardcore.component.MyComponents;
import net.ness.softhardcore.config.MyConfig;
import net.ness.softhardcore.ui.ScalingSystem;

public class LivesHudOverlayCallback implements HudRenderCallback {
    private static final Identifier HEART_TEXTURE = new Identifier(SoftHardcore.MOD_ID, "textures/heart_icon.png");
    private final MinecraftClient client;

    LivesHudOverlayCallback(MinecraftClient client) {
        this.client = client;
    }

    @Override
    public void onHudRender(DrawContext drawContext, float v) {
        if (this.client.player == null) return;

        LivesComponent component = MyComponents.LIVES_KEY.get(this.client.player);
        if (component == null) return;

        int lives = component.getLives();
        int maxLives = MyConfig.DEFAULT_LIVES;

        // Create screen container dimensions
        ScalingSystem.ContainerDimensions screenContainer = ScalingSystem.ContainerDimensions.screen();
        
        // Calculate dimensions relative to screen
        int hudWidth = ScalingSystem.widthPercent(screenContainer, 0.12f); // 12% of screen width
        int hudHeight = ScalingSystem.heightPercent(screenContainer, 0.03f); // 3% of screen height
        int iconSize = ScalingSystem.minDimensionPercent(screenContainer, 0.02f); // 2% of smaller dimension
        int margin = ScalingSystem.widthPercent(screenContainer, 0.01f); // 1% margin
        int padding = ScalingSystem.widthPercent(screenContainer, 0.005f); // 0.5% padding

        // Position in top-right corner
        int x = screenContainer.width - hudWidth - margin;
        int y = margin;

        // Draw semi-transparent background
        drawContext.fill(x - padding, y - padding, x + hudWidth + padding, y + hudHeight + padding, 0x80000000);

        // Draw heart icon
        drawContext.drawTexture(HEART_TEXTURE, x, y, 0, 0, iconSize, iconSize, 16, 16);

        // Draw lives text with color coding
        String livesText = lives + "/" + maxLives;
        int textColor = getLivesColor(lives, maxLives);
        int textX = x + iconSize + ScalingSystem.widthPercent(ScalingSystem.ContainerDimensions.of(hudWidth, 1), 0.05f);
        int textY = y + (hudHeight - client.textRenderer.fontHeight) / 2;
        drawContext.drawText(client.textRenderer, livesText, textX, textY, textColor, true);
    }

    private int getLivesColor(int lives, int maxLives) {
        if (lives >= maxLives) {
            return 0x00FF00; // Green - at max lives
        } else if (lives >= maxLives * 0.5) {
            return 0xFFFF00; // Yellow - 50-99% of max
        } else if (lives > 1) {
            return 0xFF8800; // Orange - 25-49% of max
        } else {
            return 0xFF0000; // Red - < 25% of max
        }
    }
}
