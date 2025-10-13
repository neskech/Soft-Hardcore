# SoftHardcore

A soft-hardcore, highly configurable Minecraft mod that gives each player a limited number of lives. Once players run out of lives, they are temporarily banned for a configurable amount of time. Players can drop life hearts to give other players back lives, creating a cooperative survival experience.

The mod also provides some minor UI reworks, lives and armor information now appear on top of nameplates and the number of lives reminaing on a player while checking the scoreboard, compatable with teams, alongside a couple of other minor scoreboard enhancements.


## ✨ Features

- **Limited Lives System**: Each player starts with a configurable number of lives
- **Temporary Bans**: Players are temporarily banned when they run out of lives
- **Life Hearts**: Players can drop life hearts to restore lives to others
- **Automatic Life Regeneration**: Lives can regenerate over time (configurable)
- **Multiple Interval Regeneration**: If multiple cooldown periods pass, all pending regenerations are applied
- **Team-Based Mechanics**: Support for team-based heart dropping
- **Scoreboard Integration**: Lives are displayed on the player list
- **HUD Overlay**: Shows remaining lives in-game
- **Admin Commands**: Full administrative control over player lives
- **Runtime Configuration**: Modify config values in-game with autocomplete
- **Bulk Operations**: Set lives for all players (online and offline) at once
- **Human-Readable Durations**: Ban messages show friendly time formats
- **Highly Configurable**: Extensive configuration options for all aspects

## 🆕 Recent Improvements

### Enhanced Life Regeneration
- **Multiple Interval Support**: If 3 hours pass and the cooldown is 1 hour, players get 3 regenerations at once
- **Automatic Timestamp Management**: Life lost and regeneration times are automatically tracked
- **Improved Logic**: More reliable regeneration system with proper anchor time handling

### Runtime Configuration System
- **In-Game Config Editing**: Modify any config value without server restart
- **Autocomplete Support**: Tab completion for all config keys
- **Validation**: Input validation with helpful error messages
- **Persistence**: Changes are saved to the config file immediately

### Enhanced Commands
- **Unified Command Structure**: All commands prefixed with `/softhardcore`
- **Bulk Operations**: Set lives for all players (online and offline) at once
- **Better Feedback**: Detailed confirmation messages for all operations
- **Admin Tools**: Comprehensive admin command suite

### Improved User Experience
- **Human-Readable Durations**: Ban messages show "2 days, 3 hours" instead of raw seconds
- **Better Config Format**: Comments above config lines for cleaner editing
- **Robust Error Handling**: Graceful handling of invalid config values
- **Enhanced Feedback**: Clear success/error messages for all operations

### 🚀 Dependencies

This mod requires the following dependencies to function properly:

| Dependency | Version | Description |
|------------|---------|-------------|
| **Cardinal Components API** | 5.2.3 | Used for the lives component system |



## ⚙️ Configuration

The mod creates a configuration file at `config/softhardcoreconfig.properties` when first run. You can edit this file to customize the mod's behavior, or use the in-game commands to modify settings at runtime.

### Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `default.lives` | Integer | `25` | Number of lives each player starts with |
| `ban.duration` | Duration | `P2D` | How long players are banned when they run out of lives (ISO-8601 format) |
| `lives.dropped.on.death` | Integer | `1` | Number of life hearts dropped when a player dies |
| `heart.drop.mode` | String | `NEUTRAL` | When life hearts are dropped (see [Heart Drop Modes](#heart-drop-modes)) |
| `life.regen.cooldown` | Duration | `PT24H` | Time between automatic life regeneration (ISO-8601 format) |
| `returning.lives` | Integer | `5` | Number of lives given to players when their ban expires |
| `lives.gained.from.heart` | Integer | `1` | Number of lives gained when consuming a life heart |
| `scoreboard.max.rows` | Integer | `5` | Maximum number of players shown on the lives scoreboard (1-20) |
| `passive.death.heart.drop.probability` | Double | `1.0` | Probability of dropping hearts on natural deaths (0.0-1.0) |
| `player.death.heart.drop.probability` | Double | `1.0` | Probability of dropping hearts on player kills (0.0-1.0) |

### Probability System

The mod includes a probability system for heart drops that works alongside the heart drop modes:

- **`passive.death.heart.drop.probability`**: Controls the chance of dropping hearts on natural deaths (mobs, environment, etc.)
- **`player.death.heart.drop.probability`**: Controls the chance of dropping hearts on player kills
- **Values**: Range from 0.0 (never drop) to 1.0 (always drop)
- **Mode Override**: If a heart drop mode (like NEVER) disables drops, probabilities are ignored
- **Examples**: 
  - `0.5` = 50% chance of dropping hearts
  - `0.25` = 25% chance of dropping hearts
  - `1.0` = 100% chance (always drop, default behavior)

### Duration Format Examples

The mod uses ISO-8601 duration format for time-based settings:

| Duration | Format | Description |
|----------|--------|-------------|
| 15 seconds | `PT15S` | 15 seconds |
| 1 minute | `PT1M` | 1 minute |
| 1 hour | `PT1H` | 1 hour |
| 24 hours | `PT24H` | 24 hours |
| 2 days | `P2D` | 2 days |
| 1 week | `P7D` | 7 days |
| 1 hour 30 minutes | `PT1H30M` | 1 hour and 30 minutes |

### Runtime Configuration

You can modify configuration values at runtime using in-game commands:

- **View all config**: `/softhardcore config` - Shows all current configuration values
- **Set config value**: `/softhardcore setconfig <key> <value>` - Changes a config value immediately
- **Reload config**: `/softhardcore reloadconfig` - Reloads the config file from disk

**Note**: Changes made via commands are saved to the config file and persist across server restarts.

### Example Configuration

```properties
# Players start with 25 lives
default.lives=25

# Players are banned for 2 days when they run out of lives
ban.duration=P2D

# Drop 1 life heart when a player dies
lives.dropped.on.death=1

# Use neutral heart dropping (all deaths drop hearts)
heart.drop.mode=NEUTRAL

# Lives regenerate every 24 hours
life.regen.cooldown=PT24H

# Players get 5 lives when their ban expires
returning.lives=5

# Life hearts give 1 life when consumed
lives.gained.from.heart=1

# Show up to 5 players on the scoreboard
scoreboard.max.rows=5

# Heart drop probabilities (0.0 = never, 1.0 = always)
passive.death.heart.drop.probability=1.0
player.death.heart.drop.probability=1.0
```

## 🎮 Commands

All commands are prefixed with `/softhardcore` for better organization.

### Player Commands

#### `/softhardcore getlives [player]`
- **Description**: Check remaining lives (your own or another player's)
- **Permission**: Available to all players
- **Usage**: 
  - `/softhardcore getlives` - Check your own lives
  - `/softhardcore getlives <player>` - Check another player's lives
- **Example**: 
  - Shows "You have 2 lives remaining."
  - `/softhardcore getlives Steve` shows "Steve has 1 lives remaining."

#### `/softhardcore config`
- **Description**: View all current configuration values
- **Permission**: Available to all players
- **Usage**: `/softhardcore config`
- **Example**: Shows all config options with their current values

### Admin Commands

#### `/softhardcore setlives <player> <amount>`
- **Description**: Set a specific player's lives to a specific amount
- **Permission**: Requires operator level 2+ (admin only)
- **Usage**: `/softhardcore setlives <player> <amount>`
- **Example**: `/softhardcore setlives Alex 5` sets Alex's lives to 5
- **Note**: Lives can be set between 0 and the maximum configured lives

#### `/softhardcore setalllives <amount>`
- **Description**: Set all players' lives to a specific amount (both online and offline)
- **Permission**: Requires operator level 2+ (admin only)
- **Usage**: `/softhardcore setalllives <amount>`
- **Example**: `/softhardcore setalllives 25` sets all players to 25 lives
- **Features**:
  - Updates all currently online players immediately
  - Updates all offline players who have ever joined the server
  - Shows confirmation with counts of online/offline players affected
  - Perfect for server resets or event management

#### `/softhardcore setconfig <key> <value>`
- **Description**: Modify a configuration value at runtime
- **Permission**: Requires operator level 2+ (admin only)
- **Usage**: `/softhardcore setconfig <key> <value>`
- **Example**: `/softhardcore setconfig default.lives 30`
- **Features**:
  - Autocomplete suggestions for config keys (press Tab)
  - Validates values before applying
  - Saves changes to config file immediately
  - Changes persist across server restarts
- **Available Keys**: All configuration options (see [Configuration](#-configuration))

#### `/softhardcore reloadconfig`
- **Description**: Reload the configuration file from disk
- **Permission**: Requires operator level 2+ (admin only)
- **Usage**: `/softhardcore reloadconfig`
- **Example**: Reloads all config values from `softhardcoreconfig.properties`
- **Use Case**: When you've manually edited the config file and want to apply changes

## 🎯 Gameplay Mechanics

### Life System
- Players start with a configurable number of lives (default: 25)
- Each death reduces lives by 1
- When lives reach 0, the player is temporarily banned
- Lives are displayed on the player list and in-game HUD
- Lives are automatically managed with proper timestamp tracking

### Life Hearts
- Life hearts are special items that can restore lives
- Players drop life hearts when they die (based on heart drop mode)
- Right-click to consume a life heart and gain lives
- Life hearts can be given to other players or used on yourself

### Ban System
- When a player runs out of lives, they are temporarily banned
- Ban duration is configurable (default: 2 days)
- Players can rejoin after the ban expires
- Returning players get a configurable number of lives back
- Ban messages show human-readable duration (e.g., "2 days, 3 hours, 15 minutes")

### Life Regeneration
- Lives can automatically regenerate over time
- Regeneration cooldown is configurable (default: 24 hours)
- **Multiple Interval Support**: If multiple cooldown periods have passed, all pending regenerations are applied at once
- Players must wait the full cooldown period between regenerations
- New players can regenerate immediately
- Regeneration timestamps are automatically managed

## 🎭 Heart Drop Modes

The mod supports different modes for when life hearts are dropped:

### PASSIVE
- **Description**: Drop hearts only on natural deaths (not player kills)
- **Use Case**: PvE-focused servers where PvP is discouraged
- **Example**: Dying to mobs drops hearts, but PvP deaths don't

### NEUTRAL (Default)
- **Description**: Drop hearts on any death
- **Use Case**: Balanced gameplay where all deaths matter equally
- **Example**: All deaths drop hearts regardless of cause

### TEAM
- **Description**: Drop hearts on natural deaths + when killed by different team players
- **Use Case**: Team-based gameplay with friendly fire disabled
- **Example**: Natural deaths and enemy team kills drop hearts, same team kills don't

### COMPETITIVE
- **Description**: Drop hearts ONLY when killed by different team players
- **Use Case**: Competitive team-based gameplay
- **Example**: Only enemy team kills drop hearts, natural deaths don't

### VENGEFUL
- **Description**: Drop hearts ONLY when killed by another player (any player, regardless of team)
- **Use Case**: PvP-focused gameplay where only player kills matter
- **Example**: Any player kill drops hearts, natural deaths don't (team doesn't matter)

### NEVER
- **Description**: Never drop hearts
- **Use Case**: Admin-controlled lives or special game modes
- **Example**: No hearts are ever dropped, lives must be managed by admins

## 🎨 Visual Features

### Nametag Overlay
- Custom nametag display above player heads showing lives and armor
- Red heart icon followed by the number of remaining lives
- Blue droplet/diamond icon followed by armor points
- Real-time updates as lives and armor change
- Only visible to other players (not yourself)

### Scoreboard Integration
- Lives are displayed next to player names in the player list
- Shows lives for all players (including dead/banned players)
- Configurable maximum number of players shown


## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👥 Credits

- **Neskech** - Lead Developer
- **AlternateFire** - Co-Developer

## 🔗 Links

- [GitHub Repository](https://github.com/neskech/Soft-Hardcore)


