package net.ness.softhardcore.server;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.ness.softhardcore.config.MyConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LivesStore extends SavedData {

    private static final SavedData.Factory<LivesStore> FACTORY = new SavedData.Factory<>(
            LivesStore::new,
            LivesStore::load,
            DataFixTypes.LEVEL
    );

    private final Map<UUID, Integer> lives = new HashMap<>();
    private final Map<UUID, Long> lastLifeLostTime = new HashMap<>();
    private final Map<UUID, Long> lastLifeRegenTime = new HashMap<>();
    private final Map<UUID, Boolean> pendingBan = new HashMap<>();

    public static LivesStore get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "softhardcore_lives");
    }

    public int getLives(UUID uuid) {
        if (!lives.containsKey(uuid)) {
            lives.put(uuid, MyConfig.DEFAULT_LIVES);
            setDirty();
            return MyConfig.DEFAULT_LIVES;
        }
        return lives.get(uuid);
    }

    /** Returns stored lives, or -1 if this UUID has never been tracked. Does not insert defaults. */
    public int peekLives(UUID uuid) {
        return lives.getOrDefault(uuid, -1);
    }

    public boolean hasRecord(UUID uuid) {
        return lives.containsKey(uuid);
    }

    public void setLives(UUID uuid, int amount) {
        int clamped = Math.max(Math.min(amount, MyConfig.DEFAULT_LIVES), 0);
        lives.put(uuid, clamped);
        setDirty();
    }

    public long getLastLifeLostTime(UUID uuid) {
        return lastLifeLostTime.getOrDefault(uuid, 0L);
    }

    public void setLastLifeLostTime(UUID uuid, long ts) {
        lastLifeLostTime.put(uuid, ts);
        setDirty();
    }

    public long getLastLifeRegenTime(UUID uuid) {
        return lastLifeRegenTime.getOrDefault(uuid, 0L);
    }

    public void setLastLifeRegenTime(UUID uuid, long ts) {
        lastLifeRegenTime.put(uuid, ts);
        setDirty();
    }

    public boolean isPendingBan(UUID uuid) {
        return pendingBan.getOrDefault(uuid, false);
    }

    public void setPendingBan(UUID uuid, boolean value) {
        pendingBan.put(uuid, value);
        setDirty();
    }

    public Map<UUID, Integer> snapshotLives() {
        return new HashMap<>(lives);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag livesNbt = new CompoundTag();
        for (Map.Entry<UUID, Integer> e : lives.entrySet()) {
            livesNbt.putInt(e.getKey().toString(), e.getValue());
        }
        tag.put("lives", livesNbt);

        CompoundTag lostNbt = new CompoundTag();
        for (Map.Entry<UUID, Long> e : lastLifeLostTime.entrySet()) {
            lostNbt.putLong(e.getKey().toString(), e.getValue());
        }
        tag.put("lastLifeLostTime", lostNbt);

        CompoundTag regenNbt = new CompoundTag();
        for (Map.Entry<UUID, Long> e : lastLifeRegenTime.entrySet()) {
            regenNbt.putLong(e.getKey().toString(), e.getValue());
        }
        tag.put("lastLifeRegenTime", regenNbt);

        CompoundTag pendingNbt = new CompoundTag();
        for (Map.Entry<UUID, Boolean> e : pendingBan.entrySet()) {
            pendingNbt.putBoolean(e.getKey().toString(), e.getValue());
        }
        tag.put("pendingBan", pendingNbt);

        return tag;
    }

    public static LivesStore load(CompoundTag tag, HolderLookup.Provider provider) {
        LivesStore store = new LivesStore();

        if (tag.contains("lives")) {
            CompoundTag livesNbt = tag.getCompound("lives");
            for (String k : livesNbt.getAllKeys()) {
                store.lives.put(UUID.fromString(k), livesNbt.getInt(k));
            }
        }
        if (tag.contains("lastLifeLostTime")) {
            CompoundTag lostNbt = tag.getCompound("lastLifeLostTime");
            for (String k : lostNbt.getAllKeys()) {
                store.lastLifeLostTime.put(UUID.fromString(k), lostNbt.getLong(k));
            }
        }
        if (tag.contains("lastLifeRegenTime")) {
            CompoundTag regenNbt = tag.getCompound("lastLifeRegenTime");
            for (String k : regenNbt.getAllKeys()) {
                store.lastLifeRegenTime.put(UUID.fromString(k), regenNbt.getLong(k));
            }
        }
        if (tag.contains("pendingBan")) {
            CompoundTag pendingNbt = tag.getCompound("pendingBan");
            for (String k : pendingNbt.getAllKeys()) {
                store.pendingBan.put(UUID.fromString(k), pendingNbt.getBoolean(k));
            }
        }
        return store;
    }
}
