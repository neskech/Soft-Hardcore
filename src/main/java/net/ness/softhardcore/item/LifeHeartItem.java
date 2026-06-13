package net.ness.softhardcore.item;

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ness.softhardcore.config.MyConfig;
import net.ness.softhardcore.server.LivesService;

public class LifeHeartItem extends Item {

    public LifeHeartItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);

        if (!level.isClientSide()) {
            if (LivesService.getLives(level.getServer(), user.getUUID()) >= MyConfig.DEFAULT_LIVES) {
                user.sendSystemMessage(Component.literal("You already have maximum lives!").withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(itemStack);
            }

            int livesToGive = MyConfig.LIVES_GAINED_FROM_HEART;
            int currentLives = LivesService.getLives(level.getServer(), user.getUUID());
            int newLives = Math.min(currentLives + livesToGive, MyConfig.DEFAULT_LIVES);
            int actualLivesGained = newLives - currentLives;

            LivesService.setLives(level.getServer(), user.getUUID(), newLives);

            level.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);

            String message = actualLivesGained == 1 ?
                    "You gained 1 life!" :
                    "You gained " + actualLivesGained + " lives!";
            user.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.GREEN));

            if (!user.getAbilities().instabuild) {
                itemStack.shrink(1);
            }

            return InteractionResultHolder.success(itemStack);
        }

        return InteractionResultHolder.pass(itemStack);
    }
}
