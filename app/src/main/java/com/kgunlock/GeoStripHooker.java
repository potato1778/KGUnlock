package com.kgunlock;

import android.util.Log;

import io.github.libxposed.api.XposedInterface;

import java.util.Hashtable;

/**
 * 从带定位重发的请求参数中剥离 GPS 坐标。
 *
 * <p>{@code com.kugou.common.network.a.c.a(f, Hashtable, boolean)} 会调用系统定位
 * 拿到真实经纬度并塞进请求参数表（"longitude" / "latitude"），
 * 同时把 "login" 置为 2（表示带定位的二次请求）。
 *
 * <p>这里移除经纬度并把 "login" 归 1，等效于「本机未提供定位」，
 * 避免真实 GPS 覆盖伪造的地区判定。
 *
 * <p>实现说明：直接修改参数表中的 {@link Hashtable} 对象本身（而非替换参数数组），
 * 因此照常调用 {@code chain.proceed()} 即可让修改生效。
 */
final class GeoStripHooker implements XposedInterface.Hooker {

    @Override
    @SuppressWarnings("unchecked")
    public Object intercept(XposedInterface.Chain chain) {
        if (chain.getArgs().size() < 2) {
            return chain.proceed();
        }

        Object arg1 = chain.getArg(1);
        if (!(arg1 instanceof Hashtable)) {
            return chain.proceed();
        }

        try {
            Hashtable<String, Object> table = (Hashtable<String, Object>) arg1;
            table.remove("longitude");
            table.remove("latitude");
            table.put("login", 1);
            Log.i(Entry.TAG, "GEO stripped (lon/lat removed, login->1)");
        } catch (Throwable t) {
            Log.i(Entry.TAG, "GEO strip failed: " + t);
        }

        return chain.proceed();
    }
}
