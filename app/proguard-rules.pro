# 保持 Xposed 入口类不被裁剪 / 改名
-keep class com.kgunlock.Entry { *; }
-keep class com.kgunlock.*Hooker { *; }
-keep class com.kgunlock.KgHooks { *; }
-keep class com.kgunlock.Config { *; }

# libxposed API 由宿主提供
-dontwarn io.github.libxposed.**
