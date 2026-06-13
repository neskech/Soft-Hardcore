package net.ness.softhardcore.server;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Uuids;
import net.ness.softhardcore.config.MyConfig;
import net.ness.softhardcore.networking.NetworkManager;

import java.util.Map;
import java.util.UUID;

public class LivesService {

    public static int getLives(MinecraftServer server, UUID uuid) {
        return LivesStore.get(server).getLives(uuid);
    }

    public static int setLives(MinecraftServer server, UUID uuid, int amount) {
        LivesStore store = LivesStore.get(server);
        int previous = store.getLives(uuid);
        int clamped = Math.max(Math.min(amount, MyConfig.DEFAULT_LIVES), 0);
        store.setLives(uuid, clamped);
        if (clamped < previous) {
            store.setLastLifeLostTime(uuid, System.currentTimeMillis());
        }
        boolean wentBelowRegenCeiling = previous >= MyConfig.LIFE_REGEN_CEILING && clamped < MyConfig.LIFE_REGEN_CEILING;
        if (clamped == MyConfig.DEFAULT_LIVES || wentBelowRegenCeiling) {
            store.setLastLifeRegenTime(uuid, 0);
        }
        NetworkManager.broadcastLivesOne(server, uuid, clamped);
        return clamped;
    }

    public static int decrement(MinecraftServer server, UUID uuid) {
        LivesStore store = LivesStore.get(server);
        int current = store.getLives(uuid);
        return setLives(server, uuid, current - 1);
    }

    public static int increment(MinecraftServer server, UUID uuid) {
        LivesStore store = LivesStore.get(server);
        int current = store.getLives(uuid);
        return setLives(server, uuid, current + 1);
    }

    public static void markBanStart(MinecraftServer server, UUID uuid) {
        LivesStore store = LivesStore.get(server);
        store.setLastLifeLostTime(uuid, System.currentTimeMillis());
    }

    public static void markBanEnd(MinecraftServer server, UUID uuid) {
        clearBanState(server, uuid);
    }

    public static void clearBanState(MinecraftServer server, UUID uuid) {
        LivesStore store = LivesStore.get(server);
        store.setLastLifeLostTime(uuid, 0);
        store.setLastLifeRegenTime(uuid, 0);
    }

    /**
     * Fully lifts a ban for a player with 0 lives. Returns false if the player is not banned.
     */
    public static boolean unbanPlayer(MinecraftServer server, UUID uuid) {
        LivesStore store = LivesStore.get(server);
        if (!store.hasRecord(uuid) || store.peekLives(uuid) != 0) {
            return false;
        }

        setPendingBan(server, uuid, false);
        clearBanState(server, uuid);
        setLives(server, uuid, MyConfig.RETURNING_LIVES);
        return true;
    }

    public static UUID resolvePlayerUuid(MinecraftServer server, GameProfile profile) {
        LivesStore store = LivesStore.get(server);
        String name = profile.getName();

        if (name != null) {
            ServerPlayerEntity online = server.getPlayerManager().getPlayer(name);
            if (online != null) {
                return online.getUuid();
            }

            UUID cached = server.getUserCache()
                    .findByName(name)
                    .map(GameProfile::getId)
                    .orElse(null);
            UUID offline = Uuids.getOfflinePlayerUuid(name);

            if (cached != null && store.hasRecord(cached)) {
                return cached;
            }
            if (store.hasRecord(offline)) {
                return offline;
            }
            return cached != null ? cached : offline;
        }

        if (profile.getId() != null) {
            return profile.getId();
        }
        throw new IllegalArgumentException("Cannot resolve UUID for profile without a name");
    }

    public static boolean isPendingBan(MinecraftServer server, UUID uuid) {
        return LivesStore.get(server).isPendingBan(uuid);
    }

    public static void setPendingBan(MinecraftServer server, UUID uuid, boolean value) {
        LivesStore.get(server).setPendingBan(uuid, value);
    }

    public static int tryRegenerate(MinecraftServer server, UUID uuid) {
        LivesStore store = LivesStore.get(server);
        int currentLives = store.getLives(uuid);
        if (currentLives >= MyConfig.LIFE_REGEN_CEILING || store.isPendingBan(uuid)) {
            return 0;
        }
        long currentTime = System.currentTimeMillis();
        long defaultCooldownMillis = 24 * 60 * 60 * 1000L;
        long cooldownMillis = MyConfig.LIFE_REGEN_COOLDOWN != null ? MyConfig.LIFE_REGEN_COOLDOWN.toMillis() : defaultCooldownMillis;
        long lastRegen = store.getLastLifeRegenTime(uuid);
        long anchorTime = lastRegen > 0 ? lastRegen : store.getLastLifeLostTime(uuid);
        int regenAmount = 1;
        if (anchorTime > 0) {
            long timeSince = currentTime - anchorTime;
            regenAmount = (int) (timeSince / cooldownMillis);
            regenAmount = Math.min(regenAmount, MyConfig.LIFE_REGEN_CEILING - currentLives);
        }
        if (regenAmount <= 0) {
            return 0;
        }
        int newLives = Math.min(currentLives + regenAmount, MyConfig.LIFE_REGEN_CEILING);
        store.setLives(uuid, newLives);
        store.setLastLifeRegenTime(uuid, System.currentTimeMillis());
        NetworkManager.broadcastLivesOne(server, uuid, newLives);
        return regenAmount;
    }

    public static void broadcastBulkTo(ServerPlayerEntity target) {
        MinecraftServer server = target.getServer();
        if (server == null) {
            return;
        }
        LivesStore store = LivesStore.get(server);
        Map<UUID, Integer> snapshot = store.snapshotLives();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            snapshot.putIfAbsent(player.getUuid(), store.getLives(player.getUuid()));
        }
        NetworkManager.sendLivesBulkTo(target, snapshot);
    }
}
