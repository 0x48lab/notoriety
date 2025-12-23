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

### Trust System
- Set trust relationships between players
- Trusted players can access your property
- Combat between trusted players isn't considered a crime

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
- `/trust add <player>` - Trust a player
- `/trust remove <player>` - Remove trust
- `/trust list` - View trust relationships

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
