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
import net.ness.softhardcore.server.LivesService;
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
        int lives = LivesService.decrement(player.getServer(), player.getUuid());

        // Only set pending ban if the player's run out of lives
        if (lives > 0) {
            Text message = Text.literal("You've just lost a life...").formatted(Formatting.RED);
            player.sendMessage(message);
            return ActionResult.PASS;
        }

        // Set pending ban instead of immediately banning
        LivesService.setPendingBan(player.getServer(), player.getUuid(), true);
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

        // If the player has 0 lives, check if their ban has expired
        if (LivesService.getLives(m, player.getUuid()) <= 0) {
            long currentTime = System.currentTimeMillis();
            long lastLifeLostTime = net.ness.softhardcore.server.LivesStore.get(m).getLastLifeLostTime(player.getUuid());
            long banDurationMillis = MyConfig.BAN_DURATION.toMillis();
            long timeSinceLifeLost = currentTime - lastLifeLostTime;
            
            // If the ban hasn't expired yet, kick them
            if (timeSinceLifeLost < banDurationMillis) {
                long remainingMillis = (banDurationMillis - timeSinceLifeLost);
                String human = formatDuration(remainingMillis);
                String kickMessage = "You ran out of lives! You can rejoin in " + human + ".";
                player.networkHandler.disconnect(Text.literal(kickMessage));
                return;
            }
            
            // Ban has expired, restore their lives
            // Clear any pending ban flag that might be stuck
            LivesService.setPendingBan(m, player.getUuid(), false);
            
            Text message = Text.literal("Welcome back! Your lives have been refilled.").formatted(Formatting.GREEN);
            player.sendMessage(message);
            LivesService.setLives(m, player.getUuid(), MyConfig.RETURNING_LIVES);
            // Set both timestamps to current time so they don't immediately regenerate
            LivesService.markBanEnd(m, player.getUuid());
            
            // Broadcasts handle client cache updates
        }
        
        // Ensure store entry exists and send bulk lives to the joining player
        int joiningLives = net.ness.softhardcore.server.LivesService.getLives(m, player.getUuid());
        net.ness.softhardcore.server.LivesService.broadcastBulkTo(player);
        // Also broadcast the joining player's lives to all clients so existing players see them
        net.ness.softhardcore.networking.NetworkManager.broadcastLivesOne(m, player.getUuid(), joiningLives);
    }

    private static void syncAllPlayersLives(MinecraftServer server) {}

    /**
     * Handles respawn events - bans players who have pending bans
     */
    private static void respawnEventLogic(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) {
        if (LivesService.isPendingBan(newPlayer.getServer(), newPlayer.getUuid())) {
            // Clear the pending ban flag
            LivesService.setPendingBan(newPlayer.getServer(), newPlayer.getUuid(), false);
            
            // Now ban the player
            MinecraftServer server = newPlayer.getServer();
            if (server == null) {
                Text message = Text.literal("ERROR: Player has pending ban, but server is null").formatted(Formatting.RED);
                newPlayer.sendMessage(message);
                return;
            }

            // Store the ban time
            LivesService.markBanStart(server, newPlayer.getUuid());
            
            // Calculate when the ban will expire (not used directly in message)
            long banDurationMillis = MyConfig.BAN_DURATION.toMillis();
            
            // Kick the player from the server with a message
            String banMessage = "You ran out of lives! You can rejoin in " + formatDuration(banDurationMillis) + ".";
            newPlayer.networkHandler.disconnect(Text.literal(banMessage));
        }
    }

    /**
     * Handles disconnect events - stores the ban time for players who disconnect with pending bans
     */
    private static void disconnectEventLogic(ServerPlayNetworkHandler handler, MinecraftServer server) {
        ServerPlayerEntity player = handler.getPlayer();
        if (LivesService.isPendingBan(server, player.getUuid())) {
            // Player disconnected with a pending ban, store the time they lost their life
            LivesService.markBanStart(server, player.getUuid());
            
            // Clear the pending ban flag
            LivesService.setPendingBan(server, player.getUuid(), false);
            
            server.sendMessage(Text.literal("Player " + player.getName() + " disconnected with pending ban"));
        }
    }

    private static String formatDuration(long millis) {
        if (millis < 0) millis = 0;
        long totalSeconds = millis / 1000;
        long weeks = totalSeconds / (7 * 24 * 3600);
        totalSeconds %= (7 * 24 * 3600);
        long days = totalSeconds / (24 * 3600);
        totalSeconds %= (24 * 3600);
        long hours = totalSeconds / 3600;
        totalSeconds %= 3600;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (weeks > 0) sb.append(weeks).append(weeks == 1 ? " week" : " weeks");
        if (days > 0) appendWithComma(sb, days + (days == 1 ? " day" : " days"));
        if (hours > 0) appendWithComma(sb, hours + (hours == 1 ? " hour" : " hours"));
        if (minutes > 0) appendWithComma(sb, minutes + (minutes == 1 ? " minute" : " minutes"));
        if (seconds > 0 || sb.length() == 0) appendWithComma(sb, seconds + (seconds == 1 ? " second" : " seconds"));

        return sb.toString();
    }

    private static void appendWithComma(StringBuilder sb, String part) {
        if (sb.length() > 0) sb.append(", ");
        sb.append(part);
    }

}
