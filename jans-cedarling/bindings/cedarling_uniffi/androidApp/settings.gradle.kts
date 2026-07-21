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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Required by rustls-platform-verifier for TLS certificate verification on Android.
        // Points to the prebuilt AAR shipped inside the rustls-platform-verifier-android crate.
        maven {
            url = uri(
                java.io.File(
                    System.getProperty("user.home"),
                    ".cargo/registry/src"
                ).let { dir ->
                    val crate = dir.listFiles()
                        ?.firstOrNull { it.name.contains("rustls-platform-verifier-android") }
                    requireNotNull(crate) {
                        "Cannot find rustls-platform-verifier-android crate. " +
                        "Run 'cargo build' to populate the registry."
                    }
                    java.io.File(crate, "maven").toURI()
                }
            )
        }
    }
}

rootProject.name = "androidApp"
include(":app")
 