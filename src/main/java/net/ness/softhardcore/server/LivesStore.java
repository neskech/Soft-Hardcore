package net.ness.softhardcore.server;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.ness.softhardcore.config.MyConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LivesStore extends PersistentState {

    private static final Type<LivesStore> TYPE = new Type<>(
            LivesStore::new,
            LivesStore::fromNbt,
            DataFixTypes.LEVEL
    );

    private final Map<UUID, Integer> lives = new HashMap<>();
    private final Map<UUID, Long> lastLifeLostTime = new HashMap<>();
    private final Map<UUID, Long> lastLifeRegenTime = new HashMap<>();
    private final Map<UUID, Boolean> pendingBan = new HashMap<>();

    public static LivesStore get(MinecraftServer server) {
        PersistentStateManager manager = server.getOverworld().getPersistentStateManager();
        return manager.getOrCreate(TYPE, "softhardcore_lives");
    }

    public int getLives(UUID uuid) {
        if (!lives.containsKey(uuid)) {
            lives.put(uuid, MyConfig.DEFAULT_LIVES);
            markDirty();
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
        markDirty();
    }

    public long getLastLifeLostTime(UUID uuid) {
        return lastLifeLostTime.getOrDefault(uuid, 0L);
    }

    public void setLastLifeLostTime(UUID uuid, long ts) {
        lastLifeLostTime.put(uuid, ts);
        markDirty();
    }

    public long getLastLifeRegenTime(UUID uuid) {
        return lastLifeRegenTime.getOrDefault(uuid, 0L);
    }

    public void setLastLifeRegenTime(UUID uuid, long ts) {
        lastLifeRegenTime.put(uuid, ts);
        markDirty();
    }

    public boolean isPendingBan(UUID uuid) {
        return pendingBan.getOrDefault(uuid, false);
    }

    public void setPendingBan(UUID uuid, boolean value) {
        pendingBan.put(uuid, value);
        markDirty();
    }

    public Map<UUID, Integer> snapshotLives() {
        return new HashMap<>(lives);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtCompound livesNbt = new NbtCompound();
        for (Map.Entry<UUID, Integer> entry : lives.entrySet()) {
            livesNbt.putInt(entry.getKey().toString(), entry.getValue());
        }
        nbt.put("lives", livesNbt);

        NbtCompound lostNbt = new NbtCompound();
        for (Map.Entry<UUID, Long> entry : lastLifeLostTime.entrySet()) {
            lostNbt.putLong(entry.getKey().toString(), entry.getValue());
        }
        nbt.put("lastLifeLostTime", lostNbt);

        NbtCompound regenNbt = new NbtCompound();
        for (Map.Entry<UUID, Long> entry : lastLifeRegenTime.entrySet()) {
            regenNbt.putLong(entry.getKey().toString(), entry.getValue());
        }
        nbt.put("lastLifeRegenTime", regenNbt);

        NbtCompound pendingNbt = new NbtCompound();
        for (Map.Entry<UUID, Boolean> entry : pendingBan.entrySet()) {
            pendingNbt.putBoolean(entry.getKey().toString(), entry.getValue());
        }
        nbt.put("pendingBan", pendingNbt);

        return nbt;
    }

    public static LivesStore fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        LivesStore store = new LivesStore();

        if (nbt.contains("lives")) {
            NbtCompound livesNbt = nbt.getCompound("lives");
            for (String key : livesNbt.getKeys()) {
                store.lives.put(UUID.fromString(key), livesNbt.getInt(key));
            }
        }
        if (nbt.contains("lastLifeLostTime")) {
            NbtCompound lostNbt = nbt.getCompound("lastLifeLostTime");
            for (String key : lostNbt.getKeys()) {
                store.lastLifeLostTime.put(UUID.fromString(key), lostNbt.getLong(key));
            }
        }
        if (nbt.contains("lastLifeRegenTime")) {
            NbtCompound regenNbt = nbt.getCompound("lastLifeRegenTime");
            for (String key : regenNbt.getKeys()) {
                store.lastLifeRegenTime.put(UUID.fromString(key), regenNbt.getLong(key));
            }
        }
        if (nbt.contains("pendingBan")) {
            NbtCompound pendingNbt = nbt.getCompound("pendingBan");
            for (String key : pendingNbt.getKeys()) {
                store.pendingBan.put(UUID.fromString(key), pendingNbt.getBoolean(key));
            }
        }

        return store;
    }
}
