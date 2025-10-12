package net.ness.softhardcore.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class to manage the lives cache for the scoreboard.
 * This allows external classes to update the cache without violating Mixin restrictions.
 * 
 * Note: This class is used by server-side code to update the cache,
 * but the actual cache storage is in the PlayersListMixin for client-side rendering.
 */
public class LivesCacheManager {
    
    // Cache to store lives data for dead players
    private static final Map<UUID, Integer> livesCache = new ConcurrentHashMap<>();
    
    /**
     * Updates the lives cache for a player. This should be called when a player's lives change
     * or when they die, to ensure the cache stays up to date.
     */
    public static void updateLivesCache(UUID playerUuid, int lives) {
        livesCache.put(playerUuid, lives);
    }
    
    /**
     * Gets the cached lives for a player, or null if not cached.
     */
    public static Integer getCachedLives(UUID playerUuid) {
        return livesCache.get(playerUuid);
    }
    
    /**
     * Clears the lives cache. This can be called when disconnecting from a server
     * to prevent stale data.
     */
    public static void clearLivesCache() {
        livesCache.clear();
    }
}
