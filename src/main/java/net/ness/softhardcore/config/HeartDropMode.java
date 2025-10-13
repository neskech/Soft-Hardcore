package net.ness.softhardcore.config;

public enum HeartDropMode {
    PASSIVE,    // Drop hearts on natural deaths only
    NEUTRAL,    // Drop hearts on any death
    TEAM,       // Drop on natural deaths + different team kills
    COMPETITIVE, // Drop ONLY on different team kills
    VENGEFUL,   // Drop hearts ONLY when killed by another player (any player)
    NEVER       // Never drop hearts
}
