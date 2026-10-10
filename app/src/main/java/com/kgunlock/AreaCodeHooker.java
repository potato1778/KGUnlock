package com.kgunlock;

import android.util.Log;

import io.github.libxposed.api.XposedInterface;

/**
 * 伪造地区码 —— 模块的核心。
 *
 * <p>原实现从 SharedPreferences 读取，返回真实地区（示例值 = "5"）。
 * 这里固定返回 {@link Config#FAKE_AREA_CODE}（中国 = "1"），
 * 使服务端在鉴权时把 {@code area_priv} 标记为中国大陆，从而放行受限曲目。
 *
 * <p>注意：本 hook <b>不调用</b> {@code chain.proceed()}，即完全阻断原方法。
 */
final class AreaCodeHooker implements XposedInterface.Hooker {

    @Override
    public Object intercept(XposedInterface.Chain chain) {
        if (Config.VERBOSE_LOG) {
            Object original = null;
            try {
                original = chain.proceed();
            } catch (Throwable ignored) {
                // 原方法异常时仍继续伪造
            }
            Log.i(Entry.TAG, "ay() original=" + original);
            Log.i(Entry.TAG, "ay() -> " + Config.FAKE_AREA_CODE + " (forced)");
        }
        return Config.FAKE_AREA_CODE;
    }
}
