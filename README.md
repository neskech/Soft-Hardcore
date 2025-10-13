# SoftHardcore

A soft-hardcore, highly configurable Minecraft mod that gives each player a limited number of lives. Once players run out of lives, they are temporarily banned for a configurable amount of time. Players can drop life hearts to give other players back lives, creating a cooperative survival experience.


## ✨ Features

- **Limited Lives System**: Each player starts with a configurable number of lives
- **Temporary Bans**: Players are temporarily banned when they run out of lives
- **Life Hearts**: Players can drop life hearts to restore lives to others
- **Automatic Life Regeneration**: Lives can regenerate over time (configurable)
- **Team-Based Mechanics**: Support for team-based heart dropping
- **Scoreboard Integration**: Lives are displayed on the player list
- **HUD Overlay**: Shows remaining lives in-game
- **Admin Commands**: Full administrative control over player lives
- **Highly Configurable**: Extensive configuration options for all aspects

## 🚀 Installation





### Dependencies

This mod requires the following dependencies to function properly:

| Dependency | Version | Description |
|------------|---------|-------------|
| **Fabric Loader** | 0.16.10+ | The mod loader that runs Fabric mods |
| **Fabric API** | Latest for 1.20.1 | Core Fabric API providing essential modding functionality |
| **Cardinal Components API** | 5.2.3 | Used for the lives component system |
| **Java** | 17+ | Required Java version for Minecraft 1.20.1 |



## ⚙️ Configuration

The mod creates a configuration file at `config/softhardcoreconfig.properties` when first run. You can edit this file to customize the mod's behavior.

### Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `default.lives` | Integer | `15` | Number of lives each player starts with |
| `ban.duration` | Duration | `PT48H` | How long players are banned when they run out of lives (ISO-8601 format) |
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
| 6 seconds | `PT6S` | 6 seconds |
| 1 minute | `PT1M` | 1 minute |
| 1 hour | `PT1H` | 1 hour |
| 1 day | `P1D` | 24 hours |
| 1 week | `P7D` | 7 days |
| 1 hour 30 minutes | `PT1H30M` | 1 hour and 30 minutes |

### Example Configuration

```properties
# Players start with 3 lives
default.lives=3

# Players are banned for 1 hour when they run out of lives
ban.duration=PT1H

# Drop 2 life hearts when a player dies
lives.dropped.on.death=2

# Use team-based heart dropping
heart.drop.mode=TEAM

# Lives regenerate every 12 hours
life.regen.cooldown=PT12H

# Players get 2 lives when their ban expires
returning.lives=2

# Life hearts give 2 lives when consumed
lives.gained.from.heart=2

# Show up to 10 players on the scoreboard
scoreboard.max.rows=10

# Heart drop probabilities (0.0 = never, 1.0 = always)
passive.death.heart.drop.probability=0.8
player.death.heart.drop.probability=1.0
```

## 🎮 Commands

### Player Commands

#### `/lives`
- **Description**: Check your own remaining lives
- **Permission**: Available to all players
- **Usage**: `/lives`
- **Example**: Shows "You have 2 lives remaining."

#### `/getlives <player>`
- **Description**: Check another player's remaining lives
- **Permission**: Available to all players
- **Usage**: `/getlives <player>`
- **Example**: `/getlives Steve` shows "Steve has 1 lives remaining."

### Admin Commands

#### `/setlives <player> <amount>`
- **Description**: Set a player's lives to a specific amount
- **Permission**: Requires operator level 2+ (admin only)
- **Usage**: `/setlives <player> <amount>`
- **Example**: `/setlives Alex 5` sets Alex's lives to 5
- **Note**: Lives cannot be set below 1 to avoid triggering ban logic

## 🎯 Gameplay Mechanics

### Life System
- Players start with a configurable number of lives (default: 2)
- Each death reduces lives by 1
- When lives reach 0, the player is temporarily banned
- Lives are displayed on the player list and in-game HUD

### Life Hearts
- Life hearts are special items that can restore lives
- Players drop life hearts when they die (based on heart drop mode)
- Right-click to consume a life heart and gain lives
- Life hearts can be given to other players or used on yourself

### Ban System
- When a player runs out of lives, they are temporarily banned
- Ban duration is configurable (default: 6 seconds for testing)
- Players can rejoin after the ban expires
- Returning players get a configurable number of lives back

### Life Regeneration
- Lives can automatically regenerate over time
- Regeneration cooldown is configurable (default: 24 hours)
- Players must wait the full cooldown period between regenerations
- New players can regenerate immediately

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

