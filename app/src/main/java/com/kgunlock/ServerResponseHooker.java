package com.kgunlock;

import android.util.Log;

import io.github.libxposed.api.XposedInterface;

/**
 * 诊断用：打印服务端回包里写入的 area_code。
 *
 * <p>不解锁、不改变行为，纯观察。用于确认服务端到底认不认客户端上报的地区码。
 * 典型输出：
 * <pre>
 * {"province":"<redacted>","country":"<redacted>","area_code":"5","is_cn":0,
 *  "area_original":"5","area_priv":"1", ...}
 * </pre>
 * {@code area_priv} 为 "1" 说明客户端的伪造已生效。
 */
final class ServerResponseHooker implements XposedInterface.Hooker {

    @Override
    public Object intercept(XposedInterface.Chain chain) throws Throwable {
        if (Config.VERBOSE_LOG) {
            Object value = null;
            try {
                value = chain.getArg(0);
            } catch (Throwable ignored) {
                // ignore
            }
            Log.i(Entry.TAG, "server area_code set -> " + value);
        }
        return chain.proceed();
    }
}
