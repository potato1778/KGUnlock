package com.kgunlock;

import android.util.Log;

import io.github.libxposed.api.XposedInterface;

/**
 * 伪造 is_special_vip 标志位。
 *
 * <p>{@code environment.a.aJ()} 读 SharedPreferences key 0x5e，默认 0。
 * 这里固定返回 0，与 {@code aK()}（= aJ() == 1）保持一致的「非特殊 VIP」状态。
 */
final class VipFlagHooker implements XposedInterface.Hooker {

    @Override
    public Object intercept(XposedInterface.Chain chain) {
        Log.i(Entry.TAG, "aJ() -> 0");
        return 0;
    }
}
