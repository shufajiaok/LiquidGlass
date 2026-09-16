plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // 发布用（JitPack 就是跑 publishToMavenLocal 那一套）。不发布时留着也无害。
    `maven-publish`
}

/** 库版本。改了这里，[README](README.md) 里的依赖坐标也要跟着改。 */
val libraryVersion = "0.1.1"

android {
    namespace = "com.jiale.liquidglass"
    compileSdk = 35

    defaultConfig {
        // 玻璃的 RenderEffect 要 API 31+；低版本会自动退化成"半透明膜层 + 边缘光"，
        // 不会崩，所以 minSdk 可以放到 26。
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.jiale"
                artifactId = "liquidglass"
                version = libraryVersion
            }
        }
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    // 用 api 而不是 implementation：这个库的公开签名里到处是 Compose 类型
    // （Modifier / Shape / @Composable），使用方本来也必须自己引入 Compose。
    api(composeBom)
    api(libs.androidx.ui)
    api(libs.androidx.ui.graphics)
    api(libs.androidx.foundation)
    api(libs.androidx.material3)
}
