package net.ness.softhardcore;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.ness.softhardcore.component.LivesComponent;
import net.ness.softhardcore.component.MyComponents;

public class LifeRegenerationTask {
    private static long lastCheckTime = 0;
    private static final long CHECK_INTERVAL = 20; // Check every 20 ticks (1 second)

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
            LivesComponent component = MyComponents.LIVES_KEY.get(player);
            
            if (component.regenerateLife()) {
                // Player regenerated a life
                int currentLives = component.getLives();
                Text message = Text.literal("You have regenerated a life! Lives: " + currentLives)
                        .formatted(Formatting.GREEN);
                player.sendMessage(message);
            }
        }
    }
}
