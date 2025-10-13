package net.ness.softhardcore;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
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
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;


public class EventLogic {

    public static void registerEventLogic() {
        PlayerDeathCallback.EVENT.register((player, damageSource) -> lifeDecrementLogic(player, damageSource));
        PlayerDeathCallback.EVENT.register((player, damageSource) -> lifeDropLogic(player, damageSource));
        ServerPlayerEvents.AFTER_RESPAWN.register(EventLogic::respawnEventLogic);
        ServerPlayConnectionEvents.JOIN.register(EventLogic::loginEventLogic);
        ServerPlayConnectionEvents.DISCONNECT.register(EventLogic::disconnectEventLogic);
    }

    private static ActionResult lifeDecrementLogic(ServerPlayerEntity player, DamageSource damageSource) {
        LivesComponent component = MyComponents.LIVES_KEY.get(player);
        component.decrement();
        component.setLastLifeLostTime(System.currentTimeMillis());
        
        // Component sync will automatically update the client-side cache

        // Only set pending ban if the player's run out of lives
        if (component.getLives() > 0) {
            Text message = Text.literal("You've just lost a life...").formatted(Formatting.RED);
            player.sendMessage(message);
            return ActionResult.PASS;
        }

        // Set pending ban instead of immediately banning
        component.setPendingBan(true);
        Text message = Text.literal("You've run out of lives! You will be banned when you respawn.").formatted(Formatting.RED);
        player.sendMessage(message);

        return ActionResult.PASS;
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
        boolean isPlayerKill = damageSource.getAttacker() instanceof ServerPlayerEntity;
        
        // If mode is NEVER, never drop hearts regardless of probabilities
        if (mode == HeartDropMode.NEVER) {
            return false;
        }
        
        boolean shouldDrop = false;
        
        switch (mode) {
            case PASSIVE:
                // Drop hearts only on natural deaths (not player kills)
                shouldDrop = !isPlayerKill;
                break;
                
            case NEUTRAL:
                // Drop hearts on any death
                shouldDrop = true;
                break;
                
            case TEAM:
                // Drop hearts on natural deaths + when killed by different team player
                if (!isPlayerKill) {
                    shouldDrop = true; // Natural death
                } else {
                    ServerPlayerEntity attacker = (ServerPlayerEntity) damageSource.getAttacker();
                    shouldDrop = !areOnSameTeam(player, attacker);
                }
                break;
                
            case COMPETITIVE:
                // Drop hearts ONLY when killed by different team player
                if (!isPlayerKill) {
                    shouldDrop = false; // Natural death - no hearts dropped
                } else {
                    ServerPlayerEntity competitiveAttacker = (ServerPlayerEntity) damageSource.getAttacker();
                    shouldDrop = !areOnSameTeam(player, competitiveAttacker);
                }
                break;
                
            case VENGEFUL:
                // Drop hearts ONLY when killed by another player (any player, regardless of team)
                shouldDrop = isPlayerKill;
                break;
                
            default:
                shouldDrop = true; // Default to dropping hearts
        }
        
        // If we should drop hearts based on mode, apply probability
        if (shouldDrop) {
            double probability = isPlayerKill ? 
                MyConfig.PLAYER_DEATH_HEART_DROP_PROBABILITY : 
                MyConfig.PASSIVE_DEATH_HEART_DROP_PROBABILITY;
            return Math.random() < probability;
        }
        
        return false;
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

        // If the player has 0 lives, check if their ban has expired
        if (component.getLives() <= 0) {
            long currentTime = System.currentTimeMillis();
            long lastLifeLostTime = component.getLastLifeLostTime();
            long banDurationMillis = MyConfig.BAN_DURATION.toMillis();
            long timeSinceLifeLost = currentTime - lastLifeLostTime;
            
            // If the ban hasn't expired yet, kick them
            if (timeSinceLifeLost < banDurationMillis) {
                long remainingSeconds = (banDurationMillis - timeSinceLifeLost) / 1000;
                String kickMessage = "You ran out of lives! You can rejoin in " + remainingSeconds + " seconds.";
                player.networkHandler.disconnect(Text.literal(kickMessage));
                return;
            }
            
            // Ban has expired, restore their lives
            // Clear any pending ban flag that might be stuck
            component.setPendingBan(false);
            
            Text message = Text.literal("Welcome back! Your lives have been refilled.").formatted(Formatting.GREEN);
            player.sendMessage(message);
            component.setLives(MyConfig.RETURNING_LIVES);
            // Set both timestamps to current time so they don't immediately regenerate
            component.setLastLifeRegenTime(currentTime);
            component.setLastLifeLostTime(currentTime);
            
            // Component sync will automatically update the client-side cache
        }
        
        // Component sync will automatically update the client-side cache
        
        // Trigger bulk sync to ensure joining player sees all other players' lives
        syncAllPlayersLives(m);
    }

    private static void syncAllPlayersLives(MinecraftServer server) {
        // Sync all players' lives to all other players
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            MyComponents.LIVES_KEY.sync(player);
        }
    }

    /**
     * Handles respawn events - bans players who have pending bans
     */
    private static void respawnEventLogic(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) {
        LivesComponent component = MyComponents.LIVES_KEY.get(newPlayer);
        
        if (component.isPendingBan()) {
            // Clear the pending ban flag
            component.setPendingBan(false);
            
            // Now ban the player
            MinecraftServer server = newPlayer.getServer();
            if (server == null) {
                Text message = Text.literal("ERROR: Player has pending ban, but server is null").formatted(Formatting.RED);
                newPlayer.sendMessage(message);
                return;
            }

            // Store the ban time in the component instead of using Minecraft's ban system
            component.setLastLifeLostTime(System.currentTimeMillis());
            
            // Calculate when the ban will expire
            long banDurationMillis = MyConfig.BAN_DURATION.toMillis();
            long banExpiresAt = System.currentTimeMillis() + banDurationMillis;
            
            // Kick the player from the server with a message
            String banMessage = "You ran out of lives! You can rejoin in " + (banDurationMillis / 1000) + " seconds.";
            newPlayer.networkHandler.disconnect(Text.literal(banMessage));
        }
    }

    /**
     * Handles disconnect events - stores the ban time for players who disconnect with pending bans
     */
    private static void disconnectEventLogic(ServerPlayNetworkHandler handler, MinecraftServer server) {
        ServerPlayerEntity player = handler.getPlayer();
        LivesComponent component = MyComponents.LIVES_KEY.get(player);
        
        if (component.isPendingBan()) {
            // Player disconnected with a pending ban, store the time they lost their life
            component.setLastLifeLostTime(System.currentTimeMillis());
            
            // Clear the pending ban flag
            component.setPendingBan(false);
            
            server.sendMessage(Text.literal("Player " + player.getName() + " disconnected with pending ban"));
        }
    }

}
