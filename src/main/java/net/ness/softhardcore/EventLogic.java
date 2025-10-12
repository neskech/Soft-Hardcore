package net.ness.softhardcore;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.ness.softhardcore.component.LivesComponent;
import net.ness.softhardcore.component.MyComponents;
import net.ness.softhardcore.config.HeartDropMode;
import net.ness.softhardcore.config.MyConfig;
import net.ness.softhardcore.event.PlayerDeathCallback;
import net.ness.softhardcore.util.LivesCacheManager;

import java.time.Instant;
import java.util.Date;

public class EventLogic {

    public static void registerEventLogic() {
        //TODO: Change to after respawn and maybe put a hud message too
        PlayerDeathCallback.EVENT.register((player, damageSource) -> lifeDecrementLogic(player, damageSource));
        PlayerDeathCallback.EVENT.register((player, damageSource) -> lifeDropLogic(player, damageSource));
        ServerPlayConnectionEvents.JOIN.register(EventLogic::loginEventLogic);
    }

    private static ActionResult lifeDecrementLogic(ServerPlayerEntity player, DamageSource damageSource) {
        LivesComponent component = MyComponents.LIVES_KEY.get(player);
        component.decrement();
        component.setLastLifeLostTime(System.currentTimeMillis());
        
        // Update the lives cache for the scoreboard
        LivesCacheManager.updateLivesCache(player.getUuid(), component.getLives());

        // Only ban if the player's run out of lives
        if (component.getLives() > 0) {
            Text message = Text.literal("You've just lost a life...").formatted(Formatting.RED);
            player.sendMessage(message);
            return ActionResult.PASS;
        }


        MinecraftServer server = player.getServer();
        if (server == null) {
            Text message = Text.literal("ERROR: Player ran out of lives, but server is null").formatted(Formatting.RED);
            player.sendMessage(message);
            return ActionResult.PASS;
        }

        PlayerManager playerManager = server.getPlayerManager();
        String banReason = "You ran out of lives!";

        // Calculate when the ban will expire
        Instant thisInstant = (new Date()).toInstant();
        Instant future = thisInstant.plus(MyConfig.BAN_DURATION);
        Date banExpiration = Date.from(future);

        // Add the player to the ban list
        playerManager.getUserBanList().add(
                new net.minecraft.server.BannedPlayerEntry(
                        player.getGameProfile(),
                        new Date(),
                        "Admin",
                        banExpiration,
                        banReason
                )
        );

        // Kick the player from the server
        player.networkHandler.disconnect(Text.literal("You are banned: " + banReason));

        // Prevent further events from being called here
        return ActionResult.FAIL;
    }

    private static ActionResult lifeDropLogic(ServerPlayerEntity player, DamageSource damageSource) {
        // Determine if we should drop hearts based on the configured mode
        if (shouldDropHearts(player, damageSource)) {
            Identifier id = new Identifier(SoftHardcore.MOD_ID, "life_heart");
            ItemStack lifeDrops = new ItemStack(Registries.ITEM.get(id), MyConfig.LIVES_DROPPED);
            player.dropItem(lifeDrops, false);
        }

        return ActionResult.PASS;
    }

    private static boolean shouldDropHearts(ServerPlayerEntity player, DamageSource damageSource) {
        HeartDropMode mode = MyConfig.HEART_DROP_MODE;
        
        switch (mode) {
            case PASSIVE:
                // Drop hearts only on natural deaths (not player kills)
                return !(damageSource.getAttacker() instanceof ServerPlayerEntity);
                
            case NEUTRAL:
                // Drop hearts on any death
                return true;
                
            case TEAM:
                // Drop hearts on natural deaths + when killed by different team player
                if (!(damageSource.getAttacker() instanceof ServerPlayerEntity)) {
                    return true; // Natural death
                }
                ServerPlayerEntity attacker = (ServerPlayerEntity) damageSource.getAttacker();
                return !areOnSameTeam(player, attacker);
                
            case COMPETITIVE:
                // Drop hearts ONLY when killed by different team player
                if (!(damageSource.getAttacker() instanceof ServerPlayerEntity)) {
                    return false; // Natural death - no hearts dropped
                }
                ServerPlayerEntity competitiveAttacker = (ServerPlayerEntity) damageSource.getAttacker();
                return !areOnSameTeam(player, competitiveAttacker);
                
            default:
                return true; // Default to dropping hearts
        }
    }

    private static boolean areOnSameTeam(ServerPlayerEntity player1, ServerPlayerEntity player2) {
        if (player1 == null || player2 == null) {
            return false;
        }
        
        net.minecraft.scoreboard.AbstractTeam team1 = player1.getScoreboardTeam();
        net.minecraft.scoreboard.AbstractTeam team2 = player2.getScoreboardTeam();
        
        // If both players have no team, consider them on the same "team"
        if (team1 == null && team2 == null) {
            return true;
        }
        
        // If only one has a team, they're on different teams
        if (team1 == null || team2 == null) {
            return false;
        }
        
        // Compare teams
        return team1.equals(team2);
    }

    private static void loginEventLogic(ServerPlayNetworkHandler handler, PacketSender p, MinecraftServer m) {
        ServerPlayerEntity player = handler.getPlayer();
        LivesComponent component = MyComponents.LIVES_KEY.get(player);

        // If the player has 0 lives but isn't banned,
        // Then their ban must have expired. Let them
        // back onto the server!
        if (component.getLives() <= 0) {
            Text message = Text.literal("Welcome back! Your lives have been refilled.").formatted(Formatting.GREEN);
            player.sendMessage(message);
            component.setLives(MyConfig.RETURNING_LIVES);
            // Set both timestamps to current time so they don't immediately regenerate
            long currentTime = System.currentTimeMillis();
            component.setLastLifeRegenTime(currentTime);
            component.setLastLifeLostTime(currentTime);
            
            // Update the lives cache for the scoreboard
            LivesCacheManager.updateLivesCache(player.getUuid(), component.getLives());
        }
        
        // Trigger bulk sync to ensure joining player sees all other players' lives
        syncAllPlayersLives(m);
    }

    private static void syncAllPlayersLives(MinecraftServer server) {
        // Sync all players' lives to all other players
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            LivesComponent component = MyComponents.LIVES_KEY.get(player);
            MyComponents.LIVES_KEY.sync(player);
            
            // Update the lives cache for the scoreboard
            LivesCacheManager.updateLivesCache(player.getUuid(), component.getLives());
        }
    }

}
