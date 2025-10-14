package net.ness.softhardcore.component;

import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.ness.softhardcore.SoftHardcore;
import net.ness.softhardcore.config.MyConfig;
import net.ness.softhardcore.util.LivesCacheManager;

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
        this.setLives(this.lives - 1);
    }

    public void increment() {
        this.setLives(this.lives + 1);
    }

    public int setLives(int lives) {
        int previous = this.lives;
        int clamped = Math.max(Math.min(lives, MyConfig.DEFAULT_LIVES), 0);
        this.lives = clamped;
        if (clamped < previous) {
            // Lives decreased
            this.lastLifeLostTime = System.currentTimeMillis();
        }
        boolean wentBelowRegenerationCeiling = previous >= MyConfig.LIFE_REGEN_CEILING && clamped < MyConfig.LIFE_REGEN_CEILING;
        if (clamped == MyConfig.DEFAULT_LIVES || wentBelowRegenerationCeiling) { 
            this.lastLifeRegenTime = 0;
        }
        MyComponents.LIVES_KEY.sync(this.provider);
        return clamped;
    }

    public long getLastLifeLostTime() {
        return this.lastLifeLostTime;
    }

    // Timestamp setters are intentionally internalized to avoid inconsistent state

    public long getLastLifeRegenTime() {
        return this.lastLifeRegenTime;
    }

    // Timestamp setters are intentionally internalized to avoid inconsistent state

    /**
     * Marks the start of a ban window by recording a life loss timestamp as now.
     */
    public void markBanStartNow() {
        this.lastLifeLostTime = System.currentTimeMillis();
        MyComponents.LIVES_KEY.sync(this.provider);
    }

    public void markBanEndNow() {
        // To prevent immediate regeneration
        this.lastLifeRegenTime = System.currentTimeMillis();
        this.lastLifeLostTime = System.currentTimeMillis();
        MyComponents.LIVES_KEY.sync(this.provider);
    }

    public boolean isPendingBan() {
        return this.pendingBan;
    }

    public void setPendingBan(boolean pendingBan) {
        this.pendingBan = pendingBan;
        MyComponents.LIVES_KEY.sync(this.provider);
    }

    public int tryRegenerateLife() {
        if (this.lives >= MyConfig.LIFE_REGEN_CEILING || this.pendingBan) {
            return 0;
        }

        // Use default 24 hours if config not loaded yet
        long currentTime = System.currentTimeMillis();
        long defaultCooldownMillis = 24 * 60 * 60 * 1000L;
        long cooldownMillis = MyConfig.LIFE_REGEN_COOLDOWN != null ? MyConfig.LIFE_REGEN_COOLDOWN.toMillis() : defaultCooldownMillis;
        long anchorTime = this.lastLifeRegenTime > 0 ? this.lastLifeRegenTime : this.lastLifeLostTime;

        // If anchor time is 0, then we don't have any history, which should never happen but we'll handle it anyway
        int regenerationAmount = 1;
        if (anchorTime > 0) {
            long timeSinceLastAnchor = currentTime - anchorTime;
            regenerationAmount = (int) (timeSinceLastAnchor / cooldownMillis);
            SoftHardcore.LOGGER.info("Regeneration amount: " + regenerationAmount + " from " + anchorTime / 1000 + " to " + currentTime / 1000 + " for a difference of " + timeSinceLastAnchor / 1000 + " and cooldown of " + cooldownMillis / 1000);
            regenerationAmount = Math.min(regenerationAmount, MyConfig.LIFE_REGEN_CEILING - this.lives);
        }

        if (regenerationAmount <= 0) {
            return 0;
        }
        
        int previous = this.lives;
        int current = this.setLives(this.lives + regenerationAmount);

        if (current > previous) {
            this.lastLifeRegenTime = System.currentTimeMillis();
        }

        return regenerationAmount;
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
        
        // Update the client-side cache when component data is synced
        LivesCacheManager.updateLivesCache(this.provider.getUuid(), this.lives);
    }

    @Override
    public void writeToNbt(NbtCompound nbtCompound) {
        nbtCompound.putInt("lives", this.lives);
        nbtCompound.putLong("lastLifeLostTime", this.lastLifeLostTime);
        nbtCompound.putLong("lastLifeRegenTime", this.lastLifeRegenTime);
        nbtCompound.putBoolean("pendingBan", this.pendingBan);
    }
}
