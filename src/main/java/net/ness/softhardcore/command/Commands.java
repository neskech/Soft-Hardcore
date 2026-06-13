package net.ness.softhardcore.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.ness.softhardcore.config.MyConfig;
import net.ness.softhardcore.server.LivesService;
import net.ness.softhardcore.server.LivesStore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class Commands {

    private static final SuggestionProvider<CommandSourceStack> CONFIG_KEY_SUGGESTIONS = (context, builder) -> {
        String input = builder.getRemaining().toLowerCase();

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
                MyConfig.KEY_LIFE_REGEN_CEILING,
                MyConfig.KEY_INVENTORY_LIVES_OVERLAY
        };

        for (String key : configKeys) {
            if (key.toLowerCase().startsWith(input)) {
                builder.suggest(key);
            }
        }

        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(net.minecraft.commands.Commands.literal("softhardcore")
                .then(net.minecraft.commands.Commands.literal("setlives")
                        .requires(source -> source.hasPermission(2))
                        .then(net.minecraft.commands.Commands.argument("player", EntityArgument.player())
                                .then(net.minecraft.commands.Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(Commands::setLivesCommand))))
                .then(net.minecraft.commands.Commands.literal("setalllives")
                        .requires(source -> source.hasPermission(2))
                        .then(net.minecraft.commands.Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(Commands::setAllLivesCommand)))
                .then(net.minecraft.commands.Commands.literal("getlives")
                        .executes(Commands::getLivesCommandSelf)
                        .then(net.minecraft.commands.Commands.argument("player", EntityArgument.player())
                                .executes(Commands::getLivesCommand)))
                .then(net.minecraft.commands.Commands.literal("getConfig")
                        .requires(source -> source.hasPermission(2))
                        .executes(Commands::showConfig))
                .then(net.minecraft.commands.Commands.literal("setconfig")
                        .requires(source -> source.hasPermission(2))
                        .then(net.minecraft.commands.Commands.argument("key", StringArgumentType.string())
                                .suggests(CONFIG_KEY_SUGGESTIONS)
                                .then(net.minecraft.commands.Commands.argument("value", StringArgumentType.greedyString())
                                        .executes(Commands::setConfigCommand))))
                .then(net.minecraft.commands.Commands.literal("reloadconfig")
                        .requires(source -> source.hasPermission(2))
                        .executes(Commands::reloadConfigCommand))
                .then(net.minecraft.commands.Commands.literal("unban")
                        .requires(source -> source.hasPermission(2))
                        .then(net.minecraft.commands.Commands.argument("player", GameProfileArgument.gameProfile())
                                .executes(Commands::unbanCommand))));
    }

    private static int getLivesCommandSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        int lives = LivesService.getLives(source.getServer(), player.getUUID());

        Component message = Component.literal("You have " + lives + " lives remaining.")
                .withStyle(ChatFormatting.GREEN);
        source.sendSuccess(() -> message, false);

        return lives;
    }

    private static int setLivesCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        int amount = IntegerArgumentType.getInteger(context, "amount");

        int oldLives = LivesService.getLives(source.getServer(), targetPlayer.getUUID());

        int actualAmount = Math.max(amount, 1);
        int newLives = LivesService.setLives(source.getServer(), targetPlayer.getUUID(), actualAmount);

        String messageText = "Set " + targetPlayer.getName().getString() + "'s lives from " + oldLives + " to " + newLives + ".";
        if (amount < 1) {
            messageText += " (Note: Lives cannot be set below 1 to avoid triggering ban logic)";
        }
        Component message = Component.literal(messageText).withStyle(ChatFormatting.GREEN);
        source.sendSuccess(() -> message, true);

        Component playerMessage = Component.literal("Your lives have been set to " + newLives + " by an administrator.")
                .withStyle(ChatFormatting.YELLOW);
        targetPlayer.sendSystemMessage(playerMessage);

        return newLives;
    }

    private static int getLivesCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");

        int lives = LivesService.getLives(source.getServer(), targetPlayer.getUUID());

        Component message = Component.literal(targetPlayer.getName().getString() + " has " + lives + " lives remaining.")
                .withStyle(ChatFormatting.GREEN);
        source.sendSuccess(() -> message, false);

        return lives;
    }

    private static int showConfig(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        source.sendSuccess(() -> Component.literal("=== SoftHardcore Configuration ===").withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal("Default Lives: " + MyConfig.DEFAULT_LIVES), false);
        source.sendSuccess(() -> Component.literal("Ban Duration: " + MyConfig.BAN_DURATION), false);
        source.sendSuccess(() -> Component.literal("Lives Dropped on Death: " + MyConfig.LIVES_DROPPED), false);
        source.sendSuccess(() -> Component.literal("Heart Drop Mode: " + MyConfig.HEART_DROP_MODE), false);
        source.sendSuccess(() -> Component.literal("Life Regeneration Cooldown: " + MyConfig.LIFE_REGEN_COOLDOWN), false);
        source.sendSuccess(() -> Component.literal("Returning Lives: " + MyConfig.RETURNING_LIVES), false);
        source.sendSuccess(() -> Component.literal("Lives Gained from Heart: " + MyConfig.LIVES_GAINED_FROM_HEART), false);
        source.sendSuccess(() -> Component.literal("Max Scoreboard Rows: " + MyConfig.MAX_SCOREBOARD_ROWS), false);
        source.sendSuccess(() -> Component.literal("Passive Death Heart Drop Probability: " + MyConfig.PASSIVE_DEATH_HEART_DROP_PROBABILITY), false);
        source.sendSuccess(() -> Component.literal("Player Death Heart Drop Probability: " + MyConfig.PLAYER_DEATH_HEART_DROP_PROBABILITY), false);
        source.sendSuccess(() -> Component.literal("Life Regeneration Ceiling: " + MyConfig.LIFE_REGEN_CEILING), false);
        source.sendSuccess(() -> Component.literal("Inventory Lives Overlay: " + MyConfig.INVENTORY_LIVES_OVERLAY_ENABLED), false);

        return 1;
    }

    private static int setConfigCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String key = StringArgumentType.getString(context, "key");
        String value = StringArgumentType.getString(context, "value");

        if (!isValidConfigKey(key)) {
            Component message = Component.literal("Invalid config key: " + key).withStyle(ChatFormatting.RED);
            source.sendSuccess(() -> message, false);
            return 0;
        }

        String validationError = validateConfigValue(key, value);
        if (validationError != null) {
            Component message = Component.literal("Invalid value for " + key + ": " + validationError).withStyle(ChatFormatting.RED);
            source.sendSuccess(() -> message, false);
            return 0;
        }

        boolean success = MyConfig.setConfigValue(key, value);
        if (success) {
            MyConfig.reloadConfig();
            Component message = Component.literal("Set " + key + " = " + value + " and reloaded config").withStyle(ChatFormatting.GREEN);
            source.sendSuccess(() -> message, true);
            return 1;
        } else {
            Component message = Component.literal("Failed to set config value").withStyle(ChatFormatting.RED);
            source.sendSuccess(() -> message, false);
            return 0;
        }
    }

    private static int reloadConfigCommand(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        MyConfig.reloadConfig();
        Component message = Component.literal("Config reloaded successfully").withStyle(ChatFormatting.GREEN);
        source.sendSuccess(() -> message, true);

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
                key.equals(MyConfig.KEY_LIFE_REGEN_CEILING) ||
                key.equals(MyConfig.KEY_INVENTORY_LIVES_OVERLAY);
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
                    java.time.Duration.parse(value);
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

                case MyConfig.KEY_INVENTORY_LIVES_OVERLAY:
                    if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                        return "Must be true or false";
                    }
                    break;
            }
            return null;
        } catch (Exception e) {
            return "Invalid format: " + e.getMessage();
        }
    }

    private static int setAllLivesCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        int amount = IntegerArgumentType.getInteger(context, "amount");

        if (amount < 0 || amount > MyConfig.DEFAULT_LIVES) {
            source.sendFailure(Component.literal("Amount must be between 0 and " + MyConfig.DEFAULT_LIVES));
            return 0;
        }

        MinecraftServer server = source.getServer();
        int onlineCount = 0;
        int offlineCount = 0;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            LivesService.setLives(server, player.getUUID(), amount);
            onlineCount++;
        }

        try {
            Path playerDataPath = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
            if (Files.isDirectory(playerDataPath)) {
                try (var stream = Files.list(playerDataPath)) {
                    List<Path> files = stream.filter(p -> p.getFileName().toString().endsWith(".dat")).toList();
                    for (Path p : files) {
                        String name = p.getFileName().toString();
                        String uuidStr = name.substring(0, name.length() - 4);
                        try {
                            UUID uuid = UUID.fromString(uuidStr);
                            if (server.getPlayerList().getPlayer(uuid) == null) {
                                LivesStore.get(server).setLives(uuid, amount);
                                offlineCount++;
                            }
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            }
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error accessing offline player data: " + e.getMessage()));
            return 0;
        }

        String message = String.format("Set lives to %d for %d online players and %d offline players",
                amount, onlineCount, offlineCount);
        source.sendSuccess(() -> Component.literal(message).withStyle(ChatFormatting.GREEN), true);

        return 1;
    }

    private static int unbanCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();

        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(context, "player");

        if (profiles.isEmpty()) {
            source.sendFailure(Component.literal("No player found"));
            return 0;
        }

        GameProfile targetProfile = profiles.iterator().next();
        String targetName = targetProfile.getName();
        UUID targetUuid = LivesService.resolvePlayerUuid(server, targetProfile);

        ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(targetUuid);
        int oldLives = LivesStore.get(server).peekLives(targetUuid);

        if (oldLives < 0) {
            source.sendFailure(Component.literal(targetName + " has no lives record on this server."));
            return 0;
        }

        if (oldLives > 0) {
            source.sendFailure(Component.literal(targetName + " is not banned (has " + oldLives + " lives)."));
            return 0;
        }

        if (!LivesService.unbanPlayer(server, targetUuid)) {
            source.sendFailure(Component.literal("Failed to unban " + targetName + "."));
            return 0;
        }

        int newLives = LivesService.getLives(server, targetUuid);
        String messageText = "Unbanned " + targetName + ". Lives set from " + oldLives + " to " + newLives + ".";
        Component message = Component.literal(messageText).withStyle(ChatFormatting.GREEN);
        source.sendSuccess(() -> message, true);

        if (onlinePlayer != null) {
            Component playerMessage = Component.literal("You have been unbanned! Your lives have been restored to " + newLives + ".")
                    .withStyle(ChatFormatting.GREEN);
            onlinePlayer.sendSystemMessage(playerMessage);
        }

        return 1;
    }
}
