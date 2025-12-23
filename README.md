# Notoriety

クラシックMMORPG風の名声システムをMinecraftに実装したPaperプラグイン。

プレイヤーの行動に応じて評価が変動し、PKや窃盗などの悪行を抑制するシステムを提供します。

## 動作環境

- Minecraft 1.21.1+
- Paper/Spigot サーバー
- Java 21+
- Vault（オプション：経済機能）

## 特徴

### 名声システム

プレイヤーは行動に応じて3つの状態に分類されます：

| 状態 | 色 | 条件 |
|-----|---|-----|
| Innocent | 青 | 犯罪なし |
| Criminal | 灰 | 犯罪を犯した |
| Murderer | 赤 | プレイヤーを殺害した |

### 所有権システム

- 青プレイヤーが設置したブロックは自動的に保護
- 他人の所有物を破壊・窃盗すると犯罪
- 5秒以内に元に戻せば犯罪にならない（猶予システム）

### 信頼システム

- プレイヤー間で信頼関係を設定可能
- 信頼されたプレイヤーは所有物へのアクセスが許可
- 攻撃・殺害も犯罪にならない

### 村人・ゴーレムシステム

- 村人が犯罪を目撃すると叫ぶ
- アイアンゴーレムが犯罪者を攻撃
- 遠い場合はテレポートして即座に攻撃

### 懸賞金システム

- 赤プレイヤーに懸賞金を設定可能
- 討伐すると報酬を獲得
- 看板でランキング表示

## インストール

1. [Releases](https://github.com/0x48lab/notoriety/releases)から最新のJARをダウンロード
2. サーバーの`plugins`フォルダに配置
3. サーバーを再起動

## コマンド

### メインコマンド

```
/noty status [player]     - ステータス表示
/noty history [player]    - 犯罪履歴表示
/noty admin ...           - 管理コマンド（OP専用）
```

### 信頼コマンド

```
/trust add <player>       - プレイヤーを信頼
/trust remove <player>    - 信頼を解除
/trust list               - 信頼関係一覧
/trust check <player>     - 信頼関係を確認
```

### 懸賞金コマンド

```
/bounty set <player> <amount>  - 懸賞金を設定
/bounty list                   - 懸賞金リスト
/bounty check <player>         - 懸賞金を確認
```

## 設定

### config.yml

```yaml
# 言語設定（ja / en）
locale: ja

# データベース設定
database:
  type: sqlite  # sqlite または mysql
  sqlite:
    file: "data.db"
  mysql:
    host: "localhost"
    port: 3306
    database: "notoriety"
    username: "minecraft"
    password: "password"
```

## パラメータ

| パラメータ | 範囲 | 説明 |
|-----------|-----|------|
| CrimePoint | 0-1000 | 犯罪ポイント（灰状態の管理） |
| PKCount | 0- | 殺人カウント（赤状態の管理） |
| Karma | 0-1000 | 善悪の評価（称号用） |
| Fame | 0-1000 | 名声（称号用） |

## 犯罪ポイント

| 行動 | ポイント |
|-----|---------|
| 他人の所有物を盗む | +100 |
| 他人の所有物を破壊 | +50 |
| プレイヤーを攻撃 | +150 |
| 村人を殺害 | +200 |
| 動物を殺す（目撃時） | +20 |
| 作物を収穫（目撃時） | +10 |

## 称号

### 青プレイヤー（善）

| Karma | 称号 |
|-------|-----|
| 100+ | 義人 |
| 250+ | 功士 |
| 500+ | 豪傑 |
| 625+ | 聖騎士 |
| 750+ | 聖将 |
| 875+ | 勇者 |

### 赤プレイヤー（悪）

| Karma | 称号 |
|-------|-----|
| 100+ | 罪人 |
| 250+ | 凶漢 |
| 500+ | 悪鬼 |
| 625+ | 外道 |
| 750+ | 殺人鬼 |
| 875+ | 殺戮者 |

## API

サードパーティプラグインから利用可能なAPIを提供しています。

```kotlin
val notoriety = Bukkit.getPluginManager().getPlugin("Notoriety") as Notoriety
val api = notoriety.api

// プレイヤーの色を取得
val color = api.getPlayerColor(player.uniqueId)

// 犯罪を記録
api.commitCrime(player.uniqueId, CrimeType.THEFT, 100)

// 信頼関係を確認
val isTrusted = api.isTrusted(owner, accessor)
```

## イベント

| イベント | 説明 |
|---------|------|
| PlayerColorChangeEvent | プレイヤーの色が変わった時 |
| PlayerCrimeEvent | 犯罪が発生した時 |
| PlayerGoodDeedEvent | 善行を行った時 |
| BountyClaimedEvent | 懸賞金が支払われた時 |

## ライセンス

MIT License

## 作者

hacklab
