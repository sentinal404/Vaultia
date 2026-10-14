# ⚡ Vaultia v2.0.0 — Evolution Edition

Welcome to **Vaultia v2.0.0 (Evolution Edition)**! This major release delivers a complete 2026 futuristic UI overhaul, dynamic auto-switching audio player themes, true immersive fullscreen media streaming, cloud multi-select batch actions, and tactile physics-driven controls.

---

### ✨ What's New in Version 2.0.0:

#### 1. 🎨 2026 Futuristic Visual Identity & 3D Assets
- **New Official 3D Logo**: Neon-lit 3D glowing cloud and folder with orbital media rings.
- **Adaptive Circular Launcher Icons**: Clean circular mask with safe-zone insets (no square black boxes or white plates on Android 8.0–14).
- **3D Frosted Glass Folders**: Transparent 3D glassmorphic folder cards with live file counts and combined size metrics.
- **Onboarding Carousel**: First-launch onboarding with 100% transparent 3D illustrations, animated indicator pills, and "Get Started" flow.

#### 2. 🌊 Floating Curved Bottom Navigation Bar
- Custom curved dome cutout matching modern tactile mobile ergonomics.
- Elevated circular active bubble button that glides smoothly across tabs (Vault, Library, Transfers, Settings) powered by Jetpack Compose spring physics.

#### 3. 🎬 True Immersive Fullscreen Video Player
- Fixed rotation status bar issue: Enforces sticky immersive mode (`BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`) with `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`.
- Fullscreen landscape rotation completely hides the notification bar and system navigation bars.
- Dual-side HUD gestures: Left swipe for screen brightness, right swipe for media volume with debounced indicators.

#### 4. 🎵 Modern Hi-Fi Audio Player with Dynamic Anime Art & Video Loops
- **Extended Arch Container**: Reaches ~61% screen height with a deep rounded downward curve and camera cutout/notch adaptive gutters, displaying full character portraits without clipping.
- **Alternating Video & Image Mode**:
  - Automatically alternates **Anime Artwork ↔ Looping Video** per track change (`Image -> Video -> Image -> Video`).
  - Pool of **22 dynamic anime artworks** and **4 looping short videos**.
  - **100% Muted Background Video**: Zero audio bleed or mixing with the music being played.
  - **Seamless Repeat Loop**: Videos repeat infinitely until the current track finishes or changes.
  - Play/pause state is synchronized with the audio player.
  - Interactive tap-to-shuffle cycles through media in the active mode.
- **Concentric Arc Scrubber**:
  - Concentric glowing electric violet progress scrubber hugging the bottom curve.
  - Dual-ring neon thumb with quadrant-normalized polar touch gestures.
- **Bottom-Anchored Transport Controls**:
  - Minimalist white capsule dock housing Shuffle, Previous, Next, and Queue modal sheet.
  - Elevated central circular Play/Pause button with 360° radiant neon violet halo.
- **Working Queue Sheet & Skip Controls**: Dedicated playlist modal displaying all library tracks with one-tap instant switching and auto-scanning Next/Previous track navigation.
- **Streamlined Interface**: Share button removed for a focused, distraction-free player.

#### 5. 📂 Library Multi-Selection (Batch Cloud Actions)
- Long-press any file to activate multi-select mode with clean circular checkmark badges.
- Contextual top bar with safe status bar insets:
  - **Select All / Deselect All**
  - **Batch Download All** (queues all selected files in transfers engine)
  - **Batch Delete All** (bulk deletes from Telegram cloud channel with confirmation dialog)

#### 6. ⚡ Zero Unsolicited Storage & RAM Management
- Direct MTProto chunk streaming over local loopback HTTP proxy (`127.0.0.1`).
- Files are never permanently saved to device storage unless the user explicitly taps "Download".
- Automatic cache purge on memory pressure (`onTrimMemory`), background stop, and app exit (`onTerminate`).

#### 7. 🎛️ Animated Settings Toggles
- Tactile sliding switch thumb transitions with horizontal swipe gesture support (swipe right to turn ON, swipe left to turn OFF).
- Full-row clickable hitboxes across all settings and backup tiles.

---

### 📦 Release Asset:
- `vaultia-v2.0.0-release.apk` (125 MB) — Production signed APK with native C++ TDLib MTProto engine (`libtdjni.so`), hardware AES-256-GCM encryption, full offline caching, and bundled HD media assets.
