# Notoriety

A Paper plugin that brings a classic MMORPG-style notoriety system to Minecraft. Players are dynamically categorized based on their actions, creating a self-regulating community where crime has consequences.

## Features

### Reputation System
Players are classified into three states based on their behavior:

| Status | Color | Condition |
|--------|-------|-----------|
| Innocent | Blue | No crimes committed |
| Criminal | Gray | Committed a crime |
| Murderer | Red | Killed another player |

### Ownership Protection
- Blocks placed by innocent players are automatically protected
- Breaking or stealing from others' property is a crime
- 5-second grace period to undo accidental actions
- Owned blocks are protected from explosions (Creeper, TNT, etc.)

### Three-State Trust System
- **TRUST** - Grant full access to your property
- **UNSET** - Default state; guild members get automatic trust
- **DISTRUST** - Block access even from guild members
- Distrusted players can view chests but cannot take items

### Guild System
Form groups with automatic trust relationships:
- Guild members automatically trust each other (unless DISTRUST is set)
- Guild tags displayed before player names (e.g., `[BC] PlayerName`)
- 16 customizable tag colors
- Three roles: Master, Vice Master, Member
- Guild chat channel for private communication

### Chat System
Multiple communication channels:
- **Local** - 50-block range (default)
- **Global** - Server-wide with `!` prefix
- **Guild** - Members only with `@` prefix
- **Whisper** - Private 1-on-1 messages
- Romaji to Hiragana conversion for Japanese players

### Villager & Golem System
- Villagers shout when they witness crimes
- Iron Golems attack criminals on sight
- Golems teleport to criminals if too far away

### Bounty System
- Place bounties on murderers
- Claim rewards by defeating wanted players
- Leaderboard display via signs

## Commands

**Main Commands**
- `/noty status [player]` - View reputation status
- `/noty history [player]` - View crime history

**Trust Commands**
- `/trust add <player>` - Trust a player (TRUST)
- `/trust distrust <player>` - Distrust a player (DISTRUST)
- `/trust remove <player>` - Reset trust (UNSET)
- `/trust list` - View trust relationships
- `/trust check <player>` - Check trust status with a player

**Guild Commands**
- `/guild create <name> <tag> [description]` - Create a guild
- `/guild disband` - Disband your guild (Master only)
- `/guild invite <player>` - Invite a player
- `/guild accept/decline <guild>` - Accept/decline invitation
- `/guild kick <player>` - Remove a member (Master only)
- `/guild leave` - Leave your guild
- `/guild promote/demote <player>` - Change member roles
- `/guild transfer <player>` - Transfer leadership
- `/guild tagcolor <color>` - Change tag color

**Chat Commands**
- `/w <player> <message>` - Send whisper
- `/r <message>` - Reply to last whisper
- `/romaji on/off` - Toggle romaji conversion

**Bounty Commands**
- `/bounty set <player> <amount>` - Set a bounty
- `/bounty list` - View all bounties
- `/bounty check <player>` - Check specific bounty

## Requirements

- Minecraft 1.21.1+
- Paper/Spigot Server
- Java 21+
- Vault (optional, for economy features)

## Configuration

Supports both SQLite and MySQL databases. Multi-language support with Japanese and English localization.

## API

Provides a developer API for third-party plugin integration:

```kotlin
val api = notoriety.api
val color = api.getPlayerColor(player.uniqueId)
api.commitCrime(player.uniqueId, CrimeType.THEFT, 100)
```

## Events

- `PlayerColorChangeEvent` - When player status changes
- `PlayerCrimeEvent` - When a crime is committed
- `PlayerGoodDeedEvent` - When a good deed is performed
- `BountyClaimedEvent` - When a bounty is claimed
