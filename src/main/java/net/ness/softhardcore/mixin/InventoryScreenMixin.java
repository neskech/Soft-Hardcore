package net.ness.softhardcore.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.ness.softhardcore.ui.InventoryLivesOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    @Inject(method = "renderBg", at = @At("TAIL"))
    private void softhardcore$renderLivesOverlay(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        HandledScreenAccessor screen = (HandledScreenAccessor) this;
        InventoryLivesOverlay.render(
                graphics,
                Minecraft.getInstance(),
                screen.softhardcore$getGuiX(),
                screen.softhardcore$getGuiY()
        );
    }
}
