package net.ness.softhardcore.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.ness.softhardcore.server.LivesService;
import net.ness.softhardcore.config.MyConfig;


import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class Commands {
    
    // Suggestion provider for config keys
    private static final SuggestionProvider<ServerCommandSource> CONFIG_KEY_SUGGESTIONS = (context, builder) -> {
        String input = builder.getRemaining().toLowerCase();
        
        // List of all valid config keys
        String[] configKeys = {
            MyConfig.KEY_DEFAULT_LIVES,
            MyConfig.KEY_BAN_DURATION,
            MyConfig.KEY_LIVES_DROPPED,
            MyConfig.KEY_HEART_DROP_MODE,
            MyConfig.KEY_LIFE_REGEN_COOLDOWN,
            MyConfig.KEY_RETURNING_LIVES,
            MyConfig.KEY_LIVES_GAINED_FROM_HEART,
            MyConfig.KEY_SCOREBOARD_MAX_ROWS,
            MyConfig.KEY_PASSIVE_DEATH_HEART_DROP_PROBABILITY,
            MyConfig.KEY_PLAYER_DEATH_HEART_DROP_PROBABILITY,
            MyConfig.KEY_LIFE_REGEN_CEILING
        };
        
        // Filter and suggest matching keys
        for (String key : configKeys) {
            if (key.toLowerCase().startsWith(input)) {
                builder.suggest(key);
            }
        }
        
        return builder.buildFuture();
    };
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(literal("softhardcore")
            .then(literal("setlives")
                .requires(source -> source.hasPermissionLevel(2)) // Admin only (level 2+)
                .then(argument("player", net.minecraft.command.argument.EntityArgumentType.player())
                    .then(argument("amount", IntegerArgumentType.integer(1))
                        .executes(Commands::setLivesCommand))))
            .then(literal("setalllives")
                .requires(source -> source.hasPermissionLevel(2)) // Admin only (level 2+)
                .then(argument("amount", IntegerArgumentType.integer(1))
                    .executes(Commands::setAllLivesCommand)))
            .then(literal("getlives")
                .executes(Commands::getLivesCommandSelf)
                .then(argument("player", net.minecraft.command.argument.EntityArgumentType.player())
                    .executes(Commands::getLivesCommand)))
            .then(literal("getConfig")
                .requires(source -> source.hasPermissionLevel(2)) // Admin only (level 2+)
                .executes(Commands::showConfig))
            .then(literal("setconfig")
                .requires(source -> source.hasPermissionLevel(2)) // Admin only
                .then(argument("key", StringArgumentType.string())
                    .suggests(CONFIG_KEY_SUGGESTIONS)
                    .then(argument("value", StringArgumentType.greedyString())
                        .executes(Commands::setConfigCommand))))
            .then(literal("reloadconfig")
                .requires(source -> source.hasPermissionLevel(2)) // Admin only
                .executes(Commands::reloadConfigCommand)));
    }
    
    private static int getLivesCommandSelf(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        
        int lives = LivesService.getLives(source.getServer(), player.getUuid());
        
        Text message = Text.literal("You have " + lives + " lives remaining.")
            .formatted(Formatting.GREEN);
        source.sendFeedback(() -> message, false);
        
        return lives;
    }
    
    private static int setLivesCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity targetPlayer = net.minecraft.command.argument.EntityArgumentType.getPlayer(context, "player");
        int amount = IntegerArgumentType.getInteger(context, "amount");
        
        int oldLives = LivesService.getLives(source.getServer(), targetPlayer.getUuid());
        
        // Ensure lives are set to at least 1 to avoid triggering ban logic
        int actualAmount = Math.max(amount, 1);
        int newLives = LivesService.setLives(source.getServer(), targetPlayer.getUuid(), actualAmount);
        
        // Server broadcasts handle client cache updates
        
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
        
        int lives = LivesService.getLives(source.getServer(), targetPlayer.getUuid());
        
        Text message = Text.literal(targetPlayer.getName().getString() + " has " + lives + " lives remaining.")
            .formatted(Formatting.GREEN);
        source.sendFeedback(() -> message, false);
        
        return lives;
    }
    
    private static int showConfig(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        // Send header
        source.sendFeedback(() -> Text.literal("=== SoftHardcore Configuration ===").formatted(Formatting.GOLD), false);
        
        // Send all config values
        source.sendFeedback(() -> Text.literal("Default Lives: " + MyConfig.DEFAULT_LIVES).formatted(Formatting.WHITE), false);
        source.sendFeedback(() -> Text.literal("Ban Duration: " + MyConfig.BAN_DURATION.toString()).formatted(Formatting.WHITE), false);
        source.sendFeedback(() -> Text.literal("Lives Dropped on Death: " + MyConfig.LIVES_DROPPED).formatted(Formatting.WHITE), false);
        source.sendFeedback(() -> Text.literal("Heart Drop Mode: " + MyConfig.HEART_DROP_MODE.toString()).formatted(Formatting.WHITE), false);
        source.sendFeedback(() -> Text.literal("Life Regeneration Cooldown: " + MyConfig.LIFE_REGEN_COOLDOWN.toString()).formatted(Formatting.WHITE), false);
        source.sendFeedback(() -> Text.literal("Returning Lives: " + MyConfig.RETURNING_LIVES).formatted(Formatting.WHITE), false);
        source.sendFeedback(() -> Text.literal("Lives Gained from Heart: " + MyConfig.LIVES_GAINED_FROM_HEART).formatted(Formatting.WHITE), false);
        source.sendFeedback(() -> Text.literal("Max Scoreboard Rows: " + MyConfig.MAX_SCOREBOARD_ROWS).formatted(Formatting.WHITE), false);
        source.sendFeedback(() -> Text.literal("Passive Death Heart Drop Probability: " + MyConfig.PASSIVE_DEATH_HEART_DROP_PROBABILITY).formatted(Formatting.WHITE), false);
        source.sendFeedback(() -> Text.literal("Player Death Heart Drop Probability: " + MyConfig.PLAYER_DEATH_HEART_DROP_PROBABILITY).formatted(Formatting.WHITE), false);
        source.sendFeedback(() -> Text.literal("Life Regeneration Ceiling: " + MyConfig.LIFE_REGEN_CEILING).formatted(Formatting.WHITE), false);
        
        return 1;
    }
    
    private static int setConfigCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        String key = StringArgumentType.getString(context, "key");
        String value = StringArgumentType.getString(context, "value");
        
        // Validate the key exists
        if (!isValidConfigKey(key)) {
            Text message = Text.literal("Invalid config key: " + key).formatted(Formatting.RED);
            source.sendFeedback(() -> message, false);
            return 0;
        }
        
        // Validate the value based on key type
        String validationError = validateConfigValue(key, value);
        if (validationError != null) {
            Text message = Text.literal("Invalid value for " + key + ": " + validationError).formatted(Formatting.RED);
            source.sendFeedback(() -> message, false);
            return 0;
        }
        
        // Set the config value
        boolean success = MyConfig.setConfigValue(key, value);
        if (success) {
            // Reload config to apply changes
            MyConfig.reloadConfig();
            
            Text message = Text.literal("Set " + key + " = " + value + " and reloaded config").formatted(Formatting.GREEN);
            source.sendFeedback(() -> message, true);
            return 1;
        } else {
            Text message = Text.literal("Failed to set config value").formatted(Formatting.RED);
            source.sendFeedback(() -> message, false);
            return 0;
        }
    }
    
    private static int reloadConfigCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        MyConfig.reloadConfig();
        Text message = Text.literal("Config reloaded successfully").formatted(Formatting.GREEN);
        source.sendFeedback(() -> message, true);
        
        return 1;
    }
    
    private static boolean isValidConfigKey(String key) {
        return key.equals(MyConfig.KEY_DEFAULT_LIVES) ||
               key.equals(MyConfig.KEY_BAN_DURATION) ||
               key.equals(MyConfig.KEY_LIVES_DROPPED) ||
               key.equals(MyConfig.KEY_HEART_DROP_MODE) ||
               key.equals(MyConfig.KEY_LIFE_REGEN_COOLDOWN) ||
               key.equals(MyConfig.KEY_RETURNING_LIVES) ||
               key.equals(MyConfig.KEY_LIVES_GAINED_FROM_HEART) ||
               key.equals(MyConfig.KEY_SCOREBOARD_MAX_ROWS) ||
               key.equals(MyConfig.KEY_PASSIVE_DEATH_HEART_DROP_PROBABILITY) ||
               key.equals(MyConfig.KEY_PLAYER_DEATH_HEART_DROP_PROBABILITY) ||
               key.equals(MyConfig.KEY_LIFE_REGEN_CEILING);
    }
    
    private static String validateConfigValue(String key, String value) {
        try {
            switch (key) {
                case MyConfig.KEY_DEFAULT_LIVES:
                case MyConfig.KEY_LIVES_DROPPED:
                case MyConfig.KEY_RETURNING_LIVES:
                case MyConfig.KEY_LIVES_GAINED_FROM_HEART:
                case MyConfig.KEY_SCOREBOARD_MAX_ROWS:
                case MyConfig.KEY_LIFE_REGEN_CEILING:
                    int intVal = Integer.parseInt(value);
                    if (key.equals(MyConfig.KEY_DEFAULT_LIVES) && (intVal < 1 || intVal > 100)) {
                        return "Must be between 1 and 100";
                    }
                    if (key.equals(MyConfig.KEY_SCOREBOARD_MAX_ROWS) && (intVal < 1 || intVal > 20)) {
                        return "Must be between 1 and 20";
                    }
                    if ((key.equals(MyConfig.KEY_LIVES_DROPPED) || key.equals(MyConfig.KEY_LIVES_GAINED_FROM_HEART)) && intVal < 0) {
                        return "Must be 0 or greater";
                    }
                    if (key.equals(MyConfig.KEY_RETURNING_LIVES) && intVal < 1) {
                        return "Must be 1 or greater";
                    }
                    if (key.equals(MyConfig.KEY_LIFE_REGEN_CEILING) && (intVal < 1 || intVal > MyConfig.DEFAULT_LIVES)) {
                        return "Must be between 1 and " + MyConfig.DEFAULT_LIVES;
                    }
                    break;
                    
                case MyConfig.KEY_BAN_DURATION:
                case MyConfig.KEY_LIFE_REGEN_COOLDOWN:
                    java.time.Duration.parse(value); // Will throw if invalid
                    break;
                    
                case MyConfig.KEY_HEART_DROP_MODE:
                    String upperValue = value.toUpperCase();
                    if (!upperValue.equals("PASSIVE") && !upperValue.equals("NEUTRAL") && 
                        !upperValue.equals("TEAM") && !upperValue.equals("COMPETITIVE") && 
                        !upperValue.equals("VENGEFUL") && !upperValue.equals("NEVER")) {
                        return "Must be one of: PASSIVE, NEUTRAL, TEAM, COMPETITIVE, VENGEFUL, NEVER";
                    }
                    break;
                    
                case MyConfig.KEY_PASSIVE_DEATH_HEART_DROP_PROBABILITY:
                case MyConfig.KEY_PLAYER_DEATH_HEART_DROP_PROBABILITY:
                    double doubleVal = Double.parseDouble(value);
                    if (doubleVal < 0.0 || doubleVal > 1.0) {
                        return "Must be between 0.0 and 1.0";
                    }
                    break;
            }
            return null; // No validation error
        } catch (Exception e) {
            return "Invalid format: " + e.getMessage();
        }
    }
    
    private static int setAllLivesCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        int amount = IntegerArgumentType.getInteger(context, "amount");
        
        if (amount < 0 || amount > MyConfig.DEFAULT_LIVES) {
            source.sendError(Text.literal("Amount must be between 0 and " + MyConfig.DEFAULT_LIVES));
            return 0;
        }
        
        MinecraftServer server = source.getServer();
        int onlineCount = 0;
        int offlineCount = 0;
        
        // Set lives for all online players
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            LivesService.setLives(server, player.getUuid(), amount);
            onlineCount++;
        }
        
        // Set lives for all offline players by scanning playerdata files
        try {
            java.nio.file.Path runDir = server.getRunDirectory().toPath();
            java.nio.file.Path worldDir = runDir.resolve(server.getSaveProperties().getLevelName());
            java.nio.file.Path playerDataPath = worldDir.resolve("playerdata");
            if (java.nio.file.Files.isDirectory(playerDataPath)) {
                try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.list(playerDataPath)) {
                    java.util.List<java.nio.file.Path> files = stream.filter(p -> p.getFileName().toString().endsWith(".dat")).toList();
                    for (java.nio.file.Path p : files) {
                        String name = p.getFileName().toString();
                        String uuidStr = name.substring(0, name.length() - 4);
                        try {
                            java.util.UUID uuid = java.util.UUID.fromString(uuidStr);
                            if (server.getPlayerManager().getPlayer(uuid) == null) {
                                net.ness.softhardcore.server.LivesStore.get(server).setLives(uuid, amount);
                                offlineCount++;
                            }
                        } catch (IllegalArgumentException ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            source.sendError(Text.literal("Error accessing offline player data: " + e.getMessage()));
            return 0;
        }
        
        // Send confirmation message
        String message = String.format("Set lives to %d for %d online players and %d offline players", 
                                     amount, onlineCount, offlineCount);
        source.sendFeedback(() -> Text.literal(message).formatted(Formatting.GREEN), true);
        
        return 1;
    }
}
