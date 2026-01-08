# Notoriety

A Paper plugin that brings a classic MMORPG-style notoriety system to Minecraft. Players are dynamically categorized based on their actions, creating a self-regulating community where crime has consequences.

## Features

### Reputation System
Players are classified into three states based on their behavior:

| Status | Color | Condition |
|--------|-------|-----------|
| Innocent | Blue | Alignment ≥ 0, PKCount = 0 |
| Criminal | Gray | Alignment < 0, PKCount = 0 |
| Murderer | Red | PKCount ≥ 1 |

### Ownership Protection
- Blocks placed by innocent players are automatically protected
- Breaking or stealing from others' property is a crime
- 5-second grace period to undo accidental actions
- Owned blocks are protected from explosions (Creeper, TNT, etc.)

### Three-State Trust System
- **TRUST** - Grant full access to your property
- **UNSET** - Default state; guild members get automatic trust
- **DISTRUST** - Block access even from guild members

### Guild System
Form groups with automatic trust relationships:
- Guild members automatically trust each other (unless DISTRUST is set)
- Player display format: `Title PlayerName [GuildTag]`
- 16 customizable tag colors
- Three roles: Master, Vice Master, Member
- Guild chat channel for private communication
- Application system (join guilds without invitation)

### Territory System
Guilds can claim protected land:
- Requires minimum 5 guild members
- 1 chunk per 5 members
- Non-members cannot place/break blocks or access containers
- Beacon auto-placed at territory center
- Entry/exit notifications
- Guild master receives territory status on login

### Inspect System
Investigate block ownership and territory information:
- `/inspect` - Toggle inspect mode
- `/noty inspect tool` - Get inspection stick
- Shows owner, placement time, trust status, and territory info

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
- Enhanced golem stats when attacking criminals

### Bounty System
- Place bounties on murderers
- Claim rewards by defeating wanted players
- Leaderboard display via signs

### 5-Tier Penalty System

| Severity | Penalty | Examples |
|----------|---------|----------|
| Severe | -50 | Kill villager (with bed), Theft, Kill golem |
| Moderate | -20 | Kill animal (witnessed) |
| Minor | -10 | Destroy property, Kill villager (no bed) |
| Petty | -5 | Destroy villager bed/workstation |
| Trivial | -1 | Attack player/villager, Harvest crops |

## Commands

**Main Commands**
- `/noty status [player]` - View reputation status
- `/noty history [player]` - View crime history
- `/inspect` - Toggle inspect mode
- `/noty inspect tool` - Get inspection stick

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

**Guild Application Commands**
- `/guild apply <guild>` - Apply to join a guild
- `/guild applications` - View received applications (Master/Vice Master)
- `/guild applications accept <player>` - Accept application
- `/guild applications reject <player>` - Reject application

**Territory Commands**
- `/guild territory set` - Set territory center (Master only)
- `/guild territory info` - View territory information
- `/guild territory remove` - Remove territory (Master only)

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
- Paper Server
- Java 21+
- Vault (optional, for economy features)

## Configuration

Supports both SQLite and MySQL databases. Multi-language support with Japanese and English localization.

## API

Provides a developer API for third-party plugin integration:

```kotlin
val api = notoriety.api
val color = api.getNameColor(player.uniqueId)
api.addAlignment(player.uniqueId, 50)
```

## Events

| Event | Description |
|-------|-------------|
| PlayerColorChangeEvent | When player status changes |
| PlayerCrimeEvent | When a crime is committed |
| PlayerGoodDeedEvent | When a good deed is performed |
| BountyClaimedEvent | When a bounty is claimed |
| GuildCreateEvent | When a guild is created |
| GuildMemberJoinEvent | When a member joins a guild |
| GuildMemberLeaveEvent | When a member leaves a guild |
| GuildApplicationEvent | When a guild application is submitted |
| TerritoryClaimEvent | When territory is claimed |
| TerritoryReleaseEvent | When territory is released |
| TerritoryEnterEvent | When entering a territory |
| TerritoryLeaveEvent | When leaving a territory |
