# 🎲 LudoMax

Offline mini **Ludo** for Android — 9×9 board, 2 tokens per player, 2–4 players,
vs Computer (Easy / Hard) or Pass & Play. Music, sound effects, animations,
Privacy Policy and Terms & Conditions included. **No internet permission, no ads, no tracking.**

## Rules implemented
- Roll a **6** to bring a token out of the yard onto your start cell.
- Tokens travel clockwise, then enter their own coloured home column and the centre.
- **Exact roll** needed to reach the centre.
- Landing on an opponent (on an unsafe cell) sends it back to its yard.
- Start cells and ★ star cells are **safe**. Two tokens of one colour on a cell protect each other.
- A **6**, a **capture** or **reaching the centre** gives another turn. **Three 6s in a row** cancel the turn.
- Players are ranked in the order they bring both tokens home.

## Build on GitHub (no Android Studio needed)
1. Create a new GitHub repository and upload **all files of this folder** (keep the folder structure, including the hidden `.github` folder).
2. Open the **Actions** tab → *Build LudoMax* runs automatically (or press **Run workflow**).
3. When it finishes, open the run → **Artifacts**:
   - `LudoMax-debug-apk` → install on your phone to test.
   - `LudoMax-release-aab` → the file you upload to Google Play (needs signing, see below).

## Before publishing (important)
1. **Fill your details** in the Privacy Policy / Terms (currently `{{placeholders}}`):
   ```
   ./scripts/set_contact.sh "Your Name" "you@email.com" "India"
   ```
   (or edit `app/src/main/assets/*.html` and `docs/*.html` by hand).
2. **Host the Privacy Policy** (Play Console needs a URL): GitHub → *Settings → Pages → Deploy from branch → `/docs`*.
   Your URL will be `https://<username>.github.io/<repo>/privacy_policy.html`.
3. **Create a signing key** (once, keep it safe — losing it means you cannot update the app):
   ```
   keytool -genkeypair -v -keystore ludomax.jks -alias ludomax -keyalg RSA -keysize 2048 -validity 10000
   base64 -w0 ludomax.jks      # copy the output
   ```
   In GitHub → *Settings → Secrets and variables → Actions* add:
   `LUDOMAX_KEYSTORE_BASE64`, `LUDOMAX_KEYSTORE_PASSWORD`, `LUDOMAX_KEY_ALIAS`, `LUDOMAX_KEY_PASSWORD`.
   The next workflow run produces a **signed** release AAB.
4. **Play Console**: upload the AAB, use `store/play-store-icon-512.png` (icon) and
   `store/feature-graphic-1024x500.png` (feature graphic), take phone screenshots, paste texts from
   `store/PLAY_STORE_LISTING.md`. Data safety: *no data collected, no data shared*.
   Content rating questionnaire: no violence/gambling (it is a dice board game with no real money).

## Project layout
| Path | What |
|---|---|
| `app/src/main/java/com/ludomax/game/engine` | Pure Kotlin rules, board geometry, Computer player |
| `.../ui/GameView.kt` | Custom-drawn board, dice, tokens, animations, confetti |
| `.../audio/SoundManager.kt` | Music loop + sound effects |
| `app/src/main/res/raw` | Generated music & effects (`scripts/generate_audio.py`) |
| `scripts/generate_icons.py` | Logo, launcher icons, store graphics |
| `app/src/main/assets`, `docs/` | Privacy Policy, Terms, How to play |

Package name: `com.ludomax.game` (change `applicationId` and `namespace` in `app/build.gradle.kts` if it is taken on Play).
Version: bump `versionCode` / `versionName` in `app/build.gradle.kts` for every Play update.
