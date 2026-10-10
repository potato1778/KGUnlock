package com.kgunlock;

import android.util.Log;

import androidx.annotation.NonNull;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam;

/**
 * 酷狗概念版地区解锁模块 —— 入口类。
 *
 * <p>由 LSPosed / LSPatch 通过 {@code META-INF/xposed/java_init.list} 加载。
 *
 * <p>工作流程：
 * <ol>
 *   <li>在 {@link #onPackageLoaded} 中筛选目标包名；</li>
 *   <li>委托 {@link KgHooks} 通过反射定位关键方法并挂载拦截器。</li>
 * </ol>
 *
 * @see Config 全部可调参数
 */
public final class Entry extends XposedModule {

    public static final String TAG = "KGUnlock";

    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {
        if (!Config.TARGET_PACKAGE.equals(param.getPackageName())) {
            return;
        }

        info("hit " + param.getPackageName());

        ClassLoader cl = param.getDefaultClassLoader();
        if (cl == null) {
            info("classLoader is null, abort");
            return;
        }

        try {
            new KgHooks(this, cl).install();
            info("all hooks installed");
        } catch (Throwable t) {
            info("hook error: " + t);
        }
    }

    /** 统一日志出口：同时写 logcat 与框架日志。 */
    void info(String msg) {
        if (!Config.VERBOSE_LOG) {
            return;
        }
        Log.i(TAG, msg);
        try {
            log(Log.INFO, TAG, msg);
        } catch (Throwable ignored) {
            // 框架日志不可用时忽略
        }
    }
}
