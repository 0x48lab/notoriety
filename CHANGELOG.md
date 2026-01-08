# Changelog

このプロジェクトの全ての注目すべき変更はこのファイルに記録されます。

フォーマットは [Keep a Changelog](https://keepachangelog.com/ja/1.0.0/) に基づいています。

## [Unreleased]

### Added
- 領地システム（Territory System）
  - `/guild territory set` - 現在地を領地の中心として設定
  - `/guild territory info` - 領地情報を表示
  - `/guild territory remove` - 領地を削除
  - 領地出入り通知（プレイヤーが領地に出入りするとメッセージ表示）
  - 領地保護（非ギルドメンバーのブロック破壊/設置を防止）
  - ビーコン自動設置（領地の中心にビーコンが設置される）
  - ギルドメンバー数に応じた領地サイズ制限
- ギルド申請システム（Guild Application System）
  - `/guild apply <guild>` - ギルドに加入申請
  - `/guild applications` - 受信した申請一覧（マスター/副マスター用）
  - `/guild applications accept <player>` - 申請を承認
  - `/guild applications reject <player>` - 申請を却下
  - 申請有効期限: 7日間
- ギルドマスターログイン通知（領地状態をログイン時に表示）
- 調査モードで領地情報を表示（所有ギルド名を表示）

### Changed
- PKCount が 0 になったとき懸賞金を出資者に返金するように変更
  - 管理者コマンドで PKCount を 0 にした場合
  - 時間経過で PKCount が減少して 0 になった場合
  - 出資者がオンラインの場合は返金通知を表示
  - サーバー起動時に不整合な懸賞金データをクリーンアップ（PKCount 0 のプレイヤーへの懸賞金を返金）
- 懸賞金コマンドを `set` から `add` に変更
  - `/bounty add <player> <amount>` - 懸賞金をかける
  - `/wanted add <player> <amount>` - 同上（エイリアス）
  - `set` は後方互換性のため引き続き使用可能
- `/wanted` コマンドにタブ補完を追加
- プレイヤー表示形式を変更
  - 旧: `[Title] Name` → 新: `Title Name [Guild]`
  - BELOW_NAME表示を廃止し、Team prefix/suffixを使用
  - 称号をprefix、ギルドタグをsuffixに表示
- レガシーカラーコード(§)の処理を修正
  - `Component.text()`から`LegacyComponentSerializer`に変更
  - 対象: TerritoryEntryListener, HistoryCommand

### Fixed
- 自殺時にPKCountが増加するバグを修正
  - エンダードラゴン等に吹き飛ばされて落下死した際、victim.killerが自分自身を返すケースでPKCount+1されていた
  - killer == victim の場合はPK判定をスキップするように修正
- オフラインプレイヤーに懸賞金を設定できない問題を修正
  - `PlayerManager.getOrLoadPlayer()` メソッドを追加（キャッシュまたはDBから取得）
  - `BountyService.setBounty()` でオフラインプレイヤーのデータもDBから読み込むように変更
- `/noty admin` コマンドでオフラインプレイヤーを操作できない問題を修正
  - オフラインプレイヤーのデータもDBから読み込み、変更後に保存するように修正
- SQLite接続プールのデッドロック問題を修正
  - `TerritoryRepository`でネストされたDB接続を回避
  - 同一接続を再利用する`getChunksWithConnection()`メソッドを追加
- プレイヤー名の下に「0」が表示される問題を修正
- Adventure APIでのレガシーカラーコード警告を修正

## [0.2.9] - 2025-01-07

### Changed
- ペナルティバランスを5段階に調整
  - 重大犯罪（村人殺害[ベッド付]/窃盗/ゴーレム殺害）: -50
  - 中程度（動物殺害[目撃時]）: -20
  - 軽度（所有物破壊/村人殺害[ベッドなし]）: -10
  - 軽微（村人ベッド破壊/村人仕事場破壊）: -5
  - 最軽微（攻撃/作物収穫/村人攻撃）: -1
- 村人仕事場/ベッド破壊の免除条件に信頼者を追加（自分または信頼者が設置した場合は免除）
- CrimePoint/KarmaをAlignment(-1000〜+1000)に統合
- 称号ロジック変更: 青はFameのみ、赤はPKCountのみで決定
- BELOW_NAMEを使用した称号表示に変更（プレフィックス廃止）
- 村人取引の価格調整を実装（灰は1.5倍、高Alignmentで割引）
- ゴーレム検索範囲を64→128ブロックに拡大
- 村人メッセージをi18n対応（プレフィックス・警告メッセージ）
- 村関連犯罪を全プレイヤー（青・灰・赤）に適用

### Added
- ブロック所有者調査機能
  - `/noty inspect` - 調査モードのON/OFF切り替え（アクションバーに状態表示）
  - `/noty inspect tool` - 調査の棒を入手（エンチャント付きで光る特別なアイテム）
  - `/inspect` - エイリアスコマンド
  - 調査モード中または調査の棒でブロックをクリックすると所有者情報を表示
  - 表示内容: ブロック種類、位置、所有者名、設置日時、信頼関係
- 懸賞金看板リスナー（BountySignListener）- 看板に[bounty]と入力で登録、OP権限で設置/破壊可能
- 灰色プレイヤー用の称号を追加（Scoundrel, Rogue, Outlaw, Renegade）
- 管理者コマンド `/noty admin listgray` - 全灰色プレイヤー一覧（オンライン/オフライン状態表示）
- 管理者コマンド `/noty admin listred` - 全赤プレイヤー一覧（オンライン/オフライン状態表示）
- クリエイティブモード除外機能（所有権登録・犯罪判定をスキップ）
- 灰色プレイヤーへの村人警告システム（5%確率、2分クールダウン、6種類のメッセージ）
- ゴーレム独立検知機能（視認範囲32ブロック内で赤プレイヤーを検知、灰色プレイヤーの犯罪を目撃して攻撃）
- 犯罪通知システム（犯罪時・ネームカラー変更時にプレイヤーへ通知、被害者名表示）
- ゴーレム帰還機能（ターゲットを倒した/見失った後、村に戻る）
- ゴーレム自動強化機能（スポーン時に全ゴーレムを強化、攻撃されるとテレポートして反撃）
- 村人ベッド破壊犯罪（村人の紐づいているベッドを壊すとゴーレムに通報、Alignment -5）
- 村人仕事場破壊犯罪（村人の紐づいている職業ブロックを壊すとゴーレムに通報、Alignment -5）
- 村人攻撃犯罪（村人を攻撃するとゴーレムに通報、攻撃ごとにAlignment -1）
- ゴーレム殺害犯罪（ゴーレムを殺すと周囲のゴーレムが攻撃、Alignment -50）
- モンスター討伐報酬（モンスターを倒すとAlignment +1）

### Removed
- サイドバー表示機能を削除

## [0.2.0] - 2024-12-26

### Added
- サイドバーに称号・ステータス表示機能（2行固定）
- ペット攻撃ルール: 青色プレイヤーのペット攻撃でCrimePoint+150

### Changed
- 動物殺害ルール変更: 青色プレイヤーはペナルティなし、灰色/赤色はKarma+20のみ
- CrimePoint時間減少: 1時間ごとに-10、赤プレイヤーは-1000でPKCount-1

### Fixed
- PlayerData.addCrimePoint()を修正し赤プレイヤーのマイナス値を許可
- Skillsプラグインとの競合回避（既存Objectiveを共有）

## [0.1.0] - 2024-12-25

### Added
- 初期リリース
- 名声システム（Notoriety System）の実装
  - ネームカラー: 青（Innocent）、灰（Criminal）、赤（Murderer）
  - CrimePoint、PKCount、Karma、Fame パラメータ
  - 称号システム（Trustworthy, Notable, Famous, Lord 等）
- 所有権システム
- 信頼システム
- 犯罪履歴システム
- 懸賞金システム
- 村人・アイアンゴーレム連携
- コマンド: `/noty status`, `/noty history`, `/noty bounty`, `/noty trust`, `/noty admin`
- サードパーティAPI
- GitHub Actions によるビルド自動化
