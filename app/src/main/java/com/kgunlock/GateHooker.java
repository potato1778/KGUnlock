package com.kgunlock;

import android.util.Log;

import io.github.libxposed.api.XposedInterface;

/**
 * 压制地区限制闸门。
 *
 * <p>{@code com.kugou.android.app.g.a(int)} 是错误码到「是否拦截」的判定函数：
 * 当 VIP 状态满足且 {@code area_code != "1"} 且错误码落在 [1000, 1011] 区间时
 * 返回 true，表示命中版权限制（ErrorCode_1011 = 0x3f3 = 1011）。
 *
 * <p>直接整体替换为返回 false，等效于「永不拦截」。
 * 注意：该方法在部分版本被腾讯 Sword 加固（SwordProxy），
 * 但整体替换式 hook 不依赖其内部实现，仍然有效。
 */
final class GateHooker implements XposedInterface.Hooker {

    @Override
    public Object intercept(XposedInterface.Chain chain) {
        Log.i(Entry.TAG, "GATE -> false");
        return Boolean.FALSE;
    }
}
