plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    kotlin("plugin.serialization")
    //Room
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets.commonMain {
        kotlin.srcDir("build/generated/ksp/metadata")
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
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        sourceSets {
            named("commonMain") {
                resources.srcDirs("resources")
            }
        }
    }
}

android {
    namespace = "com.example.fido2"
    compileSdk = 34

    defaultConfig {
        minSdk = 28

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    sourceSets {
        named("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
            res.srcDirs("resources")
        }
    }
//    kotlinOptions {
//        jvmTarget = "1.8"
//    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "com.example.fido2"
    generateResClass = auto
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.material)
    implementation(libs.androidx.ui.android)
    implementation(libs.androidx.material3.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.android)

    // Room
    add("kspCommonMainMetadata", libs.room.compiler)

    implementation(project(":webauthn"))

    implementation("com.nimbusds:nimbus-jose-jwt:9.31")
    implementation("io.jsonwebtoken:jjwt:0.9.1")
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>>().configureEach {
    if (name != "kspCommonMainKotlinMetadata" ) {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}