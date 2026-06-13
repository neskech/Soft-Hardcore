package net.ness.softhardcore.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
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

@Mixin(PlayerTabOverlay.class)
public abstract class PlayersListMixin {

    private static final int BACKGROUND_COLOR = 0x60000000;
    private static final int PLAYER_ENTRY_BACKGROUND_COLOR = 0x4aaaaaaa;

    private static final ResourceLocation TEAM_ICON = ResourceLocation.fromNamespaceAndPath(SoftHardcore.MOD_ID, "textures/team_icon.png");
    private static final ResourceLocation PING_UNKNOWN_SPRITE = ResourceLocation.withDefaultNamespace("icon/ping_unknown");
    private static final ResourceLocation PING_1_SPRITE = ResourceLocation.withDefaultNamespace("icon/ping_1");
    private static final ResourceLocation PING_2_SPRITE = ResourceLocation.withDefaultNamespace("icon/ping_2");
    private static final ResourceLocation PING_3_SPRITE = ResourceLocation.withDefaultNamespace("icon/ping_3");
    private static final ResourceLocation PING_4_SPRITE = ResourceLocation.withDefaultNamespace("icon/ping_4");
    private static final ResourceLocation PING_5_SPRITE = ResourceLocation.withDefaultNamespace("icon/ping_5");

    private static final Vector2i TEAM_ICON_TEX_DIMENSIONS = new Vector2i(8, 8);

    @Shadow @Final
    private static Comparator<PlayerInfo> PLAYER_COMPARATOR;

    @Accessor("minecraft")
    abstract Minecraft client();

    private List<PlayerInfo> collectPlayerEntries() {
        return this.client().player.connection.getListedOnlinePlayers().stream()
                .sorted(PLAYER_COMPARATOR)
                .limit(80L)
                .toList();
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void renderPlayersList(@NotNull GuiGraphics graphics, int scaledWindowWidth, Scoreboard scoreboard,
                                  @Nullable Objective objective, CallbackInfo ci) {
        List<PlayerInfo> playerEntries = collectPlayerEntries();
        int numPlayers = playerEntries.size();

        if (numPlayers == 0) return;

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
        graphics.fill(leftX, topY, rightX, bottomY, BACKGROUND_COLOR);

        var font = this.client().font;
        String title = "§l" + getServerName();
        int healthColor = 0xDC143C;
        drawCenteredText(graphics, font, title, screenContainer.width / 2, topY + textUpperMarginY(screenContainer), healthColor);

        String count = numPlayers == 1 ? numPlayers + " Player" : numPlayers + " Players";
        drawCenteredText(graphics, font, count, screenContainer.width / 2, topY + textUpperMarginY(screenContainer) + 10, 0xFFFFFF);

        Map<String, Player> playerMap = buildPlayerMap();

        for (int col = 0; col < numColumns; col++) {
            for (int row = 0; row < MyConfig.MAX_SCOREBOARD_ROWS; row++) {
                int i = col * MyConfig.MAX_SCOREBOARD_ROWS + row;
                if (i >= numPlayers) break;
                int x = entryX + (entryWidth + gapX) * col;
                int y = entryY + (entryHeight + gapY) * row;
                renderPlayerEntry(graphics, x, y, playerEntries.get(i), playerMap, scoreboard, entryWidth, entryHeight);
            }
        }

        graphics.flush();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        ci.cancel();
    }

    private void renderPlayerEntry(GuiGraphics graphics, int x, int y, PlayerInfo entry, Map<String, Player> map,
                                   Scoreboard scoreboard, int entryWidth, int entryHeight) {
        graphics.fill(x, y, x + entryWidth, y + entryHeight, PLAYER_ENTRY_BACKGROUND_COLOR);
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

        PlayerFaceRenderer.draw(graphics, entry.getSkin(), x, y, iconSize);

        x += iconSize + iconMarginSize;
        PlayerTeam team = scoreboard.getPlayerTeam(profile.getName());
        int color = 0xFFFFFF;
        if (team != null && team.getColor() != null) {
            Integer colorValue = team.getColor().getColor();
            if (colorValue != null) {
                color = colorValue;
            }
        }

        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        graphics.setColor(r, g, b, 1.0f);
        graphics.blit(
                TEAM_ICON,
                x,
                y,
                iconSize,
                iconSize,
                0,
                0,
                TEAM_ICON_TEX_DIMENSIONS.x,
                TEAM_ICON_TEX_DIMENSIONS.y,
                TEAM_ICON_TEX_DIMENSIONS.x,
                TEAM_ICON_TEX_DIMENSIONS.y
        );
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        x += iconSize + nameTagMarginSize;

        drawScaledText(graphics, this.client().font, profile.getName(), x, y, entryHeight, color);

        int rightX = leftX + entryWidth - pingWidthSize - pingMarginSize;
        drawPingIcon(graphics, rightX, y, pingWidthSize, pingHeightSize, entry.getLatency());

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
        float realTextWidth = this.client().font.width(livesText) * scaleY;
        rightX -= (int) realTextWidth + livesMarginSize;

        drawScaledText(graphics, this.client().font, livesText, rightX, y, entryHeight, healthColor);
    }

    private static void drawPingIcon(GuiGraphics graphics, int x, int y, int width, int height, int latency) {
        ResourceLocation sprite;
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

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 100.0F);
        graphics.blitSprite(sprite, x, y, width, height);
        poseStack.popPose();
    }

    private void drawScaledText(GuiGraphics graphics, net.minecraft.client.gui.Font font, String text,
                                int x, int y, int maxHeight, int color) {
        int textHeight = 8;
        float scaleY = (float) maxHeight / textHeight;
        float scale = scaleY;

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.scale(scale, scale, 1.0f);
        poseStack.translate(x / scale, y / scale, 0);

        font.drawInBatch(text, 0, 0, color, true, poseStack.last().pose(), graphics.bufferSource(),
                net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, 15728880);

        poseStack.popPose();
    }

    private static Map<String, Player> buildPlayerMap() {
        Map<String, Player> map = new HashMap<>();
        ClientLevel level = SoftHardcoreClient.getClient().level;
        if (level != null) {
            for (AbstractClientPlayer p : level.players()) {
                map.put(p.getName().getString(), p);
            }
        }
        return map;
    }

    private static void drawCenteredText(GuiGraphics graphics, net.minecraft.client.gui.Font font, String text,
                                         int x, int y, int color) {
        int textWidth = font.width(text);
        font.drawInBatch(text, x - textWidth / 2, y, color, true, graphics.pose().last().pose(), graphics.bufferSource(),
                net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, 15728880);
    }

    private static String getServerName() {
        Minecraft client = SoftHardcoreClient.getClient();
        if (client != null && client.getCurrentServer() != null) {
            return client.getCurrentServer().name;
        } else if (client != null && client.level != null) {
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
