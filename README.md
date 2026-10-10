# KGUnlock

> 解锁酷狗概念版（`com.kugou.android.lite`）在境外的歌曲试听 / 下载限制。
>
> 基于 **libxposed API 101** 的 LSPosed / LSPatch 模块。

[![Build](https://github.com/potato1778/KGUnlock/actions/workflows/build.yml/badge.svg)](../../actions)

---

## 原理

酷狗的地区判定链（逆向自 3.3.5 / 4.1.7）：

```
① com.kugou.common.network.a.a   请求 ip2.kugou.com/api/v1/overseas/check_v2
② com.kugou.common.network.a.e   解析响应里的 area_code / is_special_vip
③ com.kugou.common.network.a.c   把结果写回本地（key 0x50 / 0x5e）
④ com.kugou.common.environment.a 读本地值：
      ay() -> area_code（缺省 "1"）
      aJ() -> is_special_vip（缺省 0）
⑤ com.kugou.android.app.g.a(I)   闸门：命中版权限制时返回 true
⑥ ErrorCode_1011                  "版权限制，境外不允许下载试听歌曲"
```

关键点在于：**服务端虽然能通过 IP / GPS 知道你在境外，但下载与试听的鉴权只认
客户端上报的 `area_code`。** 下面是一次真实抓包的服务端返回：

```json
{
  "info": {
    "province": "<redacted>",
    "country": "<redacted>",
    "area_code": "5",        // 服务端按 IP 判定
    "is_cn": 0,              // 服务端说「不在中国」
    "area_original": "5",
    "area_priv": "1",        // ← 客户端上报 area_code=1 后，服务端标记为 1
    "is_special_vip": 1,
    "vip_switch": 1
  },
  "errcode": 0,
  "status": 1
}
```

`area_priv` 是下载 / 试听的真正判据，它由客户端上报的 `area_code` 决定。
因此**在客户端伪造即可解锁，与网络位置无关**。

### Hook 一览

| # | 目标方法 | 行为 | 作用 |
|---|----------|------|------|
| 1 | `com.kugou.common.environment.a.ay()` | 返回 `"1"` | **核心**：伪造地区为中国大陆 |
| 2 | `com.kugou.common.environment.a.aJ()` | 返回 `0` | 清除特殊 VIP 标志 |
| 3 | `com.kugou.android.app.g.a(int)` | 返回 `false` | 压制版权限制闸门 |
| 4 | `com.kugou.common.network.a.c.a(f)` | 拦截 | **必需**：阻止服务端把真实区域写回本地 |
| 5 | `com.kugou.common.network.a.c.a(f,Hashtable,boolean)` | 剥离经纬度 | 防止真实 GPS 覆盖判定 |
| 6 | `com.kugou.common.network.a.f.b(String)` | 仅日志 | 诊断：观察服务端返回值 |

> ⚠️ 第 4 项是必须的第二道防线。若只做第 1 项，服务端下次回包会把本地的
> `area_code` 改回真实值，限制会重新出现。

---

## 已验证环境

| 项目 | 值 |
|------|-----|
| 酷狗概念版版本 | `3.3.5`（versionCode 10759）、`4.1.7`（versionCode 11170） |
| 目标包名 | `com.kugou.android.lite` |
| 宿主框架 | LSPosed（root）/ LSPatch（免 root，本地模式） |
| 实测机型 | Android 10 / SDK 29、Android 15 / SDK 35，arm64-v8a |

实测结果：原本受限的 VIP 曲目（Carpenters《Yesterday Once More》）可正常播放，
无 `ErrorCode_1011`、无地区限制弹窗。

---

## 安装

### 方式 A：LSPosed（需 root）

1. 安装本模块 APK；
2. 在 LSPosed 管理器中启用模块；
3. 作用域勾选 **酷狗概念版**；
4. 强制停止酷狗概念版后重新打开。

### 方式 B：LSPatch（免 root）

1. 把 `KGUnlock.apk` 放入 LSPatch 的模块目录
   （`/sdcard/Android/data/org.lsposed.lspatch/files/modules/`），
   或在 LSPatch 内「管理模块」→ 从本地导入；
2. LSPatch 首页 → **本地模式**（Local）→ 选择酷狗概念版 APK；
3. 勾选 **KGUnlock** 作为模块，开始打包；
4. 打包完成会生成新的 APK，卸载原版后安装即可。

> 本地模式需要一份酷狗概念版的 APK。可从已安装的应用里提取
> （`adb shell pm path com.kugou.android.lite`）。

---

## 自行构建

```bash
git clone https://github.com/potato1778/KGUnlock.git
cd KGUnlock
echo "sdk.dir=/path/to/Android/sdk" > local.properties
./gradlew :app:assembleRelease
# 产物：app/build/outputs/apk/release/app-release-unsigned.apk
```

需要 JDK 17 + Android SDK（compileSdk 35）。

产物未签名。自用可直接用 `uber-apk-signer` 或 `apksigner` 签名后安装；
LSPatch 打包时也会自行处理签名。

---

## 局限与已知问题

- **仅针对酷狗概念版**：包名写死在 `Config.TARGET_PACKAGE`，
  酷狗音乐标准版（`com.kugou.android`）类名不同，需另行适配。
- **依赖类名 / 方法签名**：酷狗更新后若重命名这些类，hook 会失效。
  若日志出现 `hook FAIL: ...`，说明该类在该版本中已变动。
- **部分方法被加固**：`g.a(int)` 在 4.1.7 上被腾讯 Sword 保护，
  但本模块采用整体替换式 hook，不依赖其内部实现，仍然有效。
- **只解锁「境外限制」**：不解锁会员 / 付费曲库，
  受版权方单独限制的曲目仍然无法播放。

---

## 免责声明

本项目仅供**学习与研究**使用，用于了解 Android 应用的地域判定实现方式。

- 请勿用于商业用途；
- 请在下载后 **24 小时内删除**；
- 使用本工具可能违反目标软件的服务条款，请自行承担相应风险；
- 作者不对因使用本工具造成的任何后果负责。

如果喜欢酷狗，请支持正版。

---

## License

[MIT](LICENSE)
