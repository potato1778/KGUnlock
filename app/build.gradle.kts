plugins {
    id("com.android.application")
}

android {
    namespace = "com.kgunlock"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kgunlock.regionfree.unlockpatch"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            // 模块 APK 不需要混淆；保持类名可读便于排查
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            // 关键：META-INF/xposed 下的文件必须原样保留
            excludes += listOf(
                "META-INF/*.version",
            )
        }
    }
}

dependencies {
    // libxposed API 由宿主框架（LSPosed / LSPatch）在运行时提供，编译期仅需签名
    compileOnly("io.github.libxposed:api:101.0.1")
    compileOnly("androidx.annotation:annotation:1.9.1")
}
