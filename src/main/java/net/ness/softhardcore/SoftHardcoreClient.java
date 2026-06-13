package net.ness.softhardcore;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ness.softhardcore.util.LivesCacheManager;

@Mod(value = SoftHardcore.MOD_ID, dist = Dist.CLIENT)
public class SoftHardcoreClient {
    private static Minecraft CLIENT;

    public SoftHardcoreClient() {
        NeoForge.EVENT_BUS.register(this);
    }

    public static Minecraft getClient() {
        return CLIENT != null ? CLIENT : Minecraft.getInstance();
    }

    @SubscribeEvent
    public void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        CLIENT = null;
        LivesCacheManager.clearLivesCache();
    }
}
