package net.ness.softhardcore.config;

import com.mojang.datafixers.util.Pair;
import net.ness.softhardcore.SoftHardcore;

import java.time.Duration;

public class MyConfig {
    private static SimpleConfig CONFIG;

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

    public static void registerConfig() {
        BasicConfigProvider provider = new BasicConfigProvider();
        setDefaults(provider);
        CONFIG = SimpleConfig.of(SoftHardcore.MOD_ID + "config").provider(provider).request();
        assignClassDefaults();
    }

    private static void setDefaults(BasicConfigProvider provider) {
        provider.addKeyValuePair(new Pair<>("default.lives", 25), "Int");
        provider.addKeyValuePair(new Pair<>("ban.duration", "PT48H"), "ISO-8601 duration format");
        provider.addKeyValuePair(new Pair<>("lives.dropped.on.death", 1), "Int");
        provider.addKeyValuePair(new Pair<>("heart.drop.mode", "NEUTRAL"), "PASSIVE|NEUTRAL|TEAM|COMPETITIVE|VENGEFUL|NEVER");
        provider.addKeyValuePair(new Pair<>("life.regen.cooldown", "PT24H"), "ISO-8601 duration");
        provider.addKeyValuePair(new Pair<>("returning.lives", 5), "Int (1 to default.lives)");
        provider.addKeyValuePair(new Pair<>("lives.gained.from.heart", 1), "Int (lives gained when consuming a heart)");
        provider.addKeyValuePair(new Pair<>("scoreboard.max.rows", 5), "Int (maximum number of players shown on scoreboard)");
        provider.addKeyValuePair(new Pair<>("passive.death.heart.drop.probability", 1.0), "Double (0.0-1.0, probability of dropping hearts on natural deaths)");
        provider.addKeyValuePair(new Pair<>("player.death.heart.drop.probability", 1.0), "Double (0.0-1.0, probability of dropping hearts on player kills)");
    }

    private static void assignClassDefaults() {
        DEFAULT_LIVES = CONFIG.getOrDefault("default.lives", 2);
        BAN_DURATION = Duration.parse(CONFIG.getOrDefault("ban.duration", "PT6S"));
        LIVES_DROPPED = CONFIG.getOrDefault("lives.dropped.on.death", 3);
        
        // Parse heart drop mode
        String modeStr = CONFIG.getOrDefault("heart.drop.mode", "NEUTRAL");
        try {
            HEART_DROP_MODE = HeartDropMode.valueOf(modeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            HEART_DROP_MODE = HeartDropMode.NEUTRAL; // Default fallback
        }
        
        LIFE_REGEN_COOLDOWN = Duration.parse(CONFIG.getOrDefault("life.regen.cooldown", "PT24H"));
        
        // Validate returning lives
        int returningLives = CONFIG.getOrDefault("returning.lives", 1);
        RETURNING_LIVES = Math.max(1, Math.min(returningLives, DEFAULT_LIVES));
        
        LIVES_GAINED_FROM_HEART = CONFIG.getOrDefault("lives.gained.from.heart", 1);
        
        // Validate max scoreboard rows (minimum 1, maximum 20)
        int maxRows = CONFIG.getOrDefault("scoreboard.max.rows", 5);
        MAX_SCOREBOARD_ROWS = Math.max(1, Math.min(maxRows, 20));
        
        // Validate probability values (0.0 to 1.0)
        double passiveProb = CONFIG.getOrDefault("passive.death.heart.drop.probability", 1.0);
        PASSIVE_DEATH_HEART_DROP_PROBABILITY = Math.max(0.0, Math.min(passiveProb, 1.0));
        
        double playerProb = CONFIG.getOrDefault("player.death.heart.drop.probability", 1.0);
        PLAYER_DEATH_HEART_DROP_PROBABILITY = Math.max(0.0, Math.min(playerProb, 1.0));
    }

}
