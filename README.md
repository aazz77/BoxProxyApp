# Box Proxy App

Based on Box4Magisk - standalone Root Android proxy manager.

User uploads proxy core binary and config; app starts the core and applies transparent proxy (TPROXY/REDIRECT).

## Features

- No bundled core - upload multiple cores (sing-box, mihomo, xray, clash, etc.)
- Fully custom start command
- Core config files separated from App settings
- Runtime copy to `/data/local/tmp/box`
- Transparent proxy: TPROXY / REDIRECT / MIXED
- blacklist / whitelist / core modes
- App multi-select with search and UID
- Auto reload rules on network change
- Emergency stop

## Requirements

- Android 8.0+ (API 26)
- **Root required**
- JDK 17

## Build

### Android Studio

1. Clone repo and open in Android Studio
2. Gradle Sync (needs Google Maven + JitPack)
3. Run on rooted device

### CLI

```bash
git clone https://github.com/aazz77/BoxProxyApp.git
cd BoxProxyApp
chmod +x gradlew
./gradlew assembleDebug
# or without wrapper jar:
gradle assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Usage

1. Install APK and grant Root
2. Files tab: upload core binary + config
3. Settings: select core/config, set start command e.g. `./sing-box run -c config.yaml`
4. Home: Start
5. Use Emergency Stop if network breaks

## License

MIT
