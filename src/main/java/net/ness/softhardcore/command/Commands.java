package net.ness.softhardcore.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.ness.softhardcore.component.LivesComponent;
import net.ness.softhardcore.component.MyComponents;
import net.ness.softhardcore.util.LivesCacheManager;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class Commands {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(literal("lives")
            .executes(Commands::livesCommand));
            
        dispatcher.register(literal("setlives")
            .requires(source -> source.hasPermissionLevel(2)) // Admin only (level 2+)
            .then(argument("player", net.minecraft.command.argument.EntityArgumentType.player())
                .then(argument("amount", IntegerArgumentType.integer(1))
                    .executes(Commands::setLivesCommand))));
                    
        dispatcher.register(literal("getlives")
            .then(argument("player", net.minecraft.command.argument.EntityArgumentType.player())
                .executes(Commands::getLivesCommand)));
    }
    
    private static int livesCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        
        LivesComponent component = MyComponents.LIVES_KEY.get(player);
        int lives = component.getLives();
        
        Text message = Text.literal("You have " + lives + " lives remaining.")
            .formatted(Formatting.GREEN);
        source.sendFeedback(() -> message, false);
        
        return lives;
    }
    
    private static int setLivesCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity targetPlayer = net.minecraft.command.argument.EntityArgumentType.getPlayer(context, "player");
        int amount = IntegerArgumentType.getInteger(context, "amount");
        
        LivesComponent component = MyComponents.LIVES_KEY.get(targetPlayer);
        int oldLives = component.getLives();
        
        // Ensure lives are set to at least 1 to avoid triggering ban logic
        int actualAmount = Math.max(amount, 1);
        component.setLives(actualAmount);
        int newLives = component.getLives();
        
        // Update the lives cache for the scoreboard
        LivesCacheManager.updateLivesCache(targetPlayer.getUuid(), newLives);
        
        // Send feedback to the command executor
        String messageText = "Set " + targetPlayer.getName().getString() + "'s lives from " + oldLives + " to " + newLives + ".";
        if (amount < 1) {
            messageText += " (Note: Lives cannot be set below 1 to avoid triggering ban logic)";
        }
        Text message = Text.literal(messageText).formatted(Formatting.GREEN);
        source.sendFeedback(() -> message, true);
        
        // Send message to the target player
        Text playerMessage = Text.literal("Your lives have been set to " + newLives + " by an administrator.")
            .formatted(Formatting.YELLOW);
        targetPlayer.sendMessage(playerMessage, false);
        
        return newLives;
    }
    
    private static int getLivesCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity targetPlayer = net.minecraft.command.argument.EntityArgumentType.getPlayer(context, "player");
        
        LivesComponent component = MyComponents.LIVES_KEY.get(targetPlayer);
        int lives = component.getLives();
        
        Text message = Text.literal(targetPlayer.getName().getString() + " has " + lives + " lives remaining.")
            .formatted(Formatting.GREEN);
        source.sendFeedback(() -> message, false);
        
        return lives;
    }
}
