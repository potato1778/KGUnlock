# KGUnlock

> 其他语言：[English](README_EN.md)

解锁酷狗概念版（`com.kugou.android.lite`）在境外的歌曲试听 / 下载限制。基于 **libxposed API 101** 的 LSPosed / LSPatch 模块。

**本模块没有设置界面** —— 安装并在框架中启用、勾选酷狗概念版作用域后即生效，无需任何配置。

[![Build](https://github.com/potato1778/KGUnlock/actions/workflows/build.yml/badge.svg)](../../actions)

## 目录

- [原理](#原理)
  - [Hook 一览](#hook-一览)
- [已验证环境](#已验证环境)
- [安装](#安装)
- [自行构建](#自行构建)
- [局限](#局限)
- [免责声明](#免责声明)
- [License](#license)

---

## 原理

酷狗的地区判定链（逆向自 3.3.5 / 4.1.7）：

```
① network.a.a   请求 ip2.kugou.com/api/v1/overseas/check_v2
② network.a.e   解析响应里的 area_code / is_special_vip
③ network.a.c   把结果写回本地（key 0x50 / 0x5e）
④ environment.a 读取本地值：ay() -> area_code，aJ() -> is_special_vip
⑤ app.g.a(int)  闸门：命中版权限制时返回 true
⑥ ErrorCode_1011 "版权限制，境外不允许下载试听歌曲"
```

关键点：**服务端虽能通过 IP / GPS 知道你在境外，但下载与试听的鉴权只认客户端上报的 `area_code`。** 抓包佐证：

```json
{ "info": {
  "area_code": "5", "is_cn": 0,        // 服务端按 IP 判定你在境外
  "area_priv": "1"                      // ← 客户端上报 area_code=1 后，服务端改为 1
}}
```

`area_priv` 才是真正的判据，它由客户端上报的 `area_code` 决定，因此**在客户端伪造即可解锁，与网络位置无关**。

### Hook 一览

| # | 目标方法 | 行为 | 作用 |
|---|----------|------|------|
| 1 | `environment.a.ay()` | 返回 `"1"` | **核心**：伪造地区为中国大陆 |
| 2 | `environment.a.aJ()` | 返回 `0` | 清除特殊 VIP 标志 |
| 3 | `app.g.a(int)` | 返回 `false` | 压制版权限制闸门 |
| 4 | `network.a.c.a(f)` | 拦截 | **必需**：阻止服务端把真实区域写回本地 |
| 5 | `network.a.c.a(f,Hashtable,boolean)` | 剥离经纬度 | 防止真实 GPS 覆盖判定 |
| 6 | `network.a.f.b(String)` | 仅日志 | 诊断：观察服务端返回值 |

> 第 4 项是必须的第二道防线 —— 若只做第 1 项，服务端下次回包会把本地 `area_code` 改回真实值，限制会重新出现。

---

## 已验证环境

- 酷狗概念版 `3.3.5`（vc 10759）/ `4.1.7`（vc 11170），目标包名 `com.kugou.android.lite`
- 宿主：LSPosed（root）/ LSPatch（免 root，本地模式）
- 机型：Android 10 / SDK 29、Android 15 / SDK 35，arm64-v8a

实测：原本受限的 VIP 曲目可正常播放，无 `ErrorCode_1011`、无地区限制弹窗。

---

## 安装

**A. LSPosed（需 root）**：安装模块 APK → 在 LSPosed 中启用 → 作用域勾选酷狗概念版 → 强制停止并重开酷狗。

**B. LSPatch（免 root）**：把 `KGUnlock.apk` 放入 LSPatch 模块目录（或「管理模块」本地导入）→ 首页「本地模式」选酷狗概念版 APK → 勾选 KGUnlock 打包 → 卸载原版后安装新 APK。本地模式需自备一份酷狗概念版 APK（可从已装应用提取：`adb shell pm path com.kugou.android.lite`）。

---

## 自行构建

```bash
git clone https://github.com/potato1778/KGUnlock.git
cd KGUnlock
echo "sdk.dir=/path/to/Android/sdk" > local.properties
./gradlew :app:assembleRelease
```

需 JDK 17 + Android SDK（compileSdk 35）。产物未签名，自用可经 `apksigner` / `uber-apk-signer` 签名，LSPatch 打包时也会自行处理。

---

## 局限

- **仅针对酷狗概念版**：包名为 `Config.TARGET_PACKAGE`，标准版（`com.kugou.android`）类名不同，需另行适配。
- **依赖类名 / 方法签名**：酷狗更新若重命名相关类，hook 会失效（日志出现 `hook FAIL` 即说明该类已变动）。
- **部分方法被加固**：`g.a(int)` 在 4.1.7 上受腾讯 Sword 保护，但本模块用整体替换式 hook，不依赖其内部实现，仍有效。
- **只解锁「境外限制」**：不解锁会员 / 付费曲库，受版权方单独限制的曲目仍无法播放。

---

## 免责声明

仅供学习与研究，用于了解 Android 应用的地域判定实现。请勿商用；下载后 24 小时内删除；使用可能违反目标软件服务条款，风险自负；作者不对任何后果负责。喜欢酷狗请支持正版。

## License

[MIT](LICENSE)
