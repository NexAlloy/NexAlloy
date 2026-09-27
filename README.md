<div align="center">
  <h1>NexAlloy</h1>
  <a href="https://discord.gg/QWUrAA2mKq"><img alt="Discord Server" src="https://img.shields.io/badge/Discord%20Server-5865F2.svg?logo=discord&logoColor=white"></a>
  <a href="https://t.me/revancedxposed"><img alt="Telegram Channel" src="https://img.shields.io/badge/Telegram_Channel-blue.svg?logo=telegram&logoColor=white"></a>
  <a href="https://github.com/NexAlloy/NexAlloy/releases/latest"><img alt="GitHub Downloads" src="https://img.shields.io/endpoint?url=https%3A%2F%2Fshields.chsbuffer.workers.dev%2F%3Frepos%3DNexAlloy%2FNexAlloy%26cacheSeconds%3D3600"></a>
  <a href="https://github.com/NexAlloy/NexAlloy"><img alt="GitHub Stars" src="https://img.shields.io/github/stars/NexAlloy/NexAlloy"></a>  
  <br>
</div>

**ChsBuffer's LSPosed module, powered by Morphe, ReVanced, and beyond.**  
> [!CAUTION]
> **Migration Notice:** This project has evolved from **ReVancedXposed** to **NexAlloy**. 
> 
> **Upgrading:** We’ve kept the original Package ID for your convenience. You can install this as an update, but **you must manually export your settings from the old version and import them into the new one** to keep your configuration.

>[!IMPORTANT]  
> - This is **NOT an official Morphe or ReVanced project**, do not ask their developers for help.  
> - **Root access** is strictly **required** to use this module!
> - **Having issues?** Check the **[FAQ](https://github.com/NexAlloy/NexAlloy/wiki/Frequently-Asked-Questions)** before reporting.

## Downloads
- **Release build**: [Download](https://github.com/NexAlloy/NexAlloy/releases/latest)
- **Nightly build**: [Download](https://nightly.link/NexAlloy/NexAlloy/workflows/android/main)

<sub>If you've joined the YouTube beta program, please try the nightly build before reporting an issue.</sub>

## Patches

### YouTube
- Allow screenshots and screen recording (optional)
- Remove ads
- SponsorBlock
- Remove background and screen-off playback restrictions
- Remove share links tracking query parameter
- Hide and change navigation buttons
- Swipe controls
- Remember video quality changes
- Show video quality button
- Show advanced video quality menu
- Copy video url video player button
- Open external downloader app
- Custom playback speed
- Remember playback speed
- Playback speed dialog button
- Hide layout components
- Hide video action buttons
- Disable Shorts resuming on startup
- Disable video codecs
- Disable auto captions
- Alternative thumbnails
- Bypass image region restrictions

### YouTube Music
- Allow screenshots and screen recording (optional)
- Remove music video ads
- Remove background playback restrictions
- Hide upgrade button
- Hide 'Get Music Premium' label
- Enable exclusive audio playback

### Reddit
- Start as guest
- Allow screenshots and screen recording (optional)
- Hide ads
- Sanitize sharing links
- Open external links directly without Reddit redirect wrappers
- Open external links in the default browser (optional)
- Remove repeat NSFW community warning (optional)

### Google Photos
- Allow screenshots and screen recording (optional)
- Spoof Pixel XL

### Photomath
- Allow screenshots and screen recording (optional)
- Unlock plus

### Instagram
- Allow screenshots and screen recording (optional)
- Hide ads
- Enable native media downloads
- Sanitize shared Instagram URLs
- Disable DM/story screenshot detection (optional)
- Disable automatic story flipping (optional)

### Threads
- Allow screenshots and screen recording (optional)
- Hide ads
- Disable common analytics (optional)
- Block common display-ad SDK loads (optional)

### Messenger
- Allow screenshots and screen recording (optional)
- Hide inbox ads
- Disable common analytics (optional)
- Block common display-ad SDK loads (optional)

### Facebook
- Allow screenshots and screen recording (optional)
- Hide story ads
- Disable common analytics (optional)
- Block common display-ad SDK loads (optional)

### X (Twitter)
- Allow screenshots and screen recording (optional)
- Hide promoted and RTB timeline entries
- Remove session tracking token from shared links
- Block redirecting to X Lite

### Telegram
- Remove sponsored messages, channel/video ads, sponsored search results, and MTProxy sponsor dialogs
- Enhanced forward presets:
  - Normal
  - Without sender attribution
  - Without caption/text
  - Without sender + caption
  - Optionally remember the last forward preset
- Per-chat stealth and Ghost exceptions from the profile toolbar
- Privacy controls:
  - Hide online status on this device
  - Hide message read receipts
  - Hide listened/content-read receipts
  - Hide typing status
  - Separate voice/video recording and upload controls
  - Hide photo/file upload activity
  - Hide round-video activity
  - Hide location/contact/sticker selection activity
  - Hide game, emoji interaction, and emoji acknowledgement activity
  - Hide speaking status in group calls
  - Split story read receipts and story-view increments
  - Hide screenshot notifications in supported secret-chat flows
  - Master switch to hide all chat activity
- Local history controls:
  - Keep remotely deleted messages locally while allowing deletions initiated on this device
  - Keep the original local text when another participant edits a message while allowing edits initiated on this device
- Download speed boost: Medium or Maximum
- Force-disable video/GIF autoplay (optional global override; Telegram's native control remains available)
- Clean common tracking parameters from outgoing links
- Open ordinary external links in the system browser while preserving Telegram deep links
- Notification privacy:
  - Disable notification Mark as read
  - Quick reply without marking the dialog as read
  - Force-hide notification message previews
- Confirm outgoing user and group calls
- Optional profile tools to copy internal dialog ID and runtime diagnostics (version, account, DC, proxy, last TL request)
- Supports Play Store, direct-download, and beta packages

### TikTok
- Allow screenshots and screen recording (optional)
- Hide feed ads
- Show video seekbar
- Enable playback speed controls
- Enable client-side downloads
- Disable forced login
- Fix Google login
- Sanitize sharing links
- Open supported profile/story links in the system browser (optional)
- Remember Clear Display across videos (optional)
- Disable screenshot/screen-recording detection callbacks (optional)
- Stop automatic video looping (optional)
- Hide quick comment reactions (optional)
- Disable long-press quick share (optional)
- Disable long-press repost (optional)
- Disable common analytics (optional)
- Supports both `com.zhiliaoapp.musically` and `com.ss.android.ugc.trill`

### Twitch
- Allow screenshots and screen recording (optional)
- Block client-side audio ads
- Block client-side video ads
- Hide banner, overlay and in-feed display ads
- Auto-claim Channel Points
- Show deleted chat messages using spoiler behavior
- Enable internal debug mode (optional, disabled by default)
- Disable common analytics (optional)

### Spotify
- Allow screenshots and screen recording (optional)
- Allow runtime audio capture (experimental, disabled by default)
- Disable common analytics (optional)

### Duolingo
- Allow screenshots and screen recording (optional)
- Disable ads
- Disable dynamic launcher icon changes (optional)
- Disable common analytics SDK calls (optional)

### Busuu
- Allow screenshots and screen recording (optional)
- Disable common analytics SDK calls (optional)
- Block common banner/interstitial ad SDK loads (optional)

### CamScanner
- Allow screenshots and screen recording (optional)
- Disable CamScanner LogAgent telemetry
- Disable common analytics SDK calls (optional)
- Block common banner/interstitial ad SDK loads (optional)

### ProtonVPN
- Allow screenshots and screen recording (optional)
- Remove client-side server-change delay
- Disable common analytics SDK calls (optional)
- Does not unlock paid servers, custom DNS, LAN, or split tunneling

### Swift Backup
- Allow screenshots and screen recording (optional)
- Disable common analytics SDK calls (optional)

### Truecaller
- Allow screenshots and screen recording (optional)
- Hide after-call and caller-ID ads
- Disable CleverTap behavioural analytics
- Disable app-start telemetry
- Disable known third-party analytics/ad SDK initialization (optional)
- Block common display-ad SDK loads (optional)
- Hide full-screen upgrade prompts (optional)
- Remove Premium navigation/profile UI (optional)
- Hide Premium settings block (optional)
- Hide Assistant tab (optional)
- Hide Family Protection button (optional)
- Hide Scams tab (optional)
- Disable in-app update nag (optional)

### Inshorts
- Allow screenshots and screen recording (optional)
- Hide ads

### Hex Editor
- Allow screenshots and screen recording (optional)
- Disable ads

### Strava
- Allow screenshots and screen recording (optional)
- Unlock subscription features
- Disable subscription suggestions

### AllTrails
- Allow screenshots and screen recording (optional)
- Enable Peak membership

## Supports
[![Discord Server](https://img.shields.io/badge/Join-Discord-5865F2.svg?logo=discord)](https://discord.gg/QWUrAA2mKq)  
[![FAQ](https://img.shields.io/badge/Read-FAQ-orange.svg?logo=github)](https://github.com/NexAlloy/NexAlloy/wiki/Frequently-Asked-Questions)  
or [Create an issue](https://github.com/NexAlloy/NexAlloy/issues/new/choose)

## ⭐ Credits

[DexKit](https://luckypray.org/DexKit/en/): a high-performance dex runtime parsing library.  
[Morphe](https://morphe.software): Transform Your Android Apps  
[ReVanced](https://revanced.app): Continuing the legacy of Vanced at [revanced.app](https://revanced.app)
