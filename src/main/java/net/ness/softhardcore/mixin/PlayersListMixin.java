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
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.ness.softhardcore.SoftHardcore;
import net.ness.softhardcore.SoftHardcoreClient;
import net.ness.softhardcore.component.LivesComponent;
import net.ness.softhardcore.component.MyComponents;
import net.ness.softhardcore.config.MyConfig;
import net.ness.softhardcore.ui.ScalingSystem;
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

import java.util.*;

@Mixin(PlayerListHud.class)
public abstract class PlayersListMixin {

    private static final int MAX_ROWS = 5;
    
    private static final int BACKGROUND_COLOR = 0x80000000;      // 50% black
    private static final int PLAYER_ENTRY_BACKGROUND_COLOR = 0x40FFFFFF; // 25% white tint overlay
    
    private static final Identifier TEAM_ICON = new Identifier(SoftHardcore.MOD_ID, "textures/team_icon.png");
    private static final Identifier HEART_ICON = new Identifier(SoftHardcore.MOD_ID, "textures/heart_icon.png");
    private static final Identifier ICONS_TEXTURE = new Identifier("textures/gui/icons.png");

    private static final Vector2i TEAM_ICON_TEX_DIMENSIONS = new Vector2i(8, 8);
    private static final Vector2i HEART_ICON_TEX_DIMENSIONS = new Vector2i(16, 16);

    @Shadow @Final
    private static Comparator<PlayerListEntry> ENTRY_ORDERING;

    @Accessor("client") abstract MinecraftClient client();

    // Scaling methods that take parent container dimensions
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
    
    private static int hudBackgroundTopMarginY(ScalingSystem.ContainerDimensions parent) { 
        return ScalingSystem.heightPercent(parent, 0.06f); 
    }
    
    private static int hudBackgroundBottomMarginY(ScalingSystem.ContainerDimensions parent) { 
        return ScalingSystem.heightPercent(parent, 0.06f); 
    }
    
    private static int hudMarginTopY(ScalingSystem.ContainerDimensions parent) { 
        return ScalingSystem.heightPercent(parent, 0.01f); 
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
    

    private List<PlayerListEntry> collectPlayerEntries() {
        return this.client().player.networkHandler.getListedPlayerListEntries().stream()
                .sorted(ENTRY_ORDERING)
                .limit(80L)
                .toList();
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void renderPlayersList(@NotNull DrawContext context, int scaledWindowWidth, Scoreboard scoreboard, @Nullable ScoreboardObjective objective, CallbackInfo ci) {
        List<PlayerListEntry> playerEntries = collectPlayerEntries();
        int numPlayers = playerEntries.size();

        if (numPlayers == 0) return;

        // Create screen container dimensions
        ScalingSystem.ContainerDimensions screenContainer = ScalingSystem.ContainerDimensions.screen();

        int numColumns = (int) Math.ceil((float) numPlayers / MAX_ROWS);
        int numRows = Math.min(numPlayers, MAX_ROWS);
        
        // Calculate the bounding width and height of the player entries using screen dimensions
        int entryWidth = playerEntryWidth(screenContainer);
        int entryHeight = playerEntryHeight(screenContainer);
        int gapX = playerEntryGapX(screenContainer);
        int gapY = playerEntryGapY(screenContainer);
        
        int innerWidth = numColumns * entryWidth + (numColumns - 1) * gapX;
        int innerHeight = numRows * entryHeight + (numRows - 1) * gapY;

        int centerX = scaledWindowWidth / 2;
        int entryX = centerX - innerWidth / 2;
        int topMarginY = hudMarginTopY(screenContainer);
        int topBackgroundMarginY = hudBackgroundTopMarginY(screenContainer);
        int bottomBackgroundMarginY = hudBackgroundBottomMarginY(screenContainer);
        int entryY = topMarginY + topBackgroundMarginY;

        int leftX = entryX;
        int rightX = entryX + numColumns * entryWidth + (numColumns - 1) * gapX;
        int topY = topMarginY;
        int bottomY = topY + topBackgroundMarginY + innerHeight + bottomBackgroundMarginY;

        RenderSystem.enableBlend();
        context.fill(leftX, topY, rightX, bottomY, BACKGROUND_COLOR);

        // TextRenderer tr = this.client().textRenderer;
        // String title = "THE REALM";
        // drawCenteredText(context, tr, title, screenContainer.width / 2, topY + 10, 0xFF0000);

        // String count = numPlayers + " Players";
        // drawCenteredText(context, tr, count, screenContainer.width / 2, topY + 25, Colors.WHITE);

        Map<String, PlayerEntity> playerMap = buildPlayerMap();

        for (int col = 0; col < numColumns; col++) {
            for (int row = 0; row < MAX_ROWS; row++) {
                int i = col * MAX_ROWS + row;
                if (i >= numPlayers) break;
                int x = entryX + (entryWidth + gapX) * col;
                int y = entryY + (entryHeight + gapY) * row;
                renderPlayerEntry(context, x, y, playerEntries.get(i), playerMap, scoreboard, entryWidth, entryHeight);
            }
        }
        RenderSystem.setShaderColor(1,1,1,1);
        ci.cancel();
    }

    private void renderPlayerEntry(DrawContext ctx, int x, int y, PlayerListEntry entry, Map<String, PlayerEntity> map, Scoreboard scoreboard, int entryWidth, int entryHeight) {
        ctx.fill(x, y, x + entryWidth, y + entryHeight, PLAYER_ENTRY_BACKGROUND_COLOR);
        GameProfile profile = entry.getProfile();

        int leftX = x;

        // Create container dimensions for this player entry
        ScalingSystem.ContainerDimensions entryContainer = ScalingSystem.ContainerDimensions.of(entryWidth, entryHeight);
        
        int iconSize = iconDimensions(entryContainer);
        int iconMarginSize = iconMargin(entryContainer);
        int nameTagMarginSize = nameTagMargin(entryContainer);
        int pingMarginSize = pingMargin(entryContainer);
        int livesMarginSize = livesMargin(entryContainer);
        int pingHeightSize = pingHeight(entryContainer);
        int pingWidthSize = pingWidth(entryContainer);

        ClientWorld world = this.client().world;
        if (world != null) {
            PlayerEntity player = world.getPlayerByUuid(profile.getId());
            if (player != null) {
                PlayerSkinDrawer.draw(ctx, entry.getSkinTexture(), x, y, iconSize, false, false);
            }
        }

        x += iconSize + iconMarginSize;
        Team team = scoreboard.getPlayerTeam(profile.getName());
        int color = (team != null && team.getColor() != null) ? team.getColor().getColorValue() : 0xFFFFFF;
        
        setShaderColor(color);
        ctx.drawTexture(TEAM_ICON, x, y, iconSize, iconSize, 0, 0, TEAM_ICON_TEX_DIMENSIONS.x, TEAM_ICON_TEX_DIMENSIONS.y, TEAM_ICON_TEX_DIMENSIONS.x, TEAM_ICON_TEX_DIMENSIONS.y);
        x += iconSize + nameTagMarginSize;
        RenderSystem.setShaderColor(1,1,1,1);

        drawScaledText(ctx, this.client().textRenderer, profile.getName(), x, y, entryHeight, color);
        
        // Since we don't know the width of the text, we need to calculate the rightX
        int rightX = leftX + entryWidth - pingWidthSize - pingMarginSize;
        drawPingBars(ctx, rightX, y, pingWidthSize, pingHeightSize, entry.getLatency());

        String lives = "?";
        PlayerEntity p = map.get(profile.getName());
        if (p != null) {
            LivesComponent comp = MyComponents.LIVES_KEY.get(p);
            if (comp != null) {
                int l = comp.getLives();
                lives = String.valueOf(l);
            }
        }

        int healthColor = 0xDC143C; // Crimson red
        String heartSymbol = "\u2764";
        String livesText = lives + heartSymbol;
        
        // Calculate the width of the text
        float scaleY = (float) entryHeight / 8;
        float realTextWidth = this.client().textRenderer.getWidth(livesText) * scaleY;
        rightX -= realTextWidth + livesMarginSize;
        
        drawScaledText(ctx, this.client().textRenderer, livesText, rightX, y, entryHeight, healthColor);
    }

    private static void drawPingBars(DrawContext ctx, int x, int y, int width, int height, int latency) {
        // Use a modern, more compact style with a descriptive variable name.
        int pingBarIndex = latency < 0 ? 5 :
                           latency < 150 ? 0 :
                           latency < 300 ? 1 :
                           latency < 600 ? 2 :
                           latency < 1000 ? 3 : 4;

		ctx.getMatrices().push();
        float scaleX = (float) width / 10;
        float scaleY = (float) height / 8;
        ctx.getMatrices().scale(scaleX, scaleY, 1.0f);
		ctx.getMatrices().translate(x / scaleX, y / scaleY, 100.0F);
        ctx.drawTexture(ICONS_TEXTURE, 0, 0, 0, 176 + pingBarIndex * 8, 10, 8);
		ctx.getMatrices().pop();
    }

    private void drawScaledText(DrawContext context, TextRenderer renderer, String text, 
                            int x, int y, int maxHeight, int color) {

        int textHeight = 8; // Vanilla text height
        float scaleY = (float) maxHeight / textHeight;
        float scale = scaleY; // Keep proportions
        
        context.getMatrices().push();
        context.getMatrices().scale(scale, scale, 1.0f);
        context.getMatrices().translate(x / scale, y / scale, 0);
        
        renderer.draw(text, 0, 0, color, true, context.getMatrices().peek().getPositionMatrix(), 
                    context.getVertexConsumers(), TextRenderer.TextLayerType.NORMAL, 0, 15728880);
        
        context.getMatrices().pop();
    }


    private static Map<String, PlayerEntity> buildPlayerMap() {
        Map<String, PlayerEntity> map = new HashMap<>();
        ClientWorld w = SoftHardcoreClient.getClient().world;
        if (w != null) {
            for (AbstractClientPlayerEntity p : w.getPlayers()) map.put(p.getName().getString(), p);
        }
        return map;
    }

    private static void setShaderColor(int color) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        RenderSystem.setShaderColor(r, g, b, 1.0f);
    }
}
