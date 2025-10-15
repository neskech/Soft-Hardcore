package net.ness.softhardcore.config;

import com.mojang.datafixers.util.Pair;
import net.ness.softhardcore.SoftHardcore;

import java.time.Duration;

public class MyConfig {
    private static SimpleConfig CONFIG;

    // Keys
    public static final String KEY_DEFAULT_LIVES = "default.lives";
    public static final String KEY_BAN_DURATION = "ban.duration";
    public static final String KEY_LIVES_DROPPED = "lives.dropped.on.death";
    public static final String KEY_HEART_DROP_MODE = "heart.drop.mode";
    public static final String KEY_LIFE_REGEN_COOLDOWN = "life.regen.cooldown";
    public static final String KEY_RETURNING_LIVES = "returning.lives";
    public static final String KEY_LIVES_GAINED_FROM_HEART = "lives.gained.from.heart";
    public static final String KEY_SCOREBOARD_MAX_ROWS = "scoreboard.max.rows";
    public static final String KEY_PASSIVE_DEATH_HEART_DROP_PROBABILITY = "passive.death.heart.drop.probability";
    public static final String KEY_PLAYER_DEATH_HEART_DROP_PROBABILITY = "player.death.heart.drop.probability";
    public static final String KEY_LIFE_REGEN_CEILING = "life.regen.ceiling";

    // Defaults (single source of truth)
    public static final int DEF_DEFAULT_LIVES = 25;
    public static final String DEF_BAN_DURATION = "P2D"; // ISO-8601 (2 days)
    public static final int DEF_LIVES_DROPPED = 1;
    public static final String DEF_HEART_DROP_MODE = "NEUTRAL";
    public static final String DEF_LIFE_REGEN_COOLDOWN = "PT24H"; // ISO-8601 (24 hours)
    public static final int DEF_RETURNING_LIVES = 5;
    public static final int DEF_LIVES_GAINED_FROM_HEART = 1;
    public static final int DEF_SCOREBOARD_MAX_ROWS = 5;
    public static final double DEF_PASSIVE_DEATH_HEART_DROP_PROBABILITY = 1.0;
    public static final double DEF_PLAYER_DEATH_HEART_DROP_PROBABILITY = 1.0;
    public static final int DEF_LIFE_REGEN_CEILING = 25;

    public static int DEFAULT_LIVES;
    public static Duration BAN_DURATION;
    public static int LIVES_DROPPED;
    public static HeartDropMode HEART_DROP_MODE;
    public static Duration LIFE_REGEN_COOLDOWN;
    public static int RETURNING_LIVES;
    public static int LIVES_GAINED_FROM_HEART;
    public static int MAX_SCOREBOARD_ROWS;
    public static double PASSIVE_DEATH_HEART_DROP_PROBABILITY;
    public static double PLAYER_DEATH_HEART_DROP_PROBABILITY;
    public static int LIFE_REGEN_CEILING;

    public static void registerConfig() {
        BasicConfigProvider provider = new BasicConfigProvider();
        setDefaults(provider);
        CONFIG = SimpleConfig.of(SoftHardcore.MOD_ID + "config").provider(provider).request();
        assignClassDefaults();
    }

    private static void setDefaults(BasicConfigProvider provider) {
        provider.addKeyValuePair(new Pair<>(KEY_DEFAULT_LIVES, DEF_DEFAULT_LIVES), "Int");
        provider.addKeyValuePair(new Pair<>(KEY_BAN_DURATION, DEF_BAN_DURATION), "ISO-8601 duration format");
        provider.addKeyValuePair(new Pair<>(KEY_LIVES_DROPPED, DEF_LIVES_DROPPED), "Int");
        provider.addKeyValuePair(new Pair<>(KEY_HEART_DROP_MODE, DEF_HEART_DROP_MODE), "PASSIVE|NEUTRAL|TEAM|COMPETITIVE|VENGEFUL|NEVER");
        provider.addKeyValuePair(new Pair<>(KEY_LIFE_REGEN_COOLDOWN, DEF_LIFE_REGEN_COOLDOWN), "ISO-8601 duration");
        provider.addKeyValuePair(new Pair<>(KEY_RETURNING_LIVES, DEF_RETURNING_LIVES), "Int (1 to default.lives)");
        provider.addKeyValuePair(new Pair<>(KEY_LIVES_GAINED_FROM_HEART, DEF_LIVES_GAINED_FROM_HEART), "Int (lives gained when consuming a heart)");
        provider.addKeyValuePair(new Pair<>(KEY_SCOREBOARD_MAX_ROWS, DEF_SCOREBOARD_MAX_ROWS), "Int (maximum number of players shown on scoreboard)");
        provider.addKeyValuePair(new Pair<>(KEY_PASSIVE_DEATH_HEART_DROP_PROBABILITY, DEF_PASSIVE_DEATH_HEART_DROP_PROBABILITY), "Double (0.0-1.0, probability of dropping hearts on natural deaths)");
        provider.addKeyValuePair(new Pair<>(KEY_PLAYER_DEATH_HEART_DROP_PROBABILITY, DEF_PLAYER_DEATH_HEART_DROP_PROBABILITY), "Double (0.0-1.0, probability of dropping hearts on player kills)");
        provider.addKeyValuePair(new Pair<>(KEY_LIFE_REGEN_CEILING, DEF_LIFE_REGEN_CEILING), "Int (lives at or above this won't regenerate)");
    }

    private static void assignClassDefaults() {
        DEFAULT_LIVES = CONFIG.getOrDefault(KEY_DEFAULT_LIVES, DEF_DEFAULT_LIVES);
        
        // Parse ban duration with error handling
        String banDurationStr = CONFIG.getOrDefault(KEY_BAN_DURATION, DEF_BAN_DURATION);
        try {
            BAN_DURATION = Duration.parse(banDurationStr);
        } catch (Exception e) {
            SoftHardcore.LOGGER.error("Invalid ban duration format: " + banDurationStr + ", using default: " + DEF_BAN_DURATION);
            BAN_DURATION = Duration.parse(DEF_BAN_DURATION);
        }
        
        LIVES_DROPPED = CONFIG.getOrDefault(KEY_LIVES_DROPPED, DEF_LIVES_DROPPED);
        
        // Parse heart drop mode
        String modeStr = CONFIG.getOrDefault(KEY_HEART_DROP_MODE, DEF_HEART_DROP_MODE);
        try {
            HEART_DROP_MODE = HeartDropMode.valueOf(modeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            HEART_DROP_MODE = HeartDropMode.NEUTRAL; // Default fallback
        }
        
        // Parse life regen cooldown with error handling
        String regenCooldownStr = CONFIG.getOrDefault(KEY_LIFE_REGEN_COOLDOWN, DEF_LIFE_REGEN_COOLDOWN);
        try {
            LIFE_REGEN_COOLDOWN = Duration.parse(regenCooldownStr);
        } catch (Exception e) {
            SoftHardcore.LOGGER.error("Invalid life regen cooldown format: " + regenCooldownStr + ", using default: " + DEF_LIFE_REGEN_COOLDOWN);
            LIFE_REGEN_COOLDOWN = Duration.parse(DEF_LIFE_REGEN_COOLDOWN);
        }
        
        // Validate returning lives
        int returningLives = CONFIG.getOrDefault(KEY_RETURNING_LIVES, DEF_RETURNING_LIVES);
        RETURNING_LIVES = Math.max(1, Math.min(returningLives, DEFAULT_LIVES));
        
        LIVES_GAINED_FROM_HEART = CONFIG.getOrDefault(KEY_LIVES_GAINED_FROM_HEART, DEF_LIVES_GAINED_FROM_HEART);
        
        // Validate max scoreboard rows (minimum 1, maximum 20)
        int maxRows = CONFIG.getOrDefault(KEY_SCOREBOARD_MAX_ROWS, DEF_SCOREBOARD_MAX_ROWS);
        MAX_SCOREBOARD_ROWS = Math.max(1, Math.min(maxRows, 20));
        
        // Validate probability values (0.0 to 1.0)
        double passiveProb = CONFIG.getOrDefault(KEY_PASSIVE_DEATH_HEART_DROP_PROBABILITY, DEF_PASSIVE_DEATH_HEART_DROP_PROBABILITY);
        PASSIVE_DEATH_HEART_DROP_PROBABILITY = Math.max(0.0, Math.min(passiveProb, 1.0));
        
        double playerProb = CONFIG.getOrDefault(KEY_PLAYER_DEATH_HEART_DROP_PROBABILITY, DEF_PLAYER_DEATH_HEART_DROP_PROBABILITY);
        PLAYER_DEATH_HEART_DROP_PROBABILITY = Math.max(0.0, Math.min(playerProb, 1.0));
        
        // Validate regeneration ceiling (1 to default lives)
        int regenCeiling = CONFIG.getOrDefault(KEY_LIFE_REGEN_CEILING, DEF_LIFE_REGEN_CEILING);
        LIFE_REGEN_CEILING = Math.max(1, Math.min(regenCeiling, DEFAULT_LIVES));
    }

    /**
     * Reloads all config values from the file and updates the static fields.
     * Call this after modifying the config file to apply changes at runtime.
     */
    public static void reloadConfig() {
        try {
            CONFIG.reload();
            assignClassDefaults();
            SoftHardcore.LOGGER.info("SoftHardcore config reloaded successfully");
        } catch (Exception e) {
            SoftHardcore.LOGGER.error("Failed to reload config: " + e.getMessage());
        }
    }

    /**
     * Sets a config value and saves it to file.
     * 
     * @param key   the config key
     * @param value the config value
     * @return true if successful, false if there was an error
     */
    public static boolean setConfigValue(String key, String value) {
        try {
            CONFIG.set(key, value);
            CONFIG.save();
            SoftHardcore.LOGGER.info("Set config " + key + " = " + value);
            return true;
        } catch (Exception e) {
            SoftHardcore.LOGGER.error("Failed to set config " + key + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets the underlying SimpleConfig instance for advanced operations.
     * 
     * @return the SimpleConfig instance
     */
    public static SimpleConfig getConfig() {
        return CONFIG;
    }

}
