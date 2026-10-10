# Changelog

本项目遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [1.0.0] - 2026-10-10

### 新增
- 首个公开版本，基于 libxposed API 101。
- 伪造地区码（`environment.a.ay()` → `"1"`），解锁境外受限曲目。
- 清除特殊 VIP 标志（`environment.a.aJ()` → `0`）。
- 压制版权限制闸门（`android.app.g.a(int)` → `false`）。
- 拦截服务端写回（`network.a.c.a(f)`），防止真实区域覆盖伪造值。
- 剥离请求中的 GPS 经纬度（`network.a.c.a(f,Hashtable,boolean)`）。
- 可选的服务端返回诊断日志（`network.a.f.b(String)`）。
- 各 hook 独立开关，便于排查。

### 已验证
- 酷狗概念版 3.3.5（Android 10）
- 酷狗概念版 4.1.7（Android 15）
- 运行于 LSPosed（root）与 LSPatch 1.2（免 root，本地模式）
