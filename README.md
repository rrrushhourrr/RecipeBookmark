# レシピブックマーク

Instagram・X・クラシル・料理本の写真・Gemini の提案レシピなど、あちこちに散らばった
お気に入りレシピを 1 つに集めるための Android アプリです。
「保存 → フォルダ整理 → 探す」を、スマホ 1 台で完結させます。

- サーバー・アカウント・広告・解析はありません。データは端末の中だけで完結します。
- Instagram / X の本文自動取得、スクレイピング、非公式 API、ログイン回避は行いません。
  公開ページの OGP（og:title / og:image）だけを読みます。

---

## 1. APK の入手（GitHub だけで完結します）

**PC も Android Studio も要りません。** GitHub Actions がビルドして、
Releases に APK を貼ります。スマホのブラウザから直接ダウンロードできます。

### いちばん簡単な流れ

このリンクをスマホでブックマークしておいてください。常に最新の APK があります。

```
https://github.com/rrrushhourrr/RecipeBookmark/releases/latest
```

1. 上のリンクを開く
2. 「Assets」の中の **`recipebookmark.apk`** をタップしてダウンロード
3. あとは「3. 実機へのインストール」へ

APK は `main`（既定ブランチ）に push されるたびに自動で作り直されます。
手動で作りたいときは **Actions タブ → 「APK をビルド」→ Run workflow**。

### ビルドの状態を見る

Actions タブに実行履歴が出ます。失敗していたら、赤い実行を開くと
どのステップで落ちたかが読めます。

---

## 2. GitHub だけで署名を用意する（最初に 1 回だけ）

**レシピを貯め始める前に、必ずこれをやってください。**

Android はアプリを「署名」で見分けます。署名が変わると別のアプリとみなされ、
上書き更新ができなくなります（＝入れ直しになり、保存したレシピが消えます）。
署名用の鍵（keystore）を固定しておけば、何度更新してもレシピは残ります。

> Secret を設定しないままでも APK はできます。ただし debug 鍵で署名されるので、
> **ビルドごとに署名が変わり上書き更新できません。** お試し用と考えてください。

### 2-1. まずリポジトリを private にする

秘密鍵を扱うので、public のままでは危険です（public だと Actions の成果物を
誰でもダウンロードできてしまいます）。

**Settings → General → 最下部の Danger Zone → Change repository visibility → Make private**

個人用のレシピアプリなので private で困ることはありません。
private でない場合、次の手順は安全のため自動で止まります。

### 2-2. keystore を作る

1. **Actions タブ → 「署名用の keystore を作る（手動・1回だけ）」**
2. 右の **Run workflow** を押す
3. 確認欄に `はい` と入力して実行

パスワードは自動生成されます（入力欄に打つと実行履歴に残ってしまうため）。

### 2-3. Secret を 4 つ登録する

1. 実行が終わったら、そのページの下にある成果物
   **`signing-keystore-SECRET-DELETE-AFTER-USE`** をダウンロード（zip）
2. 中の **`READ-ME-FIRST.txt`** を開く。登録すべき 4 つの値が書いてあります
3. **Settings → Secrets and variables → Actions → New repository secret** で登録

| Secret 名 | 中身 |
|---|---|
| `KEYSTORE_BASE64` | `KEYSTORE_BASE64.txt` の中身を全部（改行なしの長い 1 行） |
| `KEYSTORE_PASSWORD` | `READ-ME-FIRST.txt` に書かれたパスワード |
| `KEY_ALIAS` | `recipebookmark` |
| `KEY_PASSWORD` | `KEYSTORE_PASSWORD` と同じ値 |

### 2-4. 鍵を控えて、成果物を消す

**これが一番大事です。** zip の中の `release.jks` と 4 つの値を、
パスワードマネージャ（1Password / Bitwarden など）に保存してください。
`release.jks` をファイルとして添付し、パスワードも同じ項目に書いておくのが確実です。

控えたら、成果物を削除します（Actions → その実行 → 右上の `…` → Delete artifact）。
成果物は 1 日で自動的に消えますが、手で消しておくほうが安全です。

> **鍵を失うと二度と上書き更新できません。** GitHub の Secret は登録後は
> 中身を読み出せないので、Secret に入れただけでは控えたことになりません。

### 2-5. 署名済み APK を作る

**Actions → 「APK をビルド」→ Run workflow**

まとめ欄に「署名: 固定 keystore ✅」と出れば成功です。
以降はこの APK を上書きインストールしてもレシピは消えません。

---

## 3. 実機へのインストール

### 方法 A: スマホだけで完結（おすすめ）

1. スマホの Chrome で Releases を開く
   → `https://github.com/rrrushhourrr/RecipeBookmark/releases/latest`
2. Assets の **`recipebookmark.apk`** をタップしてダウンロード
   （「ファイルの種類が原因で端末を破損する可能性があります」と出たら「OK」）
3. 通知、またはファイルアプリの「ダウンロード」から APK をタップ
4. **「不明なアプリのインストール」の許可**を求められるので、こう進みます
   - 出てきた「設定」ボタンをタップ
   - （手で辿る場合）設定 → アプリ → 特別なアプリアクセス → 不明なアプリのインストール
   - **APK を開いたアプリ**（Chrome、Files など）を選ぶ
   - 「この提供元のアプリを許可」をオンにする
5. 戻ってもう一度 APK をタップ → インストール

Play Protect の警告（「このアプリの開発元は不明です」）が出たら
「詳細 → 無視してインストール」で進めます。自分でビルドした APK なので問題ありません。

インストールが終わったら、手順 4 の許可はオフに戻しておくと安心です。

### 更新するとき

同じ手順で新しい APK をタップするだけです。
**「2. GitHub だけで署名を用意する」を済ませてあれば、レシピは消えません。**

### 方法 B: PC から USB で入れる

1. 「設定 → デバイス情報 → ビルド番号」を 7 回タップして開発者オプションを出す
2. 「設定 → システム → 開発者向けオプション → USB デバッグ」をオン
3. PC と USB でつなぎ、端末に出る「USB デバッグを許可しますか？」で許可
4. インストール

```bash
adb install -r recipebookmark.apk
```

`-r` は上書き（再インストール）です。署名が同じならレシピは残ります。

---

## 4. 手元の PC でビルドする場合（任意）

GitHub Actions で完結するので普段は不要ですが、ローカルでも同じものが作れます。

### 必要なもの

| | |
|---|---|
| JDK | 17 以上（Android Studio 同梱の JBR でも可） |
| Android SDK | Platform API 35 と Build-Tools。Android Studio の SDK Manager で入ります |
| Gradle | 不要（同梱の Gradle Wrapper が自動で用意します） |

`local.properties` に SDK の場所を書きます（Android Studio で開けば自動生成されます）。

```properties
sdk.dir=/Users/you/Library/Android/sdk
```

### ビルドコマンド

リポジトリのルートで実行します。

```bash
# リリース版（実機に入れるのはこちら）
./gradlew assembleRelease

# 動作確認用のデバッグ版
./gradlew assembleDebug
```

> Windows の場合は `./gradlew` を `gradlew.bat` に読み替えてください。

### APK の出力先

```
app/build/outputs/apk/release/app-release.apk      ← リリース版
app/build/outputs/apk/debug/app-debug.apk          ← デバッグ版
```

デバッグ版は `applicationId` に `.debug` が付くので、リリース版と同時に
インストールして比べることができます。

### compileSdk / targetSdk を上げるには

SDK のバージョンは `gradle/libs.versions.toml` の先頭 1 か所にまとまっています。

```toml
compileSdk = "35"
targetSdk  = "35"
minSdk     = "31"
```

新しい Android に追従したいときは、SDK Manager で該当の Platform を入れてから
この数値を上げるだけです（併せて `agp` のバージョンも上げる必要がある場合があります）。
minSdk 31 なので Android 12 以降の端末で動きます。

### リリース署名（ローカル）

#### 4-1. keystore を作る

```bash
keytool -genkeypair -v \
  -keystore recipebookmark-release.jks \
  -alias recipebookmark \
  -keyalg RSA -keysize 4096 \
  -validity 10950 \
  -storetype PKCS12
```

対話で聞かれるパスワードと名前を入力します（有効期限は 30 年にしてあります）。
できあがった `recipebookmark-release.jks` はリポジトリのルートに置きます。

#### 4-2. keystore.properties を用意する

同梱の `keystore.properties.sample` をコピーして値を埋めます。

```bash
cp keystore.properties.sample keystore.properties
```

```properties
storeFile=recipebookmark-release.jks
storePassword=（4-1 で決めたストアのパスワード）
keyAlias=recipebookmark
keyPassword=（4-1 で決めた鍵のパスワード）
```

これで `./gradlew assembleRelease` が固定 keystore で署名します。
`keystore.properties` が無い場合もビルドは通りますが、そのときは debug 鍵で署名されます
（お試し用。継続して使うなら必ず 4-1 をやってください）。

#### 4-3. keystore のバックアップ（重要）

`*.jks` と `keystore.properties` は `.gitignore` 済みで、**リポジトリには入りません**。
これは正しい状態ですが、裏を返すと *この 2 つを失うと二度とアプリを更新できません*。
次のどれかで必ず控えを取ってください。

- パスワードマネージャ（1Password / Bitwarden など）に、鍵ファイルを添付 + パスワードを保存
- 暗号化した外部ストレージや USB メモリにコピー
- 自分あての暗号化 zip をクラウドストレージに置く

```bash
# 例: パスワード付き zip にまとめる
zip -e recipebookmark-signing-backup.zip recipebookmark-release.jks keystore.properties
```

控えるのは「鍵ファイル」と「3 つのパスワード（store / key / alias 名）」の両方です。
どちらか一方だけでは復元できません。

---

## 5. 使い方

### 登録する

**共有メニューから（いちばん速い）**

Instagram / X / クラシル / Gemini などで「共有」→「レシピブックマーク」を選ぶと、
元のアプリの上に軽いシートが出ます。

- 「題名・フォルダ・料理時間」だけを選んで保存します
- **何も選ばず「保存」を押すだけでも「未分類」に入ります**
- 保存したらすぐ元のアプリに戻ります（画像の取り込みと OGP 取得は裏で続きます）
- 共有テキストに URL があれば、ホスト名から媒体を自動判定します
- URL の無いテキスト（Gemini の提案レシピなど）は本文として保存し、1 行目を題名の初期値にします

**アプリ内の「＋」から**

共有が使いづらい媒体の保険です。右下の ＋ から選べます。

- URL を貼り付ける
- クリップボードから追加（Gemini のテキストはこれが便利です）
- 写真を撮る
- 写真を選ぶ（複数選択できます。料理本のページはこれ）
- テキストで書く

写真は長辺 2048px 程度の JPEG にしてアプリ内部ストレージへコピーします。
**ギャラリー側で元の写真を消してもレシピからは消えません。**

### 探す

- 上部：フォルダのチップ（すべて / 未分類 / 各フォルダ + 件数）と検索バー
- その下：「10分以内」「15分以内」「30分以内」「未設定」のワンタップ絞り込み
- 下部（片手で届く位置）：並べ替え / お気に入りだけ表示 / リスト・グリッド切替 / ＋

並べ替えは「料理時間が短い順（未設定は最後）」「追加日（新しい順 / 古い順）」「題名順」。
検索は題名・本文・メモ・URL が対象です。

### フォルダ

初期フォルダは **メイン / 副菜 / デザート / おやつ / 丼もの / 汁物**、
それに固定の **未分類**（共有から即保存したときの行き先）です。

右上のフォルダアイコンから追加・名前変更・並べ替え・削除ができます。
フォルダを削除すると、中のレシピは「未分類」へ移動します（レシピは消えません）。
「未分類」は固定なので削除・リネームできません。

### バックアップ

設定（右上の歯車）から行います。

- **書き出し**：全データ（JSON + 画像）を 1 つの zip にして、保存先を選んで出力します
- **読み込み**：zip から復元します。「追加」（今のデータを残して足す）と
  「置き換え」（全部消してから入れ直す）を選べます

端末を買い替えるときや、アプリを入れ直す前に書き出しておくと安心です。

---

## 6. 構成

```
app/src/main/java/com/recipebookmark/
├── data/          Room（Recipe / Folder / DAO / Repository）
├── util/          画像保存・URL 判定・OGP 取得・表示整形
├── backup/        zip の書き出しと読み込み
└── ui/
    ├── home/      一覧・検索・並べ替え・絞り込み
    ├── detail/    詳細、写真のスワイプとピンチズーム
    ├── edit/      追加と編集
    ├── folder/    フォルダ整理
    ├── settings/  バックアップ
    └── share/     共有メニューの受け口（透過 Activity + シート）
```

技術スタック：Kotlin / Jetpack Compose (Material 3) / Room / Coil /
Jsoup（OGP 取得）/ kotlinx.serialization（バックアップ）。

データは Room で `recipebookmark.db`、写真はアプリ内部ストレージの `files/images/` に
ファイル名だけを DB に持つ形で保存しています。
