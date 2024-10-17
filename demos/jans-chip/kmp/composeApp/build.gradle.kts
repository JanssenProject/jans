
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    kotlin("plugin.serialization")
    //Room
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {

    sourceSets.commonMain {
        kotlin.srcDir("build/generated/ksp/metadata")
    }

    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {

        androidMain.dependencies {
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.compose.navigation)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
            implementation(libs.room.runtime.android)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(libs.ui.util)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.core)
            implementation(libs.ktor.json)
            implementation(libs.ktor.logging)
            implementation(libs.ktor.negotiation)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.image.loader)
            implementation(libs.kermit)
            implementation(libs.coil.compose)
            implementation(libs.coil.compose.ktor)
            implementation(compose.components.resources)

            val ktor_version = "2.3.7"
            implementation("io.ktor:ktor-client-cio:$ktor_version")
            implementation("io.ktor:ktor-client-auth:$ktor_version")
            implementation("io.ktor:ktor-server-auth:$ktor_version")

            implementation("com.fasterxml.jackson.core:jackson-databind:2.14.3")

            //Room
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)

            //common viewmodel
            implementation(libs.lifecycle.viewmodel.compose)

            //navigation
            implementation(libs.androidx.navigation.compose)

            // animation
            implementation(libs.androidx.core.animation)

            // required by koin
            implementation("co.touchlab:stately-common:2.0.5")

            //QR code scanner
            implementation("network.chaintech:qr-kit:2.0.0")

            implementation(project(":fido2"))
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
    task("testClasses")

}

android {
    namespace = "com.supergluu.kmp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "com.supergluu.kmp"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    dependencies {
        debugImplementation(libs.compose.ui.tooling)
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.android)
    implementation(projects.fido2)

    // Room
    add("kspCommonMainMetadata", libs.room.compiler)

    implementation("com.nimbusds:nimbus-jose-jwt:9.31")
    implementation("io.jsonwebtoken:jjwt:0.9.1")
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>>().configureEach {
    if (name != "kspCommonMainKotlinMetadata" ) {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}