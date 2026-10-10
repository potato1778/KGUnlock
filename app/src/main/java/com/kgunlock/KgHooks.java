package com.kgunlock;

import android.util.Log;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedInterface.ExceptionMode;
import io.github.libxposed.api.XposedModule;

import java.lang.reflect.Method;
import java.util.Hashtable;

/**
 * 负责定位并挂载全部 hook。
 *
 * <p>每个 hook 独立 try/catch：某个类 / 方法在某些酷狗版本里被混淆或改名时，
 * 只记日志、不影响其它 hook 继续安装。
 *
 * <p>所有 hook 统一使用 {@link ExceptionMode#PROTECTIVE}，即拦截器抛异常时由框架
 * 捕获并记录，尽量不影响目标应用运行。
 */
final class KgHooks {

    private final XposedModule module;
    private final ClassLoader cl;

    KgHooks(XposedModule module, ClassLoader cl) {
        this.module = module;
        this.cl = cl;
    }

    void install() {
        if (Config.HOOK_AREA_CODE) hookAreaCode();
        if (Config.HOOK_VIP_FLAG) hookVipFlag();
        if (Config.HOOK_GATE) hookGate();
        if (Config.HOOK_WRITEBACK) hookWriteBack();
        if (Config.HOOK_GEO_STRIP) hookGeoStrip();
        if (Config.LOG_SERVER_RESPONSE) hookServerResponse();
    }

    // ── 基础工具 ──────────────────────────────────────────────────────

    private void log(String msg) {
        module.log(Log.INFO, Entry.TAG, msg);
    }

    /** 按名加载类，失败返回 null。 */
    private Class<?> classOf(String name) {
        try {
            return Class.forName(name, false, cl);
        } catch (Throwable t) {
            return null;
        }
    }

    /** 查找声明的方法并开放访问权限。 */
    private Method method(Class<?> owner, String name, Class<?>... params) {
        try {
            Method m = owner.getDeclaredMethod(name, params);
            m.setAccessible(true);
            return m;
        } catch (Throwable t) {
            return null;
        }
    }

    /** 挂载一个方法级拦截器，带失败兜底日志。 */
    private void hook(Method target, XposedInterface.Hooker hooker, String label) {
        try {
            module.hook(target)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(hooker);
            log("hook ok: " + label);
        } catch (Throwable t) {
            log("hook FAIL: " + label + " -> " + t);
        }
    }

    // ── 1. 核心：伪造地区码 ───────────────────────────────────────────
    // com.kugou.common.environment.a.ay() : String
    //   默认读 SharedPreferences key 0x50，缺省 "1"。
    //   被 com.kugou.android.app.g.a(int) 以及全 App 71 处取 URL 逻辑引用。

    private void hookAreaCode() {
        Class<?> owner = classOf("com.kugou.common.environment.a");
        if (owner == null) {
            log("area: class not found");
            return;
        }
        Method m = method(owner, "ay");
        if (m == null) {
            log("area: method not found");
            return;
        }
        hook(m, new AreaCodeHooker(), "environment.a.ay() -> \"" + Config.FAKE_AREA_CODE + "\"");
    }

    // ── 2. 伪造 is_special_vip ────────────────────────────────────────
    // com.kugou.common.environment.a.aJ() : int

    private void hookVipFlag() {
        Class<?> owner = classOf("com.kugou.common.environment.a");
        if (owner == null) {
            return;
        }
        Method m = method(owner, "aJ");
        if (m == null) {
            return;
        }
        hook(m, new VipFlagHooker(), "environment.a.aJ() -> 0");
    }

    // ── 3. 闸门 ──────────────────────────────────────────────────────
    // com.kugou.android.app.g.a(int) : boolean
    //   返回 true 表示「拦截」（弹版权限制）。

    private void hookGate() {
        Class<?> owner = classOf("com.kugou.android.app.g");
        if (owner == null) {
            log("gate: class not found");
            return;
        }
        Method m = method(owner, "a", int.class);
        if (m == null) {
            log("gate: method not found");
            return;
        }
        hook(m, new GateHooker(), "android.app.g.a(int) -> false");
    }

    // ── 4. 拦截写回 ──────────────────────────────────────────────────
    // com.kugou.common.network.a.c.a(com.kugou.common.network.a.f) : void
    //   服务端回包后会调用它把 area_code / vip 写回本地。
    //   若不拦截，下一次检查就会把伪造的值覆盖回真实值。

    private void hookWriteBack() {
        Class<?> owner = classOf("com.kugou.common.network.a.c");
        if (owner == null) {
            return;
        }
        Class<?> f = classOf("com.kugou.common.network.a.f");
        if (f == null) {
            log("writeback: f class not found");
            return;
        }
        Method m = method(owner, "a", f);
        if (m == null) {
            log("writeback: method not found");
            return;
        }
        hook(m, new WriteBackHooker(), "network.a.c.a(f)");
    }

    // ── 5. 剥离 GPS ──────────────────────────────────────────────────
    // com.kugou.common.network.a.c.a(f, Hashtable, boolean) : void
    //   带定位重发路径，会把 longitude / latitude 塞进参数表。

    private void hookGeoStrip() {
        Class<?> owner = classOf("com.kugou.common.network.a.c");
        if (owner == null) {
            return;
        }
        Class<?> f = classOf("com.kugou.common.network.a.f");
        if (f == null) {
            return;
        }
        Method m = method(owner, "a", f, Hashtable.class, boolean.class);
        if (m == null) {
            log("geo: method not found");
            return;
        }
        hook(m, new GeoStripHooker(), "network.a.c.a(f,Hashtable,boolean)");
    }

    // ── 6. 诊断：打印服务端返回 ──────────────────────────────────────
    // com.kugou.common.network.a.f.b(String) : void
    //   服务端回包解析后写入 area_code。

    private void hookServerResponse() {
        Class<?> owner = classOf("com.kugou.common.network.a.f");
        if (owner == null) {
            return;
        }
        Method m = method(owner, "b", String.class);
        if (m == null) {
            return;
        }
        hook(m, new ServerResponseHooker(), "diag network.a.f.b(String)");
    }
}
