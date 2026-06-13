package net.ness.softhardcore.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.ness.softhardcore.ui.InventoryLivesOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    @Inject(method = "drawBackground", at = @At("TAIL"))
    private void softhardcore$renderLivesOverlay(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        HandledScreenAccessor screen = (HandledScreenAccessor) this;
        InventoryLivesOverlay.render(
                context,
                net.minecraft.client.MinecraftClient.getInstance(),
                screen.softhardcore$getGuiX(),
                screen.softhardcore$getGuiY()
        );
    }
}
