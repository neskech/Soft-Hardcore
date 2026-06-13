package net.ness.softhardcore;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.ness.softhardcore.config.HeartDropMode;
import net.ness.softhardcore.config.MyConfig;
import net.ness.softhardcore.item.ModItems;
import net.ness.softhardcore.server.LivesService;
import net.ness.softhardcore.server.LivesStore;

public class EventLogic {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        lifeDecrementLogic(player, event.getSource());
        lifeDropLogic(player, event.getSource());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        loginEventLogic(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        disconnectEventLogic(player);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) {
            return;
        }
        respawnEventLogic(newPlayer);
    }

    private static void lifeDecrementLogic(ServerPlayer player, DamageSource damageSource) {
        int lives = LivesService.decrement(player.server, player.getUUID());

        if (lives > 0) {
            Component message = Component.literal("You've just lost a life...").withStyle(ChatFormatting.RED);
            player.sendSystemMessage(message);
            return;
        }

        LivesService.setPendingBan(player.server, player.getUUID(), true);
        Component message = Component.literal("You've run out of lives! You will be banned when you respawn.").withStyle(ChatFormatting.RED);
        player.sendSystemMessage(message);
    }

    private static void lifeDropLogic(ServerPlayer player, DamageSource damageSource) {
        if (shouldDropHearts(player, damageSource)) {
            ItemStack lifeDrops = new ItemStack(ModItems.LIFE_HEART.get(), MyConfig.LIVES_DROPPED);
            player.drop(lifeDrops, false);
        }
    }

    private static boolean shouldDropHearts(ServerPlayer player, DamageSource damageSource) {
        HeartDropMode mode = MyConfig.HEART_DROP_MODE;
        boolean isPlayerKill = damageSource.getEntity() instanceof ServerPlayer;

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
                ServerPlayer attacker = (ServerPlayer) damageSource.getEntity();
                yield !areOnSameTeam(player, attacker);
            }
            case COMPETITIVE -> {
                if (!isPlayerKill) {
                    yield false;
                }
                ServerPlayer competitiveAttacker = (ServerPlayer) damageSource.getEntity();
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

    private static boolean areOnSameTeam(ServerPlayer player1, ServerPlayer player2) {
        if (player1 == null || player2 == null) {
            return false;
        }

        PlayerTeam team1 = player1.getTeam();
        PlayerTeam team2 = player2.getTeam();

        if (team1 == null && team2 == null) {
            return true;
        }

        if (team1 == null || team2 == null) {
            return false;
        }

        return team1.equals(team2);
    }

    private static void loginEventLogic(ServerPlayer player) {
        MinecraftServer server = player.server;

        if (LivesService.getLives(server, player.getUUID()) <= 0) {
            long currentTime = System.currentTimeMillis();
            long lastLifeLostTime = LivesStore.get(server).getLastLifeLostTime(player.getUUID());
            long banDurationMillis = MyConfig.BAN_DURATION.toMillis();
            long timeSinceLifeLost = currentTime - lastLifeLostTime;

            if (timeSinceLifeLost < banDurationMillis) {
                long remainingMillis = banDurationMillis - timeSinceLifeLost;
                String human = formatDuration(remainingMillis);
                String kickMessage = "You ran out of lives! You can rejoin in " + human + ".";
                player.connection.disconnect(Component.literal(kickMessage));
                return;
            }

            LivesService.setPendingBan(server, player.getUUID(), false);

            Component message = Component.literal("Welcome back! Your lives have been refilled.").withStyle(ChatFormatting.GREEN);
            player.sendSystemMessage(message);
            LivesService.setLives(server, player.getUUID(), MyConfig.RETURNING_LIVES);
            LivesService.markBanEnd(server, player.getUUID());
        }

        int joiningLives = LivesService.getLives(server, player.getUUID());
        LivesService.broadcastBulkTo(player);
        net.ness.softhardcore.networking.NetworkManager.broadcastLivesOne(server, player.getUUID(), joiningLives);
    }

    private static void respawnEventLogic(ServerPlayer newPlayer) {
        if (LivesService.isPendingBan(newPlayer.server, newPlayer.getUUID())) {
            LivesService.setPendingBan(newPlayer.server, newPlayer.getUUID(), false);

            MinecraftServer server = newPlayer.server;
            if (server == null) {
                Component message = Component.literal("ERROR: Player has pending ban, but server is null").withStyle(ChatFormatting.RED);
                newPlayer.sendSystemMessage(message);
                return;
            }

            LivesService.markBanStart(server, newPlayer.getUUID());

            long banDurationMillis = MyConfig.BAN_DURATION.toMillis();
            String banMessage = "You ran out of lives! You can rejoin in " + formatDuration(banDurationMillis) + ".";
            newPlayer.connection.disconnect(Component.literal(banMessage));
        }
    }

    private static void disconnectEventLogic(ServerPlayer player) {
        MinecraftServer server = player.server;
        if (LivesService.isPendingBan(server, player.getUUID())) {
            LivesService.markBanStart(server, player.getUUID());
            LivesService.setPendingBan(server, player.getUUID(), false);
            server.sendSystemMessage(Component.literal("Player " + player.getName().getString() + " disconnected with pending ban"));
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
