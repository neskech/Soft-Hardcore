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
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import net.ness.softhardcore.SoftHardcore;
import net.ness.softhardcore.SoftHardcoreClient;
import net.ness.softhardcore.component.LivesComponent;
import net.ness.softhardcore.component.MyComponents;
import net.ness.softhardcore.config.MyConfig;
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
    ////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////* GUI CONSTANTS AND HELPERS *//////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////
    private final static int MAX_ROWS = 5;

    private static int getWidth() {
        return SoftHardcoreClient.getClient().getWindow().getScaledWidth();
    }

    private static int getHeight() {
        return SoftHardcoreClient.getClient().getWindow().getScaledHeight();
    }

    ////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////* GUI DIMENSIONS */////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////

    private static int playerEntryWidth() {
        return (int)(0.28f * getWidth()); // Increased from 0.22f to prevent spillover
    }

    private static int playerEntryHeight() {
        return (int)(0.025f * getHeight());
    }

    private static int playerEntryGapX() {
        return (int)(0.01f * getWidth());
    }

    private static int playerEntryGapY() {
        return (int)(0.01f * getHeight());
    }

    private static int hudBackgroundMarginX() {
        return (int)(0.00f * getWidth());
    }

    private static int hudBackgroundTopMarginY() {
        return (int)(0.06f * getHeight());
    }

    private static int hudBackgroundBottomMarginY() {
        return (int)(0.06f * getHeight());
    }

    private static int hudMarginTopY() {
        return (int)(0.01f * getHeight());
    }

    private static int nameTagMargin() {
        return (int)(0.02f * playerEntryWidth()); // Increased gap between team icon and name
    }

    private static int iconMargin() {
        return (int)(0.0125f * playerEntryWidth());
    }

    private static int iconDimensions() {
        return playerEntryHeight();
    }

    ////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////* TEXTURES *///////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////

    private static final Identifier TEAM_ICON = new Identifier(SoftHardcore.MOD_ID, "textures/team_icon.png");
    private static final Identifier HEART_ICON = new Identifier(SoftHardcore.MOD_ID, "textures/heart_icon.png");

    ////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////* TEXTURE DIMENSIONS */////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////

    private static final Vector2i TEAM_ICON_TEX_DIMENSIONS = new Vector2i(8, 8);
    private static final Vector2i HEART_ICON_TEX_DIMENSIONS = new Vector2i(16, 16);

    ////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////* COLORS */////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////
    ///
    private final static int BACKGROUND_COLOR = 0x80000000; // Dark semi-transparent background
    private final static int PLAYER_ENTRY_BACKGROUND_COLOR = 0x60000000; // Lighter semi-transparent background for player entries

    @Shadow
    @Final
    private static Comparator<PlayerListEntry> ENTRY_ORDERING;

    @Accessor("client")
    abstract MinecraftClient client();

    @Accessor("header")
    abstract Text header();



    List<PlayerListEntry> collectPlayerEntries() {
        return this.client().player.networkHandler.getListedPlayerListEntries().stream().sorted(ENTRY_ORDERING).limit(80L).toList();
    }

    public Text getPlayerName(PlayerListEntry entry) {
        return entry.getDisplayName() != null
                ? this.applyGameModeFormatting(entry, entry.getDisplayName().copy())
                : this.applyGameModeFormatting(entry, Team.decorateName(entry.getScoreboardTeam(), Text.literal(entry.getProfile().getName())));
    }


    private Text applyGameModeFormatting(PlayerListEntry entry, MutableText name) {
        return entry.getGameMode() == GameMode.SPECTATOR ? name.formatted(Formatting.ITALIC) : name;
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void renderPlayersList(@NotNull DrawContext context, int scaledWindowWidth, Scoreboard scoreboard, @Nullable ScoreboardObjective objective, CallbackInfo ci) {
        // Retrieve player entries
        List<PlayerListEntry> playerEntries = this.collectPlayerEntries();
        int numPlayers = playerEntries.size();

        // Calculate the dimensions of the inner bounding box
        int numRows = Math.min(numPlayers, MAX_ROWS);
        int numColumns = Math.max(1, numPlayers / MAX_ROWS);
        int innerBoundingWidth = numColumns * playerEntryWidth() + (numColumns - 1) * playerEntryGapX();
        int innerBoundingHeight = numRows * playerEntryHeight() + (numRows - 1) * playerEntryGapY();

        // Calculate the (X, Y) of the first player entry
        int centerX = scaledWindowWidth / 2;
        int entryX = centerX - innerBoundingWidth / 2;
        int entryY = hudMarginTopY() + hudBackgroundTopMarginY();

        // Draw the background behind the inner bounding box
        int leftX = entryX - hudBackgroundMarginX();
        int rightX = centerX + innerBoundingWidth / 2 + hudBackgroundMarginX();
        int topY = hudMarginTopY();
        int bottomY = topY + hudBackgroundTopMarginY() + innerBoundingHeight + hudBackgroundBottomMarginY();
        RenderSystem.enableBlend();
        context.fill(leftX, topY, rightX, bottomY, BACKGROUND_COLOR);

        // Draw the title
        String title = "THE REALM";
        TextRenderer textRenderer = this.client().textRenderer;
        int titleWidth = textRenderer.getWidth(title);
        int titleX = (getWidth() - titleWidth) / 2;
        int titleY = topY + 10;
        drawText(context, textRenderer, title, titleX, titleY, 20, 0xFF0000); // Red title

        // Draw the player count
        String playerCount = numPlayers + " Players";
        int countWidth = textRenderer.getWidth(playerCount);
        int countX = (getWidth() - countWidth) / 2;
        int countY = titleY + 20;
        drawText(context, textRenderer, playerCount, countX, countY, 20, Colors.WHITE);

        // Draw the player entries
        HashMap<String, PlayerEntity> name2Player = getStringToPlayerMap();
        for (int column = 0; column < numColumns; column++) {
            numRows = Math.min(numPlayers - column * MAX_ROWS, MAX_ROWS);
            for (int row = 0; row < numRows; row++) {
                int x = entryX + (playerEntryWidth() + playerEntryGapX()) * column;
                int y = entryY + (playerEntryHeight() + playerEntryGapY()) * row;
                int i = column * MAX_ROWS + row;
                renderPlayerListEntry(context, x, y, playerEntries.get(i), name2Player, scoreboard);
            }
        }

        ci.cancel();
    }

    private void renderPlayerListEntry(DrawContext context, int x, int y, PlayerListEntry entry, HashMap<String, PlayerEntity> name2Player, Scoreboard scoreboard) {
        // Draw the background with semi-transparent black overlay
        context.fill(x, y, x + playerEntryWidth(), y + playerEntryHeight(), PLAYER_ENTRY_BACKGROUND_COLOR);

        int originalX = x;

        // Draw the player skin icon
        GameProfile gameProfile = entry.getProfile();
        ClientWorld world = this.client().world;
        assert world != null;
        PlayerEntity playerEntity = world.getPlayerByUuid(gameProfile.getId());
        if (playerEntity != null) {
            boolean hatVisible = LivingEntityRenderer.shouldFlipUpsideDown(playerEntity);
            boolean upsideDown = playerEntity.isPartVisible(PlayerModelPart.HAT);
            PlayerSkinDrawer.draw(context, entry.getSkinTexture(), x, y, iconDimensions(), upsideDown, hatVisible);
        }

        // Draw the team icon
        Team team = scoreboard.getPlayerTeam(gameProfile.getName());
        Formatting teamColor = team == null ? Formatting.RED : team.getColor();
        x += iconDimensions() + iconMargin();
        context.setShaderColor(     ((teamColor.getColorValue() >> 16) & 0xFF) / 255.0f, // Red
                ((teamColor.getColorValue() >> 8) & 0xFF) / 255.0f,  // Green
                (teamColor.getColorValue() & 0xFF) / 255.0f,         // Blue
                ((teamColor.getColorValue() >> 24) & 0xFF) / 255.0f );
        context.drawTexture(TEAM_ICON, x, y, iconDimensions(), iconDimensions(), 0, 0, TEAM_ICON_TEX_DIMENSIONS.x, TEAM_ICON_TEX_DIMENSIONS.y, TEAM_ICON_TEX_DIMENSIONS.x, TEAM_ICON_TEX_DIMENSIONS.y);

        // Draw the name tag
        TextRenderer textRenderer = this.client().textRenderer;
        String playerName = gameProfile.getName();
        x += iconDimensions() + nameTagMargin();

        context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        drawText(context, textRenderer, playerName, x, y, playerEntryHeight(), teamColor.getColorValue());
        //  context.drawText(textRenderer, playerName, x, y, teamColor.getColorValue(), true);

        // Now let's draw elements from right to left, with proper spacing
        x = originalX + playerEntryWidth() - iconMargin();

        // Draw the ping icon (Minecraft-style bars)
        x -= iconDimensions();
        int latency = entry.getLatency();
        int pingColor = 0x00FF00; // Green
        if (latency < 150) pingColor = 0x00FF00; // Green
        else if (latency < 300) pingColor = 0xFFFF00; // Yellow
        else pingColor = 0xFF0000; // Red
        
        // Draw 5 bars (Minecraft's standard ping display)
        int barWidth = 2;
        int barHeight = 4;
        int barSpacing = 1;
        int totalWidth = 5 * barWidth + 4 * barSpacing;
        int startX = x + (iconDimensions() - totalWidth) / 2;
        int startY = y + (iconDimensions() - barHeight) / 2;
        
        for (int i = 0; i < 5; i++) {
            int barX = startX + i * (barWidth + barSpacing);
            int currentBarHeight = barHeight;
            
            // Determine bar height based on latency
            if (latency > 150 && i < 2) currentBarHeight = barHeight / 2;
            else if (latency > 300 && i < 3) currentBarHeight = barHeight / 2;
            else if (latency > 600 && i < 4) currentBarHeight = barHeight / 2;
            else if (latency > 1000 && i < 5) currentBarHeight = barHeight / 2;
            
            context.fill(barX, startY + barHeight - currentBarHeight, barX + barWidth, startY + barHeight, pingColor);
        }

        // Draw the lives icon and number
        x -= iconDimensions() + 4; // Move left by icon width + gap
        context.drawTexture(HEART_ICON, x, y, iconDimensions(), iconDimensions(), 0, 0, HEART_ICON_TEX_DIMENSIONS.x, HEART_ICON_TEX_DIMENSIONS.y, HEART_ICON_TEX_DIMENSIONS.x, HEART_ICON_TEX_DIMENSIONS.y);

        // Get lives with proper fallback
        String numLives = "?";
        int livesColor = Colors.WHITE;
        
        if (name2Player.containsKey(playerName)) {
            PlayerEntity player = name2Player.get(playerName);
            LivesComponent component = MyComponents.LIVES_KEY.get(player);
            if (component != null) {
                int lives = component.getLives();
                numLives = "" + lives;
                
                // Color coding for lives
                if (lives >= MyConfig.DEFAULT_LIVES) {
                    livesColor = 0x00FF00; // Green - Full lives
                } else if (lives >= MyConfig.DEFAULT_LIVES * 0.5) {
                    livesColor = 0xFFFF00; // Yellow - 50-99% of max
                } else if (lives > 1) {
                    livesColor = 0xFF8800; // Orange - 1-49% of max
                } else {
                    livesColor = 0xFF0000; // Red - 1 life
                }
            }
        }

        // Draw lives number to the left of the heart icon
        int text_width = textRenderer.getWidth(numLives);
        x -= text_width + 2; // Small gap between number and heart
        drawText(context, textRenderer, numLives, x, y, playerEntryHeight(), livesColor);

        //context.fill(cent);
    }

    private void drawText(DrawContext context, TextRenderer renderer, String text, int x, int y, int height, int color) {
        //float scaleFactor = (float) height / 8;
        float scaleFactor = (0.025f) / (8.0f / getHeight());
        context.getMatrices().push();
        context.getMatrices().scale(scaleFactor, scaleFactor, scaleFactor);
        context.drawText(renderer, text, (int)(x / scaleFactor), (int)(y / scaleFactor), color, true);
       // context.drawText(renderer, text, x, y, color, true);
        context.getMatrices().pop();
    }


    private static HashMap<String, PlayerEntity> getStringToPlayerMap() {
        HashMap<String, PlayerEntity> map = new HashMap<>();
        List<AbstractClientPlayerEntity> players = SoftHardcoreClient.getClient().world.getPlayers();
        for (PlayerEntity p : players) {
            map.put(p.getName().getString(), p);
        }
        return map;
    }

//    // Modify the method that renders the player name tag
//    @Inject(method = "render", at = @At("HEAD"))
//    private void renderNameTagWithTexture(DrawContext context, int scaledWindowWidth, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
////        PlayerListHud THIS = (PlayerListHud) (Object) this;
//        List<PlayerListEntry> list = this.collectPlayerEntries();
//        int i = 0;
//        int j = 0;
//
//        for (PlayerListEntry playerListEntry : list) {
//            int k = this.client().textRenderer.getWidth(this.getPlayerName(playerListEntry));
//            i = Math.max(i, k);
//            if (objective != null && objective.getRenderType() != ScoreboardCriterion.RenderType.HEARTS) {
//                k = this.client().textRenderer.getWidth(" " + scoreboard.getPlayerScore(playerListEntry.getProfile().getName(), objective).getScore());
//                j = Math.max(j, k);
//            }
//        }
//
//
//        int l = list.size();
//        int m = l;
//
//        int k;
//        for (k = 1; m > 20; m = (l + k - 1) / k) {
//            k++;
//        }
//
//        boolean bl = this.client().isInSingleplayer() || this.client().getNetworkHandler().getConnection().isEncrypted();
//        int n;
//        if (objective != null) {
//            if (objective.getRenderType() == ScoreboardCriterion.RenderType.HEARTS) {
//                n = 90;
//            } else {
//                n = j;
//            }
//        } else {
//            n = 0;
//        }
//
//        int o = Math.min(k * ((bl ? 9 : 0) + i + n + 13), scaledWindowWidth - 50) / k;
//        int p = scaledWindowWidth / 2 - (o * k + (k - 1) * 5) / 2;
//        int q = 10;
//        List<OrderedText> list2 = null;
//        if (this.header() != null) {
//            list2 = this.client().textRenderer.wrapLines(this.header(), scaledWindowWidth - 50);
//
//
//        }
//
//
//        if (list2 != null) {
//
//            for (OrderedText orderedText2 : list2) {
//                q += 9;
//            }
//
//            q++;
//        }
//
//
//        Identifier HEART_TEXTURE = new Identifier(SoftHardcore.MOD_ID, "textures/life_heart.png");
//        for (int u = 0; u < l; u++) {
//            int s = u / m;
//            int v = u % m;
//            int w = p + s * o + s * 5;
//            int x = q + v * 9;
//            RenderSystem.enableBlend();
//            // context.drawText(this.client().textRenderer, "Hello, world! " + lives, 10, 50, 0xFFFFFFFF, true);
//            context.drawTexture(HEART_TEXTURE, w - 10, q - 1, 10, 1 + m * 9, 0, 0, 6000, 5300, 6000, 5300);
//            context.fill(w - 20, q - 1, w - 2, q + m * 9, Integer.MIN_VALUE);
//        }
//    }


    //PlayerEntity player = playerListEntry2.
    //  LivesComponent component = MyComponents.LIVES_KEY.get(this.client.player);
    // int lives = component.getLives();

    //  context.fill(w-10, x, w - 2, x, 0xFFFFFFFF);
    //  context.drawTexture(HEART_TEXTURE, 10, 10, 0, 0, 40, 40, 308, 106);
//        // context.drawText(client.textRenderer, "Hello, world! " + lives, 10, 50, 0xFFFFFFFF, true);
//        for(int u = 0; u < l; ++u) {
//            int s = u / m;
//            v = u % m;
//            int w = p + s * o + s * 5;
//            int x = q + v * 9;
//            context.fill(w-10, x, w - 2, x, 0xFFFFFFFF);
////            context.fill(w, x, w + o, x + 8, t);
////            RenderSystem.enableBlend();
////            if (u < list.size()) {
////                PlayerListEntry playerListEntry2 = (PlayerListEntry)list.get(u);
////                GameProfile gameProfile = playerListEntry2.getProfile();
////                if (bl) {
////                    PlayerEntity playerEntity = this.client.world.getPlayerByUuid(gameProfile.getId());
////                    boolean bl2 = playerEntity != null && LivingEntityRenderer.shouldFlipUpsideDown(playerEntity);
////                    boolean bl3 = playerEntity != null && playerEntity.isPartVisible(PlayerModelPart.HAT);
////                    PlayerSkinDrawer.draw(context, playerListEntry2.getSkinTexture(), w, x, 8, bl3, bl2);
////                    w += 9;
////                }
////
////                context.drawTextWithShadow(this.client.textRenderer, this.getPlayerName(playerListEntry2), w, x, playerListEntry2.getGameMode() == GameMode.SPECTATOR ? -1862270977 : -1);
////                if (objective != null && playerListEntry2.getGameMode() != GameMode.SPECTATOR) {
////                    int y = w + i + 1;
////                    int z = y + n;
////                    if (z - y > 5) {
////                        this.renderScoreboardObjective(objective, x, gameProfile.getName(), y, z, gameProfile.getId(), context);
////                    }
////                }
////
////                this.renderLatencyIcon(context, o, w - (bl ? 9 : 0), x, playerListEntry2);
//        }

}
