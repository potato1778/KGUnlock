# KGUnlock

Unlock the regional restrictions of **Kugou Concept/Lite edition** (`com.kugou.android.lite`) — lift the overseas limits on song preview and download. An LSPosed / LSPatch module built on **libxposed API 101**.

**This module has no settings UI.** Install it, enable it in your framework, and tick Kugou Concept/Lite in its scope — it takes effect immediately with zero configuration.

[![Build](https://github.com/potato1778/KGUnlock/actions/workflows/build.yml/badge.svg)](../../actions)

---

## How it works

Kugou's region-check chain (reverse-engineered from v3.3.5 / v4.1.7):

```
① network.a.a   requests ip2.kugou.com/api/v1/overseas/check_v2
② network.a.e   parses area_code / is_special_vip from the response
③ network.a.c   writes the result back to local storage (keys 0x50 / 0x5e)
④ environment.a reads the local value: ay() -> area_code, aJ() -> is_special_vip
⑤ app.g.a(int)  the gate: returns true when a copyright restriction is hit
⑥ ErrorCode_1011  "Copyright restriction: download/preview not allowed overseas"
```

The key insight: **although the server can tell you are overseas via IP / GPS, the authorization for download and preview only trusts the `area_code` reported by the client.** Packet capture evidence:

```json
{ "info": {
  "area_code": "5", "is_cn": 0,        // server flags you as overseas by IP
  "area_priv": "1"                      // ← once the client reports area_code=1, the server flips this to 1
}}
```

`area_priv` is the real criterion, and it is *decided by the client-reported `area_code`*. Therefore **spoofing it on the client unlocks playback, independent of your network location**.

### Hook overview

| # | Target method | Behavior | Effect |
|---|---------------|----------|--------|
| 1 | `environment.a.ay()` | returns `"1"` | **Core**: fake the region as Mainland China |
| 2 | `environment.a.aJ()` | returns `0` | clear the special-VIP flag |
| 3 | `app.g.a(int)` | returns `false` | suppress the copyright-restriction gate |
| 4 | `network.a.c.a(f)` | intercept | **Required**: stop the server from writing the real region back locally |
| 5 | `network.a.c.a(f,Hashtable,boolean)` | strip lat/lng | prevent real GPS from overriding the check |
| 6 | `network.a.f.b(String)` | log only | diagnostic: observe server responses |

> Hook #4 is the essential second line of defense — if you only apply #1, the server's next response will rewrite the local `area_code` back to the real value and the restriction returns.

---

## Verified environment

- Kugou Concept/Lite `3.3.5` (vc 10759) / `4.1.7` (vc 11170), target package `com.kugou.android.lite`
- Host: LSPosed (root) / LSPatch (rootless, local mode)
- Devices: Android 10 / SDK 29, Android 15 / SDK 35, arm64-v8a

In practice: previously restricted VIP tracks play normally, with no `ErrorCode_1011` and no region-limit popup.

---

## Installation

**A. LSPosed (requires root):** Install the module APK → enable it in LSPosed → tick Kugou Concept/Lite in its scope → force-stop and restart Kugou.

**B. LSPatch (rootless):** Put `KGUnlock.apk` into the LSPatch module directory (or import it locally via "Manage Modules") → on the home screen use "Local Mode" and select the Kugou Concept/Lite APK → tick KGUnlock and repackage → uninstall the original then install the repackaged APK. Local mode requires you to supply a Kugou Concept/Lite APK yourself (extract from an installed app: `adb shell pm path com.kugou.android.lite`).

---

## Build it yourself

```bash
git clone https://github.com/potato1778/KGUnlock.git
cd KGUnlock
echo "sdk.dir=/path/to/Android/sdk" > local.properties
./gradlew :app:assembleRelease
```

Requires JDK 17 + Android SDK (compileSdk 35). The output is unsigned; for personal use sign it with `apksigner` / `uber-apk-signer`, or let LSPatch handle signing during repackaging.

---

## Limitations

- **Kugou Concept/Lite only**: the target package is `Config.TARGET_PACKAGE`. The standard edition (`com.kugou.android`) uses different class names and needs separate adaptation.
- **Depends on class / method signatures**: if a Kugou update renames the relevant classes, the hooks break (a `hook FAIL` in the log means that class has changed).
- **Some methods are hardened**: `g.a(int)` is protected by Tencent Sword on v4.1.7, but this module uses whole-method replacement hooks that do not depend on its internals, so it still works.
- **Unlocks "overseas restriction" only**: it does not unlock membership / paid libraries; tracks separately restricted by the copyright holder still cannot play.

---

## Disclaimer

For learning and research only, to understand how Android apps implement regional checks. Not for commercial use. Delete within 24 hours of download. Usage may violate the target software's terms of service — use at your own risk. The author is not responsible for any consequences. If you like Kugou, please support the official version.

## License

[MIT](LICENSE)
