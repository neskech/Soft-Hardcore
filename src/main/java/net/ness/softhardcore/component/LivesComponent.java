package net.ness.softhardcore.component;

import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.ness.softhardcore.config.MyConfig;

public class LivesComponent implements AutoSyncedComponent {
    private final PlayerEntity provider;
    private int lives = MyConfig.DEFAULT_LIVES;
    private long lastLifeLostTime = 0;
    private long lastLifeRegenTime = 0;
    private boolean pendingBan = false;

    LivesComponent(PlayerEntity provider) {
        this.provider = provider;
    }

    public int getLives() {
        return this.lives;
    }

    public void decrement() {
        this.lives = Math.max(this.lives - 1, 0);
        MyComponents.LIVES_KEY.sync(this.provider);
    }

    public void increment() {
        this.lives = Math.min(this.lives + 1, MyConfig.DEFAULT_LIVES);
        MyComponents.LIVES_KEY.sync(this.provider);
    }

    public void setLives(int lives) {
        this.lives = Math.max(Math.min(lives, MyConfig.DEFAULT_LIVES), 0);
        MyComponents.LIVES_KEY.sync(this.provider);
    }

    public long getLastLifeLostTime() {
        return this.lastLifeLostTime;
    }

    public void setLastLifeLostTime(long timestamp) {
        this.lastLifeLostTime = timestamp;
        MyComponents.LIVES_KEY.sync(this.provider);
    }

    public long getLastLifeRegenTime() {
        return this.lastLifeRegenTime;
    }

    public void setLastLifeRegenTime(long timestamp) {
        this.lastLifeRegenTime = timestamp;
        MyComponents.LIVES_KEY.sync(this.provider);
    }

    public boolean isPendingBan() {
        return this.pendingBan;
    }

    public void setPendingBan(boolean pendingBan) {
        this.pendingBan = pendingBan;
        MyComponents.LIVES_KEY.sync(this.provider);
    }

    public boolean canRegenerateLife() {
        if (this.lives >= MyConfig.DEFAULT_LIVES) {
            return false; // Already at max lives
        }
        
        long currentTime = System.currentTimeMillis();
        // Use default 24 hours if config not loaded yet
        long cooldownMillis = MyConfig.LIFE_REGEN_COOLDOWN != null ? 
            MyConfig.LIFE_REGEN_COOLDOWN.toMillis() : 24 * 60 * 60 * 1000L;
        
        // Check cooldown from when player last lost a life, not when they last regenerated
        long timeSinceLastDeath = currentTime - this.lastLifeLostTime;
        return timeSinceLastDeath >= cooldownMillis;
    }

    public boolean regenerateLife() {
        if (canRegenerateLife()) {
            this.lives = Math.min(this.lives + 1, MyConfig.DEFAULT_LIVES);
            this.lastLifeRegenTime = System.currentTimeMillis();
            MyComponents.LIVES_KEY.sync(this.provider);
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return true; // Sync to ALL players, not just owner
    }

    @Override
    public void readFromNbt(NbtCompound nbtCompound) {
        this.lives = nbtCompound.getInt("lives");
        this.lastLifeLostTime = nbtCompound.getLong("lastLifeLostTime");
        this.lastLifeRegenTime = nbtCompound.getLong("lastLifeRegenTime");
        this.pendingBan = nbtCompound.getBoolean("pendingBan");
    }

    @Override
    public void writeToNbt(NbtCompound nbtCompound) {
        nbtCompound.putInt("lives", this.lives);
        nbtCompound.putLong("lastLifeLostTime", this.lastLifeLostTime);
        nbtCompound.putLong("lastLifeRegenTime", this.lastLifeRegenTime);
        nbtCompound.putBoolean("pendingBan", this.pendingBan);
    }
}
