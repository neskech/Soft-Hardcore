package net.ness.softhardcore.item;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.ness.softhardcore.config.MyConfig;
import net.ness.softhardcore.server.LivesService;

public class LifeHeart extends Item {

    public LifeHeart(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient)
            return TypedActionResult.pass(user.getStackInHand(hand));

        ServerPlayerEntity player = (ServerPlayerEntity) user;
        int livesBefore = net.ness.softhardcore.server.LivesService.getLives(player.getServer(), player.getUuid());
        int livesAfter = net.ness.softhardcore.server.LivesService.increment(player.getServer(), player.getUuid());

        if (livesBefore == livesAfter) {
            Text message = Text.literal("You're already have the maximum number of lives").formatted(Formatting.GREEN);
            player.sendMessage(message);
            return TypedActionResult.pass(user.getStackInHand(hand));
        }
        else {
            Text message = Text.literal("You've gained a life!").formatted(Formatting.GREEN);
            player.sendMessage(message);
            return TypedActionResult.consume(user.getStackInHand(hand));
        }

    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.EAT;  // Make the item behave like food (eaten by right-clicking)
    }
}