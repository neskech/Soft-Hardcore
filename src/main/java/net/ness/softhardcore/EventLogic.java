package net.ness.softhardcore;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.ness.softhardcore.config.HeartDropMode;
import net.ness.softhardcore.config.MyConfig;
import net.ness.softhardcore.event.PlayerDeathCallback;
import net.ness.softhardcore.item.ModItems;
import net.ness.softhardcore.server.LivesService;
import net.ness.softhardcore.server.LivesStore;

import java.util.UUID;

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

        if (lives > 0) {
            Text message = Text.literal("You've just lost a life...").formatted(Formatting.RED);
            player.sendMessage(message);
            return ActionResult.PASS;
        }

        LivesService.setPendingBan(player.getServer(), player.getUuid(), true);
        Text message = Text.literal("You've run out of lives! You will be banned when you respawn.").formatted(Formatting.RED);
        player.sendMessage(message);
        return ActionResult.PASS;
    }

    private static ActionResult lifeDropLogic(ServerPlayerEntity player, DamageSource damageSource) {
        if (shouldDropHearts(player, damageSource)) {
            ItemStack lifeDrops = new ItemStack(ModItems.LIFE_HEART, MyConfig.LIVES_DROPPED);
            player.dropItem(lifeDrops, false);
        }
        return ActionResult.PASS;
    }

    private static boolean shouldDropHearts(ServerPlayerEntity player, DamageSource damageSource) {
        HeartDropMode mode = MyConfig.HEART_DROP_MODE;
        boolean isPlayerKill = damageSource.getAttacker() instanceof ServerPlayerEntity;

        if (mode == HeartDropMode.NEVER) {
            return false;
        }

        boolean shouldDrop = switch (mode) {
            case PASSIVE -> !isPlayerKill;
            case NEUTRAL -> true;
            case TEAM -> {
                if (!isPlayerKill) {
                    yield true;
                }
                ServerPlayerEntity attacker = (ServerPlayerEntity) damageSource.getAttacker();
                yield !areOnSameTeam(player, attacker);
            }
            case COMPETITIVE -> {
                if (!isPlayerKill) {
                    yield false;
                }
                ServerPlayerEntity competitiveAttacker = (ServerPlayerEntity) damageSource.getAttacker();
                yield !areOnSameTeam(player, competitiveAttacker);
            }
            case VENGEFUL -> isPlayerKill;
            default -> true;
        };

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

        Team team1 = player1.getScoreboardTeam();
        Team team2 = player2.getScoreboardTeam();

        if (team1 == null && team2 == null) {
            return true;
        }

        if (team1 == null || team2 == null) {
            return false;
        }

        return team1.equals(team2);
    }

    private static void loginEventLogic(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        ServerPlayerEntity player = handler.getPlayer();
        UUID uuid = player.getUuid();
        LivesStore store = LivesStore.get(server);
        int storedLives = store.peekLives(uuid);

        if (storedLives == 0) {
            long currentTime = System.currentTimeMillis();
            long lastLifeLostTime = store.getLastLifeLostTime(uuid);
            long banDurationMillis = MyConfig.BAN_DURATION.toMillis();
            long timeSinceLifeLost = currentTime - lastLifeLostTime;

            if (timeSinceLifeLost < banDurationMillis) {
                long remainingMillis = banDurationMillis - timeSinceLifeLost;
                String human = formatDuration(remainingMillis);
                Text kickMessage = Text.literal("You ran out of lives! You can rejoin in " + human + ".");
                disconnectPlayer(handler, server, kickMessage);
                return;
            }

            LivesService.setPendingBan(server, uuid, false);

            Text message = Text.literal("Welcome back! Your lives have been refilled.").formatted(Formatting.GREEN);
            player.sendMessage(message);
            LivesService.setLives(server, uuid, MyConfig.RETURNING_LIVES);
            LivesService.markBanEnd(server, uuid);
        }

        int joiningLives = LivesService.getLives(server, uuid);
        LivesService.broadcastBulkTo(player);
        net.ness.softhardcore.networking.NetworkManager.broadcastLivesOne(server, uuid, joiningLives);
    }

    private static void respawnEventLogic(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) {
        if (LivesService.isPendingBan(newPlayer.getServer(), newPlayer.getUuid())) {
            LivesService.setPendingBan(newPlayer.getServer(), newPlayer.getUuid(), false);

            MinecraftServer server = newPlayer.getServer();
            if (server == null) {
                Text message = Text.literal("ERROR: Player has pending ban, but server is null").formatted(Formatting.RED);
                newPlayer.sendMessage(message);
                return;
            }

            LivesService.markBanStart(server, newPlayer.getUuid());

            long banDurationMillis = MyConfig.BAN_DURATION.toMillis();
            Text banMessage = Text.literal("You ran out of lives! You can rejoin in " + formatDuration(banDurationMillis) + ".");
            disconnectPlayer(newPlayer.networkHandler, server, banMessage);
        }
    }

    /**
     * Disconnect on the next server tick so the client receives a proper kick message.
     * Calling disconnect synchronously during JOIN causes "Connecting..." hangs and
     * {@code handleDisconnection() called twice} in the server log.
     */
    private static void disconnectPlayer(ServerPlayNetworkHandler handler, MinecraftServer server, Text reason) {
        server.execute(() -> {
            if (handler.isConnectionOpen()) {
                handler.disconnect(reason);
            }
        });
    }

    private static void disconnectEventLogic(ServerPlayNetworkHandler handler, MinecraftServer server) {
        ServerPlayerEntity player = handler.getPlayer();
        if (LivesService.isPendingBan(server, player.getUuid())) {
            LivesService.markBanStart(server, player.getUuid());
            LivesService.setPendingBan(server, player.getUuid(), false);
            server.sendMessage(Text.literal("Player " + player.getName().getString() + " disconnected with pending ban"));
        }
    }

    private static String formatDuration(long millis) {
        if (millis < 0) {
            millis = 0;
        }
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
        if (weeks > 0) {
            sb.append(weeks).append(weeks == 1 ? " week" : " weeks");
        }
        if (days > 0) {
            appendWithComma(sb, days + (days == 1 ? " day" : " days"));
        }
        if (hours > 0) {
            appendWithComma(sb, hours + (hours == 1 ? " hour" : " hours"));
        }
        if (minutes > 0) {
            appendWithComma(sb, minutes + (minutes == 1 ? " minute" : " minutes"));
        }
        if (seconds > 0 || sb.isEmpty()) {
            appendWithComma(sb, seconds + (seconds == 1 ? " second" : " seconds"));
        }

        return sb.toString();
    }

    private static void appendWithComma(StringBuilder sb, String part) {
        if (!sb.isEmpty()) {
            sb.append(", ");
        }
        sb.append(part);
    }
}
