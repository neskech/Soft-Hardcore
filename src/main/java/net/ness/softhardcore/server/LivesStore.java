package net.ness.softhardcore.server;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtLong;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.ness.softhardcore.config.MyConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LivesStore extends PersistentState {

	private final Map<UUID, Integer> lives = new HashMap<>();
	private final Map<UUID, Long> lastLifeLostTime = new HashMap<>();
	private final Map<UUID, Long> lastLifeRegenTime = new HashMap<>();
	private final Map<UUID, Boolean> pendingBan = new HashMap<>();

	public static LivesStore get(MinecraftServer server) {
		PersistentStateManager mgr = server.getOverworld().getPersistentStateManager();
		return mgr.getOrCreate(LivesStore::fromNbt, LivesStore::new, "softhardcore_lives");
	}

	public int getLives(UUID uuid) {
		return lives.computeIfAbsent(uuid, u -> MyConfig.DEFAULT_LIVES);
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
	public NbtCompound writeNbt(NbtCompound nbt) {
		NbtCompound livesNbt = new NbtCompound();
		for (Map.Entry<UUID, Integer> e : lives.entrySet()) {
			livesNbt.put(e.getKey().toString(), NbtInt.of(e.getValue()));
		}
		nbt.put("lives", livesNbt);

		NbtCompound lostNbt = new NbtCompound();
		for (Map.Entry<UUID, Long> e : lastLifeLostTime.entrySet()) {
			lostNbt.put(e.getKey().toString(), NbtLong.of(e.getValue()));
		}
		nbt.put("lastLifeLostTime", lostNbt);

		NbtCompound regenNbt = new NbtCompound();
		for (Map.Entry<UUID, Long> e : lastLifeRegenTime.entrySet()) {
			regenNbt.put(e.getKey().toString(), NbtLong.of(e.getValue()));
		}
		nbt.put("lastLifeRegenTime", regenNbt);

		NbtCompound pendingNbt = new NbtCompound();
		for (Map.Entry<UUID, Boolean> e : pendingBan.entrySet()) {
			pendingNbt.putBoolean(e.getKey().toString(), e.getValue());
		}
		nbt.put("pendingBan", pendingNbt);

		return nbt;
	}

	public static LivesStore fromNbt(NbtCompound nbt) {
		LivesStore store = new LivesStore();
		if (nbt.contains("lives", NbtElement.COMPOUND_TYPE)) {
			NbtCompound livesNbt = nbt.getCompound("lives");
			for (String k : livesNbt.getKeys()) {
				store.lives.put(UUID.fromString(k), livesNbt.getInt(k));
			}
		}
		if (nbt.contains("lastLifeLostTime", NbtElement.COMPOUND_TYPE)) {
			NbtCompound lostNbt = nbt.getCompound("lastLifeLostTime");
			for (String k : lostNbt.getKeys()) {
				store.lastLifeLostTime.put(UUID.fromString(k), lostNbt.getLong(k));
			}
		}
		if (nbt.contains("lastLifeRegenTime", NbtElement.COMPOUND_TYPE)) {
			NbtCompound regenNbt = nbt.getCompound("lastLifeRegenTime");
			for (String k : regenNbt.getKeys()) {
				store.lastLifeRegenTime.put(UUID.fromString(k), regenNbt.getLong(k));
			}
		}
		if (nbt.contains("pendingBan", NbtElement.COMPOUND_TYPE)) {
			NbtCompound pendingNbt = nbt.getCompound("pendingBan");
			for (String k : pendingNbt.getKeys()) {
				store.pendingBan.put(UUID.fromString(k), pendingNbt.getBoolean(k));
			}
		}
		return store;
	}
}


