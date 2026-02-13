pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "BirthKeeper"
include(
    ":app",
    ":feature-person",
    ":feature-capture",
    ":feature-reminder",
    ":core-common",
    ":core-domain",
    ":core-data"
)

