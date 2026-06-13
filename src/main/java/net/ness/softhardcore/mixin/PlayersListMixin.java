package net.ness.softhardcore.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Identifier;
import net.ness.softhardcore.SoftHardcore;
import net.ness.softhardcore.SoftHardcoreClient;
import net.ness.softhardcore.config.MyConfig;
import net.ness.softhardcore.ui.ScalingSystem;
import net.ness.softhardcore.util.LivesCacheManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mixin(PlayerListHud.class)
public abstract class PlayersListMixin {

    private static final int BACKGROUND_COLOR = 0x60000000;
    private static final int PLAYER_ENTRY_BACKGROUND_COLOR = 0x4aaaaaaa;

    private static final Identifier TEAM_ICON = Identifier.of(SoftHardcore.MOD_ID, "textures/team_icon.png");
    private static final Identifier PING_UNKNOWN_SPRITE = Identifier.ofVanilla("icon/ping_unknown");
    private static final Identifier PING_1_SPRITE = Identifier.ofVanilla("icon/ping_1");
    private static final Identifier PING_2_SPRITE = Identifier.ofVanilla("icon/ping_2");
    private static final Identifier PING_3_SPRITE = Identifier.ofVanilla("icon/ping_3");
    private static final Identifier PING_4_SPRITE = Identifier.ofVanilla("icon/ping_4");
    private static final Identifier PING_5_SPRITE = Identifier.ofVanilla("icon/ping_5");

    private static final Vector2i TEAM_ICON_TEX_DIMENSIONS = new Vector2i(8, 8);

    @Shadow @Final
    private static Comparator<PlayerListEntry> ENTRY_ORDERING;

    @Accessor("client")
    abstract MinecraftClient client();

    private List<PlayerListEntry> collectPlayerEntries() {
        return this.client().player.networkHandler.getListedPlayerListEntries().stream()
                .sorted(ENTRY_ORDERING)
                .limit(80L)
                .toList();
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void renderPlayersList(@NotNull DrawContext context, int scaledWindowWidth, Scoreboard scoreboard,
                                  @Nullable ScoreboardObjective objective, CallbackInfo ci) {
        List<PlayerListEntry> playerEntries = collectPlayerEntries();
        int numPlayers = playerEntries.size();

        if (numPlayers == 0) {
            return;
        }

        ScalingSystem.ContainerDimensions screenContainer = ScalingSystem.ContainerDimensions.screen();

        int numColumns = (int) Math.ceil((float) numPlayers / MyConfig.MAX_SCOREBOARD_ROWS);
        int numRows = Math.min(numPlayers, MyConfig.MAX_SCOREBOARD_ROWS);

        int entryWidth = playerEntryWidth(screenContainer);
        int entryHeight = playerEntryHeight(screenContainer);
        int gapX = playerEntryGapX(screenContainer);
        int gapY = playerEntryGapY(screenContainer);

        int innerWidth = numColumns * entryWidth + (numColumns - 1) * gapX;
        int innerHeight = numRows * entryHeight + (numRows - 1) * gapY;

        int centerX = scaledWindowWidth / 2;
        int entryX = centerX - innerWidth / 2;
        int topMarginY = hudMarginTopY(screenContainer);
        int bottomBackgroundMarginY = hudBackgroundBottomMarginY(screenContainer);
        int entryY = topMarginY + textUpperMarginY(screenContainer) + textLowerMarginY(screenContainer);

        int leftX = entryX - hudSideMargins(screenContainer);
        int rightX = entryX + numColumns * entryWidth + (numColumns - 1) * gapX + hudSideMargins(screenContainer);
        int topY = topMarginY;
        int bottomY = topY + innerHeight + bottomBackgroundMarginY + textLowerMarginY(screenContainer) + textUpperMarginY(screenContainer);

        RenderSystem.enableBlend();
        context.fill(leftX, topY, rightX, bottomY, BACKGROUND_COLOR);

        TextRenderer textRenderer = this.client().textRenderer;
        String title = "§l" + getServerName();
        int healthColor = 0xDC143C;
        drawCenteredText(context, textRenderer, title, screenContainer.width / 2, topY + textUpperMarginY(screenContainer), healthColor);

        String count = numPlayers == 1 ? numPlayers + " Player" : numPlayers + " Players";
        drawCenteredText(context, textRenderer, count, screenContainer.width / 2, topY + textUpperMarginY(screenContainer) + 10, 0xFFFFFF);

        Map<String, PlayerEntity> playerMap = buildPlayerMap();

        for (int col = 0; col < numColumns; col++) {
            for (int row = 0; row < MyConfig.MAX_SCOREBOARD_ROWS; row++) {
                int i = col * MyConfig.MAX_SCOREBOARD_ROWS + row;
                if (i >= numPlayers) {
                    break;
                }
                int x = entryX + (entryWidth + gapX) * col;
                int y = entryY + (entryHeight + gapY) * row;
                renderPlayerEntry(context, x, y, playerEntries.get(i), playerMap, scoreboard, entryWidth, entryHeight);
            }
        }

        context.draw();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        ci.cancel();
    }

    private void renderPlayerEntry(DrawContext context, int x, int y, PlayerListEntry entry, Map<String, PlayerEntity> map,
                                   Scoreboard scoreboard, int entryWidth, int entryHeight) {
        context.fill(x, y, x + entryWidth, y + entryHeight, PLAYER_ENTRY_BACKGROUND_COLOR);
        GameProfile profile = entry.getProfile();

        int leftX = x;
        ScalingSystem.ContainerDimensions entryContainer = ScalingSystem.ContainerDimensions.of(entryWidth, entryHeight);

        int iconSize = iconDimensions(entryContainer);
        int iconMarginSize = iconMargin(entryContainer);
        int nameTagMarginSize = nameTagMargin(entryContainer);
        int pingMarginSize = pingMargin(entryContainer);
        int livesMarginSize = livesMargin(entryContainer);
        int pingHeightSize = pingHeight(entryContainer);
        int pingWidthSize = pingWidth(entryContainer);

        PlayerSkinDrawer.draw(context, entry.getSkinTextures(), x, y, iconSize);

        x += iconSize + iconMarginSize;
        Team team = scoreboard.getScoreHolderTeam(profile.getName());
        int color = 0xFFFFFF;
        if (team != null && team.getColor() != null) {
            Integer colorValue = team.getColor().getColorValue();
            if (colorValue != null) {
                color = colorValue;
            }
        }

        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        context.setShaderColor(r, g, b, 1.0f);
        context.drawTexture(TEAM_ICON, x, y, iconSize, iconSize, 0, 0,
                TEAM_ICON_TEX_DIMENSIONS.x, TEAM_ICON_TEX_DIMENSIONS.y,
                TEAM_ICON_TEX_DIMENSIONS.x, TEAM_ICON_TEX_DIMENSIONS.y);
        context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        x += iconSize + nameTagMarginSize;

        drawScaledText(context, this.client().textRenderer, profile.getName(), x, y, entryHeight, color);

        int rightX = leftX + entryWidth - pingWidthSize - pingMarginSize;
        drawPingIcon(context, rightX, y, pingWidthSize, pingHeightSize, entry.getLatency());

        String lives = "?";
        UUID playerUuid = profile.getId();

        Integer cachedLives = LivesCacheManager.getCachedLives(playerUuid);
        if (cachedLives != null) {
            lives = String.valueOf(cachedLives);
        }

        int healthColor = 0xDC143C;
        String heartSymbol = "\u2764";
        String livesText = lives + heartSymbol;

        float scaleY = (float) entryHeight / 8;
        float realTextWidth = this.client().textRenderer.getWidth(livesText) * scaleY;
        rightX -= (int) realTextWidth + livesMarginSize;

        drawScaledText(context, this.client().textRenderer, livesText, rightX, y, entryHeight, healthColor);
    }

    private static void drawPingIcon(DrawContext context, int x, int y, int width, int height, int latency) {
        Identifier sprite;
        if (latency < 0) {
            sprite = PING_UNKNOWN_SPRITE;
        } else if (latency < 150) {
            sprite = PING_5_SPRITE;
        } else if (latency < 300) {
            sprite = PING_4_SPRITE;
        } else if (latency < 600) {
            sprite = PING_3_SPRITE;
        } else if (latency < 1000) {
            sprite = PING_2_SPRITE;
        } else {
            sprite = PING_1_SPRITE;
        }

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(0.0F, 0.0F, 100.0F);
        context.drawGuiTexture(sprite, x, y, width, height);
        matrices.pop();
    }

    private void drawScaledText(DrawContext context, TextRenderer textRenderer, String text,
                                int x, int y, int maxHeight, int color) {
        int textHeight = 8;
        float scaleY = (float) maxHeight / textHeight;
        float scale = scaleY;

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.scale(scale, scale, 1.0f);
        matrices.translate(x / scale, y / scale, 0);

        textRenderer.draw(text, 0, 0, color, true, matrices.peek().getPositionMatrix(),
                context.getVertexConsumers(), TextRenderer.TextLayerType.NORMAL, 0, 15728880);

        matrices.pop();
    }

    private static Map<String, PlayerEntity> buildPlayerMap() {
        Map<String, PlayerEntity> map = new HashMap<>();
        ClientWorld world = SoftHardcoreClient.getClient().world;
        if (world != null) {
            for (AbstractClientPlayerEntity player : world.getPlayers()) {
                map.put(player.getName().getString(), player);
            }
        }
        return map;
    }

    private static void drawCenteredText(DrawContext context, TextRenderer textRenderer, String text,
                                         int x, int y, int color) {
        int textWidth = textRenderer.getWidth(text);
        textRenderer.draw(text, x - textWidth / 2, y, color, true, context.getMatrices().peek().getPositionMatrix(),
                context.getVertexConsumers(), TextRenderer.TextLayerType.NORMAL, 0, 15728880);
    }

    private static String getServerName() {
        MinecraftClient client = SoftHardcoreClient.getClient();
        if (client != null && client.getCurrentServerEntry() != null) {
            return client.getCurrentServerEntry().name;
        } else if (client != null && client.world != null) {
            return "Singleplayer World";
        } else {
            return "THE REALM";
        }
    }

    private static int playerEntryWidth(ScalingSystem.ContainerDimensions parent) {
        return ScalingSystem.widthPercent(parent, 0.28f);
    }

    private static int playerEntryHeight(ScalingSystem.ContainerDimensions parent) {
        return ScalingSystem.heightPercent(parent, 0.038f);
    }

    private static int playerEntryGapX(ScalingSystem.ContainerDimensions parent) {
        return ScalingSystem.widthPercent(parent, 0.01f);
    }

    private static int playerEntryGapY(ScalingSystem.ContainerDimensions parent) {
        return ScalingSystem.heightPercent(parent, 0.01f);
    }

    private static int hudBackgroundBottomMarginY(ScalingSystem.ContainerDimensions parent) {
        return ScalingSystem.heightPercent(parent, 0.06f);
    }

    private static int textUpperMarginY(ScalingSystem.ContainerDimensions parent) {
        return ScalingSystem.heightPercent(parent, 0.04f);
    }

    private static int textLowerMarginY(ScalingSystem.ContainerDimensions parent) {
        return ScalingSystem.heightPercent(parent, 0.10f);
    }

    private static int hudMarginTopY(ScalingSystem.ContainerDimensions parent) {
        return ScalingSystem.heightPercent(parent, 0.02f);
    }

    private static int hudSideMargins(ScalingSystem.ContainerDimensions parent) {
        return ScalingSystem.widthPercent(parent, 0.02f);
    }

    private static int nameTagMargin(ScalingSystem.ContainerDimensions parent) {
        return ScalingSystem.widthPercent(parent, 0.035f);
    }

    private static int iconMargin(ScalingSystem.ContainerDimensions parent) {
        return ScalingSystem.widthPercent(parent, 0.0125f);
    }

    private static int iconDimensions(ScalingSystem.ContainerDimensions parent) {
        return ScalingSystem.heightPercent(parent, 1);
    }

    private static int pingHeight(ScalingSystem.ContainerDimensions parent) {
        return ScalingSystem.heightPercent(parent, 1.0f);
    }

    private static int pingWidth(ScalingSystem.ContainerDimensions parent) {
        return ScalingSystem.heightPercent(parent, 1.25f);
    }

    private static int pingMargin(ScalingSystem.ContainerDimensions parent) {
        return ScalingSystem.widthPercent(parent, 0.01f);
    }

    private static int livesMargin(ScalingSystem.ContainerDimensions parent) {
        return ScalingSystem.widthPercent(parent, 0.010f);
    }
}
