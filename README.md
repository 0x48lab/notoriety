# Notoriety

クラシックMMORPG風の名声システムをMinecraftに実装したPaperプラグイン。

プレイヤーの行動に応じて評価が変動し、PKや窃盗などの悪行を抑制するシステムを提供します。

**[Documentation / ドキュメント](https://0x48lab.github.io/notoriety/)**

## 動作環境

- Minecraft 1.21.1+
- Paper サーバー
- Java 21+
- Vault（オプション：経済機能）

## 特徴

### 名声システム

プレイヤーは行動に応じて3つの状態に分類されます：

| 状態 | 色 | 条件 |
|-----|---|-----|
| Innocent | 青 | Alignment ≥ 0 かつ PKCount = 0 |
| Criminal | 灰 | Alignment < 0 かつ PKCount = 0 |
| Murderer | 赤 | PKCount ≥ 1 |

### 所有権システム

- 青プレイヤーが設置したブロックは自動的に保護
- 他人の所有物を破壊・窃盗すると犯罪
- 5秒以内に元に戻せば犯罪にならない（猶予システム）
- 爆発（クリーパー、TNT等）からも保護

### 信頼システム

- プレイヤー間で信頼関係を設定可能
- 信頼されたプレイヤーは所有物へのアクセスが許可
- 攻撃・殺害も犯罪にならない
- 三段階信頼: 信頼/未設定/不信頼

### ギルドシステム

- ギルドを作成してメンバーを招待
- ギルドメンバー間は自動的に信頼関係
- プレイヤー表示: `称号 プレイヤー名 [ギルドタグ]`
- GUIによる直感的な管理
- 申請システム（招待なしでギルドに加入申請可能）

### 領地システム

- ギルドメンバー5人以上で領地を設定可能
- 5人あたり1チャンクの領地サイズ
- 領地内では非メンバーのブロック設置・破壊・コンテナアクセスを拒否
- 領地中心にビーコンを自動設置
- 領地出入り時に通知メッセージ

### チャットシステム

- ローカルチャット（50ブロック範囲）
- グローバルチャット（`!`プレフィックス）
- ギルドチャット（`@`プレフィックス）
- ウィスパー（`/w`コマンド）
- ローマ字→ひらがな自動変換

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
/noty inspect [tool]      - 所有権確認モード切替 / 調査棒を取得
/noty admin ...           - 管理コマンド（OP専用）
```

### 調査コマンド

```
/inspect                  - 所有権確認モードのON/OFF切替
/noty inspect             - 所有権確認モードのON/OFF切替
/noty inspect tool        - 調査棒（Inspection Stick）を取得
```

所有権確認モードでは、ブロックを右クリックすると所有者情報が表示されます。

### 信頼コマンド

```
/trust add <player>       - プレイヤーを信頼
/trust remove <player>    - 信頼設定を解除（未設定に戻す）
/trust distrust <player>  - プレイヤーを不信頼に設定
/trust list               - 信頼関係一覧
/trust check <player>     - 信頼関係を確認
```

三段階信頼システム：
- **信頼**: 所有物へのアクセスを許可
- **未設定**: ギルドメンバーの場合はアクセス許可（デフォルト）
- **不信頼**: ギルドメンバーでもアクセスを拒否

### 懸賞金コマンド

```
/bounty add <player> <amount>  - 懸賞金をかける
/bounty list                   - 懸賞金リスト
/bounty check <player>         - 懸賞金を確認
```

### ギルドコマンド

```
/guild                         - ギルドGUIを開く（メニュー）
/guild menu                    - ギルドGUIを開く
/guild create <name> <tag>     - ギルドを作成
/guild info [guild]            - ギルド情報を表示
/guild list                    - ギルド一覧を表示
/guild members                 - メンバー一覧を表示
/guild invite <player>         - プレイヤーを招待
/guild kick <player>           - メンバーを追放
/guild leave                   - ギルドを脱退
/guild accept [guild]          - 招待を承諾
/guild deny [guild]            - 招待を拒否
/guild invites                 - 受け取った招待一覧
/guild promote <player>        - メンバーを昇格（マスター専用）
/guild demote <player>         - メンバーを降格（マスター専用）
/guild transfer <player>       - マスター権限を譲渡
/guild dissolve                - ギルドを解散（マスター専用）
/guild color <color>           - タグの色を変更
/guild help                    - ヘルプを表示

# ギルド申請
/guild apply <guild>           - ギルドに加入申請
/guild applications            - 受信した申請一覧（マスター/副マスター用）
/guild applications accept <player>  - 申請を承認
/guild applications reject <player>  - 申請を却下

# 領地管理
/guild territory set           - 現在地を領地中心に設定（マスター専用）
/guild territory info          - 領地情報を表示
/guild territory remove        - 領地を削除（マスター専用）
```

### チャットコマンド

```
/chat local                - ローカルチャットモード
/chat global               - グローバルチャットモード
/chat guild                - ギルドチャットモード
/chat romaji               - ローマ字変換のON/OFF
/w <player> <message>      - プライベートメッセージ
/r <message>               - 返信
```

### 管理者コマンド（OP専用）

```
/noty admin listgray                           - 灰色プレイヤー一覧
/noty admin listred                            - 赤プレイヤー一覧
/noty admin guildtag <player> <tag|clear>      - ギルドタグテスト用
/noty admin <player> <alignment|fame|pk> <set|add> <value>
                                               - パラメータ操作
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

| パラメータ | 範囲 | 初期値 | 説明 |
|-----------|-----|--------|------|
| Alignment | -1000〜+1000 | 0 | 善悪軸（負=灰、正=善行累積） |
| PKCount | 0〜 | 0 | 殺人カウント（赤状態の管理） |
| Fame | 0〜1000 | 0 | 名声（称号用） |

## Alignment変動

### 減少（悪行）- 5段階

| 行動 | 変動 | 備考 |
|-----|------|------|
| **重大犯罪** | | |
| 村人を殺害（ベッド紐付き） | -50 | |
| 他人の所有物を盗む | -50 | |
| アイアンゴーレムを殺害 | -50 | 周囲のゴーレムも敵対 |
| **中程度** | | |
| 動物を殺す | -20 | 村人/ゴーレムに目撃時 |
| **軽度** | | |
| 他人の所有物を破壊 | -10 | 5秒以内復元でキャンセル |
| 村人を殺害（ベッドなし） | -10 | |
| **軽微** | | |
| 村人のベッドを破壊 | -5 | 自分/信頼者設置は免除 |
| 村人の仕事場を破壊 | -5 | 自分/信頼者設置は免除 |
| **最軽微** | | |
| プレイヤーを攻撃 | -1 | |
| 村人を攻撃 | -1 | |
| 作物を収穫 | -1 | 村人に目撃時 |

### 増加（善行・時間経過）

| 行動 | 変動 |
|-----|------|
| 時間経過 | +10/時間（0に向かって回復） |
| 赤プレイヤーを討伐 | +50 |
| 村人と取引 | +5 |
| モンスターを討伐 | +1 |

## 称号

### 青（Innocent）- Fame基準

| Fame | 称号（英語） | 称号（日本語） |
|------|-------------|---------------|
| 750〜1000 | Glorious Lord | 勇者 |
| 500〜749 | Great Lord | 聖将 |
| 250〜499 | Lord | 聖騎士 |
| 100〜249 | Notable | 功士 |

### 灰（Criminal）- Fame基準

| Fame | 称号（英語） | 称号（日本語） |
|------|-------------|---------------|
| 750〜1000 | Renegade | 反逆者 |
| 500〜749 | Outlaw | 無法者 |
| 250〜499 | Rogue | ならず者 |
| 100〜249 | Scoundrel | 悪党 |

### 赤（Murderer）- PKCount基準

| PKCount | 称号（英語） | 称号（日本語） |
|---------|-------------|---------------|
| 200+ | Dread Lord | 殺戮者 |
| 100〜199 | Dark Lord | 殺人鬼 |
| 50〜99 | Infamous | 悪鬼 |
| 30〜49 | Notorious | 凶漢 |
| 10〜29 | Wicked | 外道 |
| 1〜9 | Outcast | 罪人 |

## API

サードパーティプラグインから利用可能なAPIを提供しています。

```kotlin
val notoriety = Bukkit.getPluginManager().getPlugin("Notoriety") as Notoriety
val api = notoriety.api

// Alignmentを取得
val alignment = api.getAlignment(player.uniqueId)

// ネームカラーを取得
val color = api.getNameColor(player.uniqueId)

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
| GuildCreateEvent | ギルドが作成された時 |
| GuildMemberJoinEvent | メンバーがギルドに参加した時 |
| GuildMemberLeaveEvent | メンバーがギルドを脱退した時 |
| GuildApplicationEvent | ギルド申請が送信された時 |
| TerritoryClaimEvent | 領地が設定された時 |
| TerritoryReleaseEvent | 領地が削除された時 |
| TerritoryEnterEvent | 領地に入った時 |
| TerritoryLeaveEvent | 領地から出た時 |

## 変更履歴

### v0.3.0

- **領地システム**を追加
  - ギルドメンバー5人以上で領地を設定可能
  - 5人あたり1チャンクの領地サイズ
  - 領地内では非メンバーのブロック設置・破壊・コンテナアクセスを拒否
  - 領地中心にビーコンを自動設置
  - 領地出入り時に通知メッセージ
  - ギルドマスターログイン時に領地状態を通知
- **ギルド申請システム**を追加
  - `/guild apply <guild>` で招待なしに加入申請可能
  - マスター/副マスターが申請を承認・却下
  - 申請有効期限: 7日間
- **プレイヤー表示形式を変更**
  - 旧: `[称号] 名前` → 新: `称号 名前 [ギルドタグ]`
  - Team prefix/suffixを使用した表示
- **調査モードで領地情報を表示**
- **ペナルティバランスを5段階に調整**
- SQLite接続プールのデッドロック問題を修正
- Adventure APIでのレガシーカラーコード警告を修正

### v0.2.9

- ペナルティバランス調整（重大-50、中程度-20、軽度-10、軽微-5、最軽微-1）
- 村人仕事場/ベッド破壊の免除条件に信頼者を追加
- 調査モード・調査棒の追加
- 懸賞金看板リスナー追加
- ゴーレム独立検知・帰還・自動強化機能
- 犯罪通知システム
- モンスター討伐報酬（Alignment +1）

### v0.2.6

- 調査コマンド（`/inspect`）のドキュメントを追加
- 三段階信頼システム（信頼/未設定/不信頼）のコマンドを実装
- ウィスパーコマンド（`/w`, `/r`）を実装
- 爆発保護機能を実装
- ギルド信頼との所有権システム統合

## ライセンス

MIT License

## 作者

hacklab
