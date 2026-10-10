package com.kgunlock;

import android.util.Log;

import io.github.libxposed.api.XposedInterface;

/**
 * 拦截服务端回包对本地状态（area_code / is_special_vip）的写回。
 *
 * <p>{@code com.kugou.common.network.a.c.a(f)} 负责把服务端结果落到
 * SharedPreferences（key 0x50 = area_code，key 0x5e = vip）。
 *
 * <p>若不禁用它，服务端仍会按真实 IP 把 area_code 改回 "5"，
 * 下一次判定就会重新触发限制 —— 这是必须的第二道防线。
 *
 * <p>本 hook 不调用 {@code chain.proceed()}，即完全阻断写回。
 */
final class WriteBackHooker implements XposedInterface.Hooker {

    @Override
    public Object intercept(XposedInterface.Chain chain) {
        Log.i(Entry.TAG, "WRITEBACK blocked (area_code/vip preserved)");
        return null; // 目标方法返回 void
    }
}
