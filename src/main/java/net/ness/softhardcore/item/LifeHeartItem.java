package net.ness.softhardcore.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.ness.softhardcore.config.MyConfig;

public class LifeHeartItem extends Item {
    
    public LifeHeartItem(Settings settings) {
        super(settings);
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        
        if (!world.isClient) {
            // Check if the player is at max lives
            if (net.ness.softhardcore.server.LivesService.getLives(world.getServer(), user.getUuid()) >= MyConfig.DEFAULT_LIVES) {
                user.sendMessage(Text.literal("You already have maximum lives!").formatted(Formatting.RED), false);
                return new TypedActionResult<>(ActionResult.FAIL, itemStack);
            }
            
            // Calculate how many lives to give
            int livesToGive = MyConfig.LIVES_GAINED_FROM_HEART;
            int currentLives = net.ness.softhardcore.server.LivesService.getLives(world.getServer(), user.getUuid());
            int newLives = Math.min(currentLives + livesToGive, MyConfig.DEFAULT_LIVES);
            int actualLivesGained = newLives - currentLives;

            // Give the lives via centralized setLives logic
            net.ness.softhardcore.server.LivesService.setLives(world.getServer(), user.getUuid(), newLives);
            
            // Play sound effect
            world.playSound(null, user.getX(), user.getY(), user.getZ(), 
                SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.2f);
            
            // Send message to player
            String message = actualLivesGained == 1 ? 
                "You gained 1 life!" : 
                "You gained " + actualLivesGained + " lives!";
            user.sendMessage(Text.literal(message).formatted(Formatting.GREEN), false);
            
            // Server broadcast updates client caches
            
            // Consume the item
            if (!user.getAbilities().creativeMode) {
                itemStack.decrement(1);
            }
            
            return new TypedActionResult<>(ActionResult.SUCCESS, itemStack);
        }
        
        return new TypedActionResult<>(ActionResult.PASS, itemStack);
    }
}
