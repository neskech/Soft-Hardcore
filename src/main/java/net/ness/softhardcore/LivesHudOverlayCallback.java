package net.ness.softhardcore;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import net.ness.softhardcore.config.MyConfig;
import net.ness.softhardcore.ui.ScalingSystem;
import net.ness.softhardcore.util.LivesCacheManager;

public class LivesHudOverlayCallback implements HudRenderCallback {
    private static final Identifier HEART_TEXTURE = Identifier.of(SoftHardcore.MOD_ID, "textures/heart_icon.png");
    private final MinecraftClient client;

    LivesHudOverlayCallback(MinecraftClient client) {
        this.client = client;
    }

    @Override
    public void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        if (this.client.player == null) {
            return;
        }

        Integer cached = LivesCacheManager.getCachedLives(this.client.player.getUuid());
        if (cached == null) {
            return;
        }

        int lives = cached;
        int maxLives = MyConfig.DEFAULT_LIVES;

        ScalingSystem.ContainerDimensions screenContainer = ScalingSystem.ContainerDimensions.screen();

        int hudWidth = ScalingSystem.widthPercent(screenContainer, 0.12f);
        int hudHeight = ScalingSystem.heightPercent(screenContainer, 0.03f);
        int iconSize = ScalingSystem.minDimensionPercent(screenContainer, 0.02f);
        int margin = ScalingSystem.widthPercent(screenContainer, 0.01f);
        int padding = ScalingSystem.widthPercent(screenContainer, 0.005f);

        int x = screenContainer.width - hudWidth - margin;
        int y = margin;

        drawContext.fill(x - padding, y - padding, x + hudWidth + padding, y + hudHeight + padding, 0x80000000);
        drawContext.drawTexture(HEART_TEXTURE, x, y, 0, 0, iconSize, iconSize, 16, 16);

        String livesText = lives + "/" + maxLives;
        int textColor = getLivesColor(lives, maxLives);
        int textX = x + iconSize + ScalingSystem.widthPercent(ScalingSystem.ContainerDimensions.of(hudWidth, 1), 0.05f);
        int textY = y + (hudHeight - client.textRenderer.fontHeight) / 2;
        drawContext.drawText(client.textRenderer, livesText, textX, textY, textColor, true);
    }

    private int getLivesColor(int lives, int maxLives) {
        if (lives >= maxLives) {
            return 0x00FF00;
        } else if (lives >= maxLives * 0.5) {
            return 0xFFFF00;
        } else if (lives > 1) {
            return 0xFF8800;
        } else {
            return 0xFF0000;
        }
    }
}
