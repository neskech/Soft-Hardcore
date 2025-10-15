package net.ness.softhardcore;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.ness.softhardcore.server.LivesService;

public class LifeRegenerationTask {
    private static long lastCheckTime = 0;
    private static final int CHECK_SECONDS = 60 * 5; // 5 minutes
    private static final long CHECK_INTERVAL = 20 * CHECK_SECONDS; // Check every 5 minutes

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(LifeRegenerationTask::tick);
    }

    private static void tick(MinecraftServer server) {
        long currentTime = server.getTicks();
        
        // Only check every CHECK_INTERVAL ticks
        if (currentTime - lastCheckTime < CHECK_INTERVAL) {
            return;
        }
        
        lastCheckTime = currentTime;
        
        // Check all online players for life regeneration
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            int regenerationAmount = LivesService.tryRegenerate(server, player.getUuid());
            if (regenerationAmount > 0) {
                int currentLives = LivesService.getLives(server, player.getUuid());
                SoftHardcore.LOGGER.info("Player " + player.getName() + " can regenerate life. Current lives: " + currentLives);
                player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), 
                    SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.2f);
                String messageText = regenerationAmount == 1 ? 
                    "You have regenerated a life!" :
                    "You have regenerated " + regenerationAmount + " lives!";
                Text message = Text.literal(messageText).formatted(Formatting.GREEN);
                player.sendMessage(message);
                SoftHardcore.LOGGER.info("Player " + player.getName() + " regenerated a life! New lives: " + currentLives);
            }
        }
    }
}
