pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
// Locates the Maven repository bundled inside the rustls-platform-verifier-android
// crate (in the local cargo registry). Its Kotlin component must be in the APK so
// that Cedarling's Rust code can use Android's certificate verifier for TLS.
// See https://github.com/rustls/rustls-platform-verifier#android
fun rustlsPlatformVerifierMavenRepo(): File {
    val json = providers.exec {
        workingDir = rootDir
        commandLine(
            "cargo", "metadata",
            "--format-version", "1",
            "--filter-platform", "aarch64-linux-android",
            "--manifest-path", File(rootDir, "../Cargo.toml").absolutePath
        )
    }.standardOutput.asText.get()

    @Suppress("UNCHECKED_CAST")
    val packages = (groovy.json.JsonSlurper().parseText(json) as Map<String, Any>)["packages"] as List<Map<String, Any>>
    val manifestPath = File(packages.first { it["name"] == "rustls-platform-verifier-android" }["manifest_path"] as String)
    return File(manifestPath.parentFile, "maven")
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri(rustlsPlatformVerifierMavenRepo())
            metadataSources { artifact() }
        }
    }
}

rootProject.name = "androidApp"
include(":app")
 