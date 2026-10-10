package com.kgunlock;

/**
 * 全局配置。
 *
 * <p>所有可调项集中在这里，方便 fork 后按需修改。
 */
public final class Config {

    private Config() {
    }

    /** 目标应用包名（酷狗概念版）。 */
    public static final String TARGET_PACKAGE = "com.kugou.android.lite";

    /**
     * 伪造的地区码。
     *
     * <p>酷狗的中国大陆地区码是 {@code "1"}。服务端返回体里的 {@code area_priv} 字段
     * 由客户端上报的 {@code area_code} 决定，因此把它固定成 {@code "1"} 即可解锁
     * 境外受限的歌曲试听 / 下载。
     */
    public static final String FAKE_AREA_CODE = "1";

    // ── 各 hook 的独立开关 ────────────────────────────────────────────
    // 调试时可逐个关闭以定位问题；正常使用建议全开。

    /** L1 核心：伪造 {@code environment.a.ay()} 返回的地区码。<b>必须开启</b>。 */
    public static final boolean HOOK_AREA_CODE = true;

    /** L1b：伪造 {@code environment.a.aJ()}（is_special_vip）返回 0。 */
    public static final boolean HOOK_VIP_FLAG = true;

    /** GATE：让 {@code com.kugou.android.app.g.a(int)} 直接返回 false。 */
    public static final boolean HOOK_GATE = true;

    /** L2：拦截服务端回包对本地 area_code 的写回。<b>必须开启</b>。 */
    public static final boolean HOOK_WRITEBACK = true;

    /** L3：剥离请求里的 GPS 经纬度。 */
    public static final boolean HOOK_GEO_STRIP = true;

    // ── 日志 ─────────────────────────────────────────────────────────

    /** 是否输出详细日志。发布版可关。 */
    public static final boolean VERBOSE_LOG = true;

    /** 诊断：打印服务端返回的 area_code / is_cn 等字段。 */
    public static final boolean LOG_SERVER_RESPONSE = true;
}
