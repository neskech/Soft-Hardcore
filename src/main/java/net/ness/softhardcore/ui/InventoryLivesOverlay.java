package net.ness.softhardcore.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.ness.softhardcore.config.MyConfig;
import net.ness.softhardcore.util.LivesCacheManager;

public final class InventoryLivesOverlay {
    private static final String HEART_SYMBOL = "\u2764";

    private static final int BOX_HEIGHT = 18;
    private static final int BOX_GAP = 2;
    private static final int HORIZONTAL_PADDING = 4;
    private static final int HEART_TEXT_GAP = 1;
    private static final int CORNER_RADIUS = 2;
    private static final int BORDER_WIDTH = 1;

    private static final int PANEL_COLOR = 0xFF8B8B8B;
    private static final int BORDER_COLOR = 0xFF373737;
    private static final int HEART_COLOR = 0xa63f3f;
    private static final int COUNT_COLOR = 0xFFFFFF;

    private InventoryLivesOverlay() {
    }

    public static void render(DrawContext context, MinecraftClient client, int guiX, int guiY) {
        if (!MyConfig.INVENTORY_LIVES_OVERLAY_ENABLED || client.player == null) {
            return;
        }

        Integer cachedLives = LivesCacheManager.getCachedLives(client.player.getUuid());
        if (cachedLives == null) {
            return;
        }

        String countText = String.valueOf(cachedLives);
        int heartWidth = client.textRenderer.getWidth(HEART_SYMBOL);
        int countWidth = client.textRenderer.getWidth(countText);
        int contentWidth = heartWidth + HEART_TEXT_GAP + countWidth;
        int boxWidth = HORIZONTAL_PADDING * 2 + contentWidth;

        int boxX = guiX;
        int boxY = guiY - BOX_HEIGHT - BOX_GAP;
        int boxBottom = boxY + BOX_HEIGHT;

        drawPanel(context, boxX, boxY, boxWidth, BOX_HEIGHT);

        int contentLeft = boxX + HORIZONTAL_PADDING;
        int contentRight = boxX + boxWidth - HORIZONTAL_PADDING;
        int textY = centeredTextY(boxY, boxBottom);
        int heartX = contentLeft + (contentRight - contentLeft - contentWidth) / 2;
        int countX = heartX + heartWidth + HEART_TEXT_GAP;

        context.drawText(client.textRenderer, HEART_SYMBOL, heartX, textY, HEART_COLOR, false);
        context.drawText(client.textRenderer, countText, countX, textY, COUNT_COLOR, false);
    }

    private static int centeredTextY(int top, int bottom) {
        return (top + bottom - 9) / 2 + 1;
    }

    private static void drawPanel(DrawContext context, int x, int y, int width, int height) {
        drawRoundedRect(context, x, y, width, height, CORNER_RADIUS, BORDER_COLOR);

        int inset = BORDER_WIDTH;
        int innerWidth = width - inset * 2;
        int innerHeight = height - inset * 2;
        if (innerWidth <= 0 || innerHeight <= 0) {
            return;
        }

        int innerRadius = Math.max(0, CORNER_RADIUS - inset);
        drawRoundedRect(context, x + inset, y + inset, innerWidth, innerHeight, innerRadius, PANEL_COLOR);
    }

    private static void drawRoundedRect(DrawContext context, int x, int y, int width, int height, int cornerRadius, int color) {
        int radius = Math.min(cornerRadius, Math.min(width, height) / 2);
        if (radius <= 0) {
            context.fill(x, y, x + width, y + height, color);
            return;
        }

        context.fill(x + radius, y, x + width - radius, y + height, color);
        context.fill(x, y + radius, x + radius, y + height - radius, color);
        context.fill(x + width - radius, y + radius, x + width, y + height - radius, color);

        context.fill(x + 1, y, x + radius, y + 1, color);
        context.fill(x, y + 1, x + 1, y + radius, color);

        context.fill(x + width - radius, y, x + width - 1, y + 1, color);
        context.fill(x + width - 1, y + 1, x + width, y + radius, color);

        context.fill(x, y + height - radius, x + 1, y + height - 1, color);
        context.fill(x + 1, y + height - 1, x + radius, y + height, color);

        context.fill(x + width - radius, y + height - 1, x + width - 1, y + height, color);
        context.fill(x + width - 1, y + height - radius, x + width, y + height - 1, color);
    }
}
