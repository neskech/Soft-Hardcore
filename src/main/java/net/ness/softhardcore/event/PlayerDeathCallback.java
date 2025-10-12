package net.ness.softhardcore.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

public interface PlayerDeathCallback {
    Event<PlayerDeathCallback> EVENT = EventFactory.createArrayBacked(PlayerDeathCallback.class,
            (listeners) -> (player, damageSource) -> {
                for (PlayerDeathCallback listener : listeners) {
                    ActionResult result = listener.interact(player, damageSource);

                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }

                return ActionResult.PASS;
    });

    ActionResult interact(ServerPlayerEntity player, DamageSource damageSource);
}
