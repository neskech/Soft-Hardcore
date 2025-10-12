package net.ness.softhardcore;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.ness.softhardcore.component.LivesComponent;
import net.ness.softhardcore.component.MyComponents;
import net.ness.softhardcore.config.MyConfig;

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

        // Position in top-right corner
        int x = this.client.getWindow().getScaledWidth() - 100;
        int y = 10;

        // Draw semi-transparent background
        drawContext.fill(x - 5, y - 5, x + 85, y + 25, 0x80000000);

        // Draw heart icon
        drawContext.drawTexture(HEART_TEXTURE, x, y, 0, 0, 16, 16, 16, 16);

        // Draw lives text with color coding
        String livesText = lives + "/" + maxLives;
        int textColor = getLivesColor(lives, maxLives);
        drawContext.drawText(client.textRenderer, livesText, x + 20, y + 4, textColor, true);
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
