package net.ness.softhardcore;

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.ness.softhardcore.server.LivesService;

public class LifeRegenerationTask {
    private static long lastCheckTime = 0;
    private static final int CHECK_SECONDS = 60 * 5;
    private static final long CHECK_INTERVAL = 20L * CHECK_SECONDS;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tick(event.getServer());
    }

    private static void tick(MinecraftServer server) {
        long currentTime = server.getTickCount();

        if (currentTime - lastCheckTime < CHECK_INTERVAL) {
            return;
        }

        lastCheckTime = currentTime;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            int regenerationAmount = LivesService.tryRegenerate(server, player.getUUID());
            if (regenerationAmount > 0) {
                int currentLives = LivesService.getLives(server, player.getUUID());
                SoftHardcore.LOGGER.info("Player {} can regenerate life. Current lives: {}", player.getName().getString(), currentLives);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);
                String messageText = regenerationAmount == 1 ?
                        "You have regenerated a life!" :
                        "You have regenerated " + regenerationAmount + " lives!";
                Component message = Component.literal(messageText).withStyle(ChatFormatting.GREEN);
                player.sendSystemMessage(message);
                SoftHardcore.LOGGER.info("Player {} regenerated a life! New lives: {}", player.getName().getString(), currentLives);
            }
        }
    }
}
